package com.rounak.productsecureapi.serviceImpl;

import com.rounak.productsecureapi.dto.user.AuthResponse;
import com.rounak.productsecureapi.dto.user.LoginRequest;
import com.rounak.productsecureapi.dto.user.UserRegistrationRequest;
import com.rounak.productsecureapi.dto.user.UserResponse;
import com.rounak.productsecureapi.entity.User;
import com.rounak.productsecureapi.enums.OtpPurpose;
import com.rounak.productsecureapi.enums.Role;
import com.rounak.productsecureapi.repository.UserRepository;
import com.rounak.productsecureapi.service.AuthService;
import com.rounak.productsecureapi.service.JwtService;
import com.rounak.productsecureapi.service.OtpService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OtpService otpService;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            OtpService otpService
    ) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        User user = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                Role.USER
        );

        User savedUser = userRepository.save(user);



        return toUserResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());


        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.password()
                )
        );

        String token = jwtService.generateToken(authentication);

        return new AuthResponse(token, "Bearer");
    }
    @Override
    @Transactional
    public void verifyEmail(String email, String otp) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid email or OTP")
                );

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email is already verified");
        }

        boolean valid = otpService.verifyOtp(
                normalizedEmail,
                otp,
                OtpPurpose.EMAIL_VERIFICATION
        );

        if (!valid) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        user.markEmailVerified();
        userRepository.save(user);
    }
    @Override
    public void generateEmailVerificationOtp(String email) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email is already verified");
        }

        otpService.generateAndSendOtp(
                normalizedEmail,
                OtpPurpose.EMAIL_VERIFICATION
        );
    }
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.isEmailVerified()
        );
    }
}
