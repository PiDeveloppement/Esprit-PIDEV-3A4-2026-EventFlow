package com.example.pidev.service.external;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;

public class OcrSpaceService {

    private static final String API_KEY = "K88774792488957";
    private static final String API_URL = "https://api.ocr.space/parse/image";

    public static String extractTextFromFile(File file) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(API_URL);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("apikey", API_KEY);
            builder.addTextBody("language", "fre");
            builder.addBinaryBody("file", file, ContentType.IMAGE_JPEG, file.getName());

            post.setEntity(builder.build());

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpEntity entity = response.getEntity();
                String json = EntityUtils.toString(entity);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();

                if (root.has("IsErroredOnProcessing") && root.get("IsErroredOnProcessing").getAsBoolean()) {
                    String errorMsg = root.has("ErrorMessage") ? root.get("ErrorMessage").getAsString() : "Erreur inconnue";
                    throw new IOException("Erreur OCR: " + errorMsg);
                }

                return root.get("ParsedResults").getAsJsonArray()
                        .get(0).getAsJsonObject()
                        .get("ParsedText").getAsString();
            }
        }
    }
}