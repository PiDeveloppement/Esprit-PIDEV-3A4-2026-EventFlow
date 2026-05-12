package com.example.pidev.service.upload;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

public class CloudinaryUploadService {

    public static final String CLOUD_NAME = "dzfkwwvqn";

    // ✅ tes presets Cloudinary (exact)
    public static final String PRESET_LOGO = "Eventflow_logo";
    public static final String PRESET_CONTRACT = "eventflow_contract";

    private static final HttpClient client = HttpClient.newHttpClient();

    public String uploadLogo(File file) throws IOException, InterruptedException {
        // images => auto/upload ok
        return upload(file, PRESET_LOGO, "auto");
    }

    public String uploadPdfContract(File file) throws IOException, InterruptedException {
        // ✅ PDF => raw/upload (évite image/upload + problèmes 401)
        return upload(file, PRESET_CONTRACT, "raw");
    }

    private String upload(File file, String preset, String resourceType) throws IOException, InterruptedException {
        if (file == null || !file.exists()) throw new IOException("Fichier introuvable");

        String boundary = "----Boundary" + UUID.randomUUID();
        byte[] body = buildMultipart(boundary, file, preset);

        String url = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/" + resourceType + "/upload";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("Cloudinary upload error: " + response.statusCode() + " => " + response.body());
        }

        String secureUrl = extractJsonValue(response.body(), "secure_url");
        if (secureUrl == null) throw new IOException("Cloudinary response sans secure_url: " + response.body());

        return secureUrl;
    }

    private byte[] buildMultipart(String boundary, File file, String preset) throws IOException {
        String CRLF = "\r\n";

        String part1 =
                "--" + boundary + CRLF +
                        "Content-Disposition: form-data; name=\"upload_preset\"" + CRLF + CRLF +
                        preset + CRLF;

        String contentType = guessContentType(file);
        String part2Header =
                "--" + boundary + CRLF +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + CRLF +
                        "Content-Type: " + contentType + CRLF + CRLF;

        byte[] fileBytes = Files.readAllBytes(file.toPath());

        String end =
                CRLF + "--" + boundary + "--" + CRLF;

        return concat(
                part1.getBytes(StandardCharsets.UTF_8),
                part2Header.getBytes(StandardCharsets.UTF_8),
                fileBytes,
                end.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String guessContentType(File file) {
        String n = file.getName().toLowerCase();
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    private byte[] concat(byte[]... arrays) {
        int len = 0;
        for (byte[] a : arrays) len += a.length;
        byte[] out = new byte[len];
        int p = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, out, p, a.length);
            p += a.length;
        }
        return out;
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int i = json.indexOf(pattern);
        if (i < 0) return null;
        int start = json.indexOf("\"", i + pattern.length());
        if (start < 0) return null;
        int end = json.indexOf("\"", start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }
}
