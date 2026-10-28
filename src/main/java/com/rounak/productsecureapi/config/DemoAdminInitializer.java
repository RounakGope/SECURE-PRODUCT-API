package com.rounak.productsecureapi.config;
import com.rounak.productsecureapi.entity.User;
import com.rounak.productsecureapi.enums.Role;
import com.rounak.productsecureapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class DemoAdminInitializer implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DemoAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DemoAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${DEMO_ADMIN_EMAIL:}") String adminEmail,
            @Value("${DEMO_ADMIN_PASSWORD:}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn(
                    "Demo admin not created: configure DEMO_ADMIN_EMAIL and DEMO_ADMIN_PASSWORD."
            );
            return;
        }

        String normalizedEmail =
                adminEmail.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.info("Demo admin initialization skipped: account already exists.");
            return;
        }

        User admin = new User(
                "Demo Administrator",
                normalizedEmail,
                passwordEncoder.encode(adminPassword),
                Role.ADMIN
        );

        admin.markEmailVerified();
        userRepository.save(admin);

        log.info("Demo admin account initialized.");
    }
}