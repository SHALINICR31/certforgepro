package com.certforge.controller;

import com.certforge.model.User;
import com.certforge.repository.UserRepository;
import com.certforge.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthenticationManager authManager;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );

            String token = jwtUtils.generateToken(req.getUsername());

            userRepository.findByUsername(req.getUsername()).ifPresent(u -> {
                u.setLastLogin(LocalDateTime.now());
                userRepository.save(u);
            });

            User user = userRepository.findByUsername(req.getUsername()).orElseThrow();

            return ResponseEntity.ok(Map.of(
                "token",    token,
                "username", user.getUsername(),
                "fullName", user.getFullName() != null ? user.getFullName() : user.getUsername(),
                "email",    user.getEmail()    != null ? user.getEmail()    : ""
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        } catch (DisabledException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Account is disabled"));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        return userRepository.findByUsername(auth.getName())
            .map(u -> ResponseEntity.ok(Map.of(
                "username",  u.getUsername(),
                "fullName",  u.getFullName()  != null ? u.getFullName()  : u.getUsername(),
                "email",     u.getEmail()     != null ? u.getEmail()     : "",
                "lastLogin", u.getLastLogin() != null ? u.getLastLogin().toString() : ""
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    // ── DTO ──────────────────────────────────────────────
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
