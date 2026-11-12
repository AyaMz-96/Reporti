package com.example.Reporti.controller;

import com.example.Reporti.service.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    public record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6) String password,
            @NotBlank String fullName,
            String phone,
            String role) {}

    public record UpdateUserRequest(String fullName, String phone, String role, Boolean suspended) {}

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<String> list(@RequestHeader(value = "Authorization", required = false) String auth) {
        service.requireAdmin(auth);
        return json(service.list());
    }

    @PostMapping
    public ResponseEntity<String> create(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @Valid @RequestBody CreateUserRequest r) {
        service.requireAdmin(auth);
        return json(service.create(r.email(), r.password(), r.fullName(), r.phone(), r.role()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String id,
            @RequestBody UpdateUserRequest r) {
        AdminUserService.Caller me = service.requireAdmin(auth);
        if (me.id().equals(id)) {
            if (Boolean.TRUE.equals(r.suspended())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot suspend yourself");
            }
            if (r.role() != null && !"admin".equals(r.role())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot remove your own admin role");
            }
        }
        return json(service.update(id, r.fullName(), r.phone(), r.role(), r.suspended()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String id) {
        AdminUserService.Caller me = service.requireAdmin(auth);
        if (me.id().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete yourself");
        }
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<String> json(String body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }
}