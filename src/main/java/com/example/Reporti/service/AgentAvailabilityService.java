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
public class AgentAvailabilityService {

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient http = HttpClient.newHttpClient();

    public AgentAvailabilityService(@Value("${supabase.url}") String baseUrl,
                                    @Value("${supabase.service-key}") String apiKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * Met à jour is_available dans les métadonnées de l'agent connecté.
     * On utilise le token de l'agent lui-même (PUT /auth/v1/user) : il ne peut donc modifier que son propre profil.
     */
    public boolean setAvailability(String authHeader, boolean available) {
        String body = "{\"data\":{\"is_available\":" + available + "}}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/auth/v1/user"))
                .header("apikey", apiKey)
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                HttpStatus status = HttpStatus.resolve(response.statusCode());
                throw new ResponseStatusException(
                        status != null ? status : HttpStatus.BAD_GATEWAY, response.body());
            }
            return available;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Supabase unreachable: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Request interrupted");
        }
    }
}