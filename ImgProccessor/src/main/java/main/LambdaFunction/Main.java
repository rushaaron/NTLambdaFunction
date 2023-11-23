package main.LambdaFunction;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.ImageUtils.ImageUtils;
import main.NeuronTracer.Branch;
import main.NeuronTracer.Tracer;
import main.NeuronTracer.Trips;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.image.BufferedImage;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

public class Main implements RequestHandler<Map<String, Object>, ApiGatewayResponse>{

    @Override
    /*
     * Takes in an InputRecord, which contains two integers and a String.
     * Logs the String, then returns the sum of the two Integers.
     */
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        final String bodyJson = input.get(NTConstants.BODY).toString();
        logger.log(bodyJson);
        final Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", NTConstants.DOMAIN);
        headers.put("Access-Control-Allow-Credentials", true);

        try {
            // Converting input image to a BufferedStream and getting user parameters
            final String imageDataBase64 = extractJsonValue(bodyJson, NTConstants.IMAGE_DATA).toString().split(",")[1];
            final byte[] imageData = Base64.getDecoder().decode(imageDataBase64);
            final BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            final UserParameters userParameters = populateUserParameters(bodyJson);
            logger.log(userParameters.toString());

            //
            // Insert other program here
            //
            BufferedImage whiteOutput = new BufferedImage(image.getWidth(), image.getHeight(),BufferedImage.TYPE_INT_RGB);
            ImageUtils.setImageToWhite(whiteOutput, image, true);
            BufferedImage blackOutput = new BufferedImage(image.getWidth(), image.getHeight(),BufferedImage.TYPE_INT_RGB);
            ImageUtils.setImageToWhite(blackOutput, image, false);
            BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(),BufferedImage.TYPE_INT_RGB);
            ImageUtils.setImageToWhite(output, image, false);

            //
            // Start of the actual work
            //
            Tracer tracer = new Tracer(userParameters.getRedBranch(), userParameters.getXOffset(), userParameters.getYOffset(),
                    userParameters.getRedCellBranch(), userParameters.getStemToIgnore(), image, output, whiteOutput, blackOutput);

            ArrayList<Branch> branches = tracer.createBranches();
            logger.log("Finished Tracing");

            //
            // Save processed images and file to temporary files
            //
            final File tracedImage = File.createTempFile("TracedImage", ".jpg");
            ImageIO.write(output, "jpg", tracedImage);
            final File tracedImageWhite = File.createTempFile("TracedImageWhite", ".jpg");
            ImageIO.write(whiteOutput, "jpg", tracedImageWhite);

            final File ndfFile = createNDFFile("NeuronJInformation", branches);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(output, "jpg", baos);
            byte[] processedImageData = baos.toByteArray();
            final String processedImageDataBase64 = Base64.getEncoder().encodeToString(processedImageData);

            //
            // Creating and uploading the zip file
            //
            final File zipFile = createZipFile("processed_images.zip", tracedImage, tracedImageWhite, ndfFile);
            final String signedUrl = uploadToBucket(zipFile);

            Map<String, Object> body = new HashMap<>();
            body.put(NTConstants.SIGNED_URL, signedUrl);
            body.put(NTConstants.TRACED_EXAMPLE, processedImageDataBase64);

            final ObjectMapper objectMapper = new ObjectMapper();
            return new ApiGatewayResponse(200, headers, objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            logger.log(e.getMessage());
            Map<String, Object> body = new HashMap<>();
            body.put("Error Message", "Failed to Trace");
        }
        return new ApiGatewayResponse(400, headers, "didn't work");
    }

    private static UserParameters populateUserParameters(String jsonString){
        final int redBranch = Integer.parseInt(extractJsonValue(jsonString, NTConstants.RED_BRANCH).toString());
        final int redCellBranch = Integer.parseInt(extractJsonValue(jsonString, NTConstants.RED_CELL_BRANCH).toString());
        final int xOffset = Integer.parseInt(extractJsonValue(jsonString, NTConstants.X_OFFSET).toString());
        final int yOffset = Integer.parseInt(extractJsonValue(jsonString, NTConstants.Y_OFFSET).toString());
        final String stemToIgnore = extractJsonValue(jsonString, NTConstants.STEM_TO_IGNORE).toString();
        return new UserParameters(redBranch, redCellBranch, xOffset, yOffset, stemToIgnore);
    }

    private static String uploadToBucket(File zipFile) {
        final AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion("us-east-1")
                .build();

        s3Client.putObject(new PutObjectRequest(NTConstants.BUCKET_NAME, NTConstants.ZIP_FILE_NAME, zipFile));

        final Date expiration = new Date(System.currentTimeMillis() + NTConstants.TEN_MINUTES);
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(NTConstants.BUCKET_NAME, NTConstants.ZIP_FILE_NAME)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);

        return s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
    }

    private static File createZipFile(String zipFileName, File... filesToAdd) {
        try {
            File tempDir = Files.createTempDirectory("tempDir").toFile();
            File zipFile = new File(tempDir, zipFileName);

            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                for (File file : filesToAdd) {
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zos.putNextEntry(zipEntry);

                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    zos.write(fileBytes);
                    zos.closeEntry();
                }
            }
            return zipFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Creates the NDF file
    private static File createNDFFile(String name, ArrayList<Branch> branches) throws IOException {
        final File ndfFile = File.createTempFile(name, ".ndf");

        try {
            FileWriter writer = new FileWriter(ndfFile);
            writer.write("// NeuronJ Data File - DO NOT CHANGE\n");
            writer.write("1.4.3\n");
            writer.write("// Parameters\n");
            writer.write("1\n1.0\n0.7\n2\n1100\n3\n5\n1\n");
            writer.write("// Type names and colors\n");
            writer.write("Default\n4\nAxon\n7\nDendrite\n1\nPrimary\n7\nSecondary\n1\nTertiary\n8\n");
            writer.write("Type 06\n4\nType 07\n4\nType 08\n4\nType 09\n4\nType 10\n4\n");
            writer.write("// Cluster names\nDefault\nCluster 01\nCluster 02\nCluster 03\nCluster 04\nCluster 05\nCluster 06\nCluster 07\nCluster 08\nCluster 09\nCluster 10\n");

            int i = 1;
            for (Branch b : branches) {
                writer.write("// Tracing N"+ i+"\n"+i+"\n0\n0\nDefault\n");
                writer.write("// Segment 1 of Tracing N"+i+"\n");
                for (Trips t : b.getPoints()) {
                    writer.write(t.x + "\n" + t.y+"\n");
                }
                i++;
            }
            writer.write("// End of NeuronJ Data File");
            writer.close();
        } catch (IOException e) {
            System.out.println("Error has occured");
        }
        return ndfFile;
    }

    private static Object extractJsonValue(String jsonString, String key) {
        String pattern = "\"" + key + "\":\\s*(\"[^\"]*\"|\\d+)";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(jsonString);

        if (matcher.find()) {
            String valueString = matcher.group(1);

            // Remove quotes if it's a string
            if (valueString.startsWith("\"") && valueString.endsWith("\"")) {
                return valueString.substring(1, valueString.length() - 1);
            }

            // Parse as integer if it's a number
            try {
                return Integer.parseInt(valueString);
            } catch (NumberFormatException e) {
                // Handle the case where the key is not found or the value is not an integer
                return null; // or throw an exception, return a default value, etc.
            }
        } else {
            // Handle the case where the key is not found
            return null; // or throw an exception, return a default value, etc.
        }
    }
}

record ApiGatewayResponse(int statusCode, Map<String, Object> headers, Object body) {
}
