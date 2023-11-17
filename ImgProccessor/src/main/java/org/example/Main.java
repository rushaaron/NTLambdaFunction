package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;


public class Main implements RequestHandler<Map<String, Object>, ApiGatewayResponse>{
    private static final String BODY = "body";
    private static final String DOMAIN = "https://neurontracer.com";

    @Override
    /*
     * Takes in an InputRecord, which contains two integers and a String.
     * Logs the String, then returns the sum of the two Integers.
     */
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        final String bodyJson = input.get(BODY).toString();

        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        headers.put("Access-Control-Allow-Origin", DOMAIN);
        headers.put("isBase64Encoded", true);

        try {
            String imageDataBase64 = extractJsonValue(bodyJson, "imageData").toString().split(",")[1];
            // Convert base64-encoded image data to bytes
            byte[] imageData = Base64.getDecoder().decode(imageDataBase64);
            /* Convert bytes to BufferedImage */
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));

            //
            // Insert other program here
            //

            // Convert BufferedImage back to bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", baos);
            byte[] processedImageData = baos.toByteArray();

            // Return the processed image data as base64-encoded string
            String processedImageDataBase64 = Base64.getEncoder().encodeToString(processedImageData);

            // Return a success response with a 200 status code
            return new ApiGatewayResponse(200, headers, processedImageDataBase64);
        } catch (Exception e) {
            // Handle exceptions appropriately
            logger.log(e.getMessage());
        }
        return new ApiGatewayResponse(400, headers, "Error processing image data");
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

record userParameters(int redBranch, int redCellBranch, int xOffset, int yOffset, String stemToIgnore) {
}

record ApiGatewayResponse(int statusCode, Map<String, Object> headers, String body) {
}
