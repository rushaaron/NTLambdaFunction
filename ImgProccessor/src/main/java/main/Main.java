package main;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private static final String BODY = "body";
    private static final String IMAGE_DATA = "imageData";
    private static final String RED_BRANCH = "redBranch";
    private static final String RED_CELL_BRANCH = "redCellBranch";
    private static final String STEM_TO_IGNORE = "stemToIgnore";
    private static final String X_OFFSET = "xOffset";
    private static final String Y_OFFSET = "yOffset";
    private static final String DOMAIN = "https://neurontracer.com";
    private static final String ZIP_FILE_NAME = "TracedImages.zip";
    private static final String BUCKET_NAME = "neuron-tracer-uploads";
    private static final String SIGNED_URL = "signedUrl";
    private static final String TRACED_EXAMPLE = "tracedExample";
    private static final int TEN_MINUTES = 600000;

    @Override
    /*
     * Takes in an InputRecord, which contains two integers and a String.
     * Logs the String, then returns the sum of the two Integers.
     */
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        final String bodyJson = input.get(BODY).toString();

        final Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", DOMAIN);
        headers.put("Access-Control-Allow-Credentials", true); // Allow credentials if required

        try {
            // Converting input image to a BufferedStream
            final String imageDataBase64 = extractJsonValue(bodyJson, IMAGE_DATA).toString().split(",")[1];
            final byte[] imageData = Base64.getDecoder().decode(imageDataBase64);
            final BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));

            //
            // Insert other program here
            //

            // Save processed images and file to temporary files
            final File image1 = File.createTempFile("image1", ".jpg");
            final File image2 = File.createTempFile("image2", ".jpg");

            ImageIO.write(image, "jpg", image1);
            BufferedImage whiteOutput = new BufferedImage(image.getWidth(), image.getHeight(),BufferedImage.TYPE_INT_RGB);
            setImageToWhite(whiteOutput, image);
            ImageIO.write(whiteOutput, "jpg", image2);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(whiteOutput, "jpg", baos);
            byte[] processedImageData = baos.toByteArray();
            String processedImageDataBase64 = Base64.getEncoder().encodeToString(processedImageData);

            //
            // Creating and uploading the zip file
            //
            final File zipFile = createZipFile("processed_images.zip", image1, image2);
            final String signedUrl = uploadToBucket(zipFile);

            Map<String, Object> body = new HashMap<>();
            body.put(SIGNED_URL, signedUrl);
            body.put(TRACED_EXAMPLE, processedImageDataBase64);

            final ObjectMapper objectMapper = new ObjectMapper();
            String responseBody = objectMapper.writeValueAsString(body);

            return new ApiGatewayResponse(200, headers, responseBody);
        } catch (Exception e) {
            logger.log(e.getMessage());
            Map<String, Object> body = new HashMap<>();
            body.put("Error Message", "didn'twork bud");
            //responseObject.put("signedUrl", signedUrl);
            return new ApiGatewayResponse(400, headers, "didn't work");
        }
    }

    private static void setImageToWhite(BufferedImage img, BufferedImage input) {
        Color white = new Color(55,255,34);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                img.setRGB(x, y, white.getRGB());
            }
        }
    }

    private static UserParameters populateUserParameters(String jsonString){
        // still need to mess with stem to ignore
        final int redBranch = Integer.parseInt(extractJsonValue(jsonString, RED_BRANCH).toString());
        final int redCellBranch = Integer.parseInt(extractJsonValue(jsonString, RED_CELL_BRANCH).toString());
        final int xOffset = Integer.parseInt(extractJsonValue(jsonString, X_OFFSET).toString());
        final int yOffset = Integer.parseInt(extractJsonValue(jsonString, Y_OFFSET).toString());
        final String stemToIgnore = extractJsonValue(jsonString, STEM_TO_IGNORE).toString();
        return new UserParameters(redBranch, redCellBranch, xOffset, yOffset, stemToIgnore);
    }

    private static String uploadToBucket(File zipFile) {
        final AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion("us-east-1")
                .build();

        s3Client.putObject(new PutObjectRequest(BUCKET_NAME, ZIP_FILE_NAME, zipFile));

        final Date expiration = new Date(System.currentTimeMillis() + TEN_MINUTES);
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(BUCKET_NAME, ZIP_FILE_NAME)
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

record UserParameters(int redBranch, int redCellBranch, int xOffset, int yOffset, String stemToIgnore) {
}

record ApiGatewayResponse(int statusCode, Map<String, Object> headers, Object body) {
}
