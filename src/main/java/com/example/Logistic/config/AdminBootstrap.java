package com.example.Logistic.config;

import com.example.Logistic.entity.UserAccount;
import com.example.Logistic.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {

    @Bean
    CommandLineRunner createAdmin(
            UserAccountRepository users,
            PasswordEncoder encoder,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.password:}") String password) {

        return args -> {
            if (email.isBlank() || password.isBlank()) {
                return;
            }

            String normalizedEmail = email.trim().toLowerCase();

            UserAccount admin = users.findByEmail(normalizedEmail)
                    .orElseGet(UserAccount::new);

            admin.setEmail(normalizedEmail);
            admin.setPassword(encoder.encode(password));
            admin.setRole("ADMIN");

            users.save(admin);
        };
    }
}