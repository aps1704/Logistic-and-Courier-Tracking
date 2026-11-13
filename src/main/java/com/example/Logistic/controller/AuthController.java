package com.example.Logistic.controller;

import com.example.Logistic.entity.UserAccount;
import com.example.Logistic.repository.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            UserAccountRepository users,
            PasswordEncoder encoder,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.users = users;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String password = body.getOrDefault("password", "");

        if (email.isBlank() || password.length() < 8) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Enter an email and a password of at least 8 characters."));
        }

        if (users.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message", "An account with this email already exists."));
        }

        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setRole("USER");

        users.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("message", "Registration successful. Please log in."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    body.getOrDefault("email", "")
                                            .trim().toLowerCase(),
                                    body.getOrDefault("password", "")
                            )
                    );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            request.getSession(true);
            securityContextRepository.saveContext(
                    context, request, response);

            String role = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                    ? "ADMIN" : "USER";

            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "role", role
            ));

        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Invalid email or password."));
        }
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                ? "ADMIN" : "USER";

        return Map.of(
                "email", authentication.getName(),
                "role", role
        );
    }
}