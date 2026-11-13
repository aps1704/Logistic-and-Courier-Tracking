package com.example.Logistic.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public Map<String, String> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute("_csrf");

        if (token == null) {
            throw new IllegalStateException("CSRF token unavailable");
        }

        return Map.of(
                "headerName", token.getHeaderName(),
                "token", token.getToken()
        );
    }
}