package com.certforge.config;

import com.certforge.model.User;
import com.certforge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createUser("admin", "admin123", "admin@example.com", "Admin User");
        createUser("john",  "john123",  "john@example.com",  "John Doe");
        createUser("priya", "priya123", "priya@example.com", "Priya Kumar");
    }

    private void createUser(String username, String rawPassword, String email, String fullName) {
        if (userRepository.existsByUsername(username)) {
            log.info("User already exists: {}", username);
            return;
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setEmail(email);
        u.setFullName(fullName);
        u.setActive(true);
        userRepository.save(u);
        log.info("Created user: {}", username);
    }
}
