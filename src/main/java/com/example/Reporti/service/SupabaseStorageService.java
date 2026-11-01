package com.example.Reporti.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class SupabaseStorageService {

    private final String baseUrl;
    private final String serviceKey;
    private final String bucket;
    private final HttpClient http = HttpClient.newHttpClient();

    public SupabaseStorageService(
            @Value("${supabase.url}") String baseUrl,
            @Value("${supabase.service-key}") String serviceKey,
            @Value("${supabase.bucket}") String bucket) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.serviceKey = serviceKey;
        this.bucket = bucket;
    }

    /** Uploads the bytes to the bucket and returns the public URL of the stored file. */
    public String upload(String path, byte[] data, String contentType) {
        URI uri = URI.create(baseUrl + "/storage/v1/object/" + bucket + "/" + path);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + serviceKey)
                .header("apikey", serviceKey)
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();

        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Storage upload failed: " + response.body());
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Storage unreachable: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Storage upload interrupted");
        }

        return baseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }
}