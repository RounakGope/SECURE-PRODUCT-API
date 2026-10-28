package com.rounak.productsecureapi.serviceImpl;

import com.rounak.productsecureapi.enums.OtpPurpose;
import com.rounak.productsecureapi.service.OtpService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpServiceImpl(
            StringRedisTemplate redisTemplate,
            PasswordEncoder passwordEncoder
    ) {
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void generateAndSendOtp(String email, OtpPurpose purpose) {
        String normalizedEmail = normalizeEmail(email);
        String key = buildKey(normalizedEmail, purpose);

        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        Map<String, String> data = Map.of(
                "otpHash", passwordEncoder.encode(otp),
                "attempts", "0",
                "purpose", purpose.name()
        );

        redisTemplate.opsForHash().putAll(key, data);
        redisTemplate.expire(key, OTP_TTL);

        System.out.println(
                "[DEV OTP] Purpose: " + purpose
                        + " | Email: " + normalizedEmail
                        + " | OTP: " + otp
        );
    }

    @Override
    public boolean verifyOtp(
            String email,
            String otp,
            OtpPurpose purpose
    ) {
        if (otp == null || !otp.matches("\\d{6}")) {
            return false;
        }

        String key = buildKey(normalizeEmail(email), purpose);

        // Redis hash reads.
        List<Object> values = redisTemplate.opsForHash()
                .multiGet(key, List.of("otpHash", "attempts", "purpose"));

        if (values == null
                || values.size() != 3
                || values.get(0) == null
                || values.get(1) == null
                || values.get(2) == null) {
            return false;
        }

        String otpHash = values.get(0).toString();
        int attempts = Integer.parseInt(values.get(1).toString());
        String storedPurpose = values.get(2).toString();

        if (!purpose.name().equals(storedPurpose)) {
            return false;
        }

        if (attempts >= MAX_ATTEMPTS) {
            redisTemplate.delete(key);
            return false;
        }

        if (!passwordEncoder.matches(otp, otpHash)) {
            redisTemplate.opsForHash().increment(key, "attempts", 1);

            if (attempts + 1 >= MAX_ATTEMPTS) {
                redisTemplate.delete(key);
            }

            return false;
        }

        redisTemplate.delete(key);
        return true;
    }

    private String buildKey(String email, OtpPurpose purpose) {
        return "otp:" + purpose.name() + ":" + email;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}