package com.rounak.productsecureapi.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
        @Bean
        public SecretKey jwtSecretKey(
                @Value("${security.jwt.secret}") String secret
        ) {
            byte[] keyBytes = Base64.getDecoder().decode(secret);

            if (keyBytes.length < 32) {
                throw new IllegalStateException(
                        "JWT secret must contain at least 32 bytes"
                );
            }

            return new SecretKeySpec(keyBytes, "HmacSHA256");
        }

        @Bean
        public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
            return new NimbusJwtEncoder(
                    new ImmutableSecret<>(jwtSecretKey)
            );
        }

        @Bean
        public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
            return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(
                AuthenticationConfiguration configuration
        ) throws Exception {
            return configuration.getAuthenticationManager();
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
            JwtGrantedAuthoritiesConverter authoritiesConverter =
                    new JwtGrantedAuthoritiesConverter();

            authoritiesConverter.setAuthoritiesClaimName("roles");
            authoritiesConverter.setAuthorityPrefix("");

            JwtAuthenticationConverter converter =
                    new JwtAuthenticationConverter();

            converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

            return converter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                HttpSecurity http,
                JwtAuthenticationConverter jwtAuthenticationConverter
        ) throws Exception {

            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/auth/**").permitAll()

                            .requestMatchers(HttpMethod.GET, "/api/products/**")
                            .hasAnyRole("ADMIN", "USER")

                            .requestMatchers(HttpMethod.POST, "/api/products/**")
                            .hasRole("ADMIN")

                            .requestMatchers(HttpMethod.PUT, "/api/products/**")
                            .hasRole("ADMIN")

                            .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                            .hasRole("ADMIN")

                            .requestMatchers(
                                    "/swagger-ui/**",
                                    "/swagger-ui.html",
                                    "/v3/api-docs/**",
                                    "/v3/api-docs.yaml"
                            ).permitAll()

                            .anyRequest().authenticated()
                    )
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt
                                    .jwtAuthenticationConverter(
                                            jwtAuthenticationConverter
                                    )
                            )
                    );

            return http.build();
        }

}
