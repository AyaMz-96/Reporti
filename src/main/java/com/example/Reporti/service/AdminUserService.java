package com.example.Reporti.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    public record Caller(String id, String role) {}

    private static final Set<String> ROLES = Set.of("citizen", "agent", "sector_manager", "admin");

    private final String baseUrl;
    private final String serviceKey;
    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper mapper = JsonMapper.builder().build();

    public AdminUserService(
            @Value("${supabase.url}") String baseUrl,
            @Value("${supabase.service-key}") String serviceKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.serviceKey = serviceKey;
    }

    public Caller requireAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        Map<String, Object> user;
        try {
            user = parse(send("GET", "/auth/v1/user", null, authHeader));
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token: " + e.getReason());
        }
        String role = "citizen";
        if (user.get("app_metadata") instanceof Map<?, ?> meta && meta.get("role") instanceof String r) {
            role = r;
        }
        if (!"admin".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }
        return new Caller(String.valueOf(user.get("id")), role);
    }

    public String list() {
        return send("GET", "/auth/v1/admin/users?per_page=200", null, adminBearer());
    }

    public List<Map<String, Object>> getAvailableAgents(Integer categoryId, Integer sectorId) {
        String json = list();
        try {
            Map<String, Object> response = mapper.readValue(json, Map.class);
            List<Map<String, Object>> users = (List<Map<String, Object>>) response.get("users");
            if (users == null) return List.of();

            return users.stream()
                .filter(u -> {
                    Map<String, Object> appMeta = (Map<String, Object>) u.get("app_metadata");
                    Map<String, Object> userMeta = (Map<String, Object>) u.get("user_metadata");
                    if (appMeta == null || !"agent".equals(appMeta.get("role"))) return false;
                    if (userMeta == null) return false;
                    if (Boolean.FALSE.equals(userMeta.get("is_available"))) return false;

                    if (categoryId != null) {
                        Integer aCat = toIntOrNull(userMeta.get("categoryId"));
                        if (aCat == null || !aCat.equals(categoryId)) return false;
                    }
                    if (sectorId != null) {
                        Integer aSec = toIntOrNull(userMeta.get("sectorId"));
                        if (aSec == null || !aSec.equals(sectorId)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse agents");
        }
    }

    public String create(String email, String password, String fullName, String phone, String role,
                         Integer categoryId, Integer sectorId) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("full_name", fullName);
        meta.put("phone", phone);
        if (categoryId != null) meta.put("categoryId", categoryId);
        if (sectorId != null) meta.put("sectorId", sectorId);
        if ("agent".equals(role)) meta.put("is_available", true);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("email_confirm", true);
        body.put("user_metadata", meta);
        body.put("app_metadata", Map.of("role", checkRole(role == null ? "citizen" : role)));
        return send("POST", "/auth/v1/admin/users", toJson(body), adminBearer());
    }

    public String update(String id, String fullName, String phone, String role, Boolean suspended,
                         Integer categoryId, Integer sectorId) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (fullName != null || phone != null || categoryId != null || sectorId != null) {
            Map<String, Object> meta = new LinkedHashMap<>();
            if (fullName != null) meta.put("full_name", fullName);
            if (phone != null) meta.put("phone", phone);
            if (categoryId != null) meta.put("categoryId", categoryId);
            if (sectorId != null) meta.put("sectorId", sectorId);
            body.put("user_metadata", meta);
        }
        if (role != null) body.put("app_metadata", Map.of("role", checkRole(role)));
        if (suspended != null) body.put("ban_duration", suspended ? "876000h" : "none");
        return send("PUT", "/auth/v1/admin/users/" + checkId(id), toJson(body), adminBearer());
    }

    public void delete(String id) {
        send("DELETE", "/auth/v1/admin/users/" + checkId(id), null, adminBearer());
    }

    private Integer toIntOrNull(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String adminBearer() {
        return serviceKey.startsWith("sb_secret_") ? null : "Bearer " + serviceKey;
    }

    private String checkRole(String role) {
        if (!ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }
        return role;
    }

    private String checkId(String id) {
        try {
            return UUID.fromString(id).toString();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id");
        }
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JSON error");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) {
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unexpected response from Supabase");
        }
    }

    private String send(String method, String path, String json, String bearer) {
        HttpRequest.BodyPublisher body = json == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("apikey", serviceKey)
                .header("Content-Type", "application/json")
                .method(method, body);
        if (bearer != null) {
            builder.header("Authorization", bearer);
        }
        HttpRequest request = builder.build();

        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                HttpStatus status = HttpStatus.resolve(response.statusCode());
                throw new ResponseStatusException(
                        status != null ? status : HttpStatus.BAD_GATEWAY, response.body());
            }
            return response.body();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Supabase unreachable: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Request interrupted");
        }
    }
}
