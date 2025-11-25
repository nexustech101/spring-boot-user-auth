package com.example.auth.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Allow unauthenticated access to signup/signin endpoints, H2 console, and Swagger docs
                .requestMatchers(
                    "/api/v1/users/signup", "/api/v1/users/signin", "/h2-console/**",
                    // Swagger UI endpoints
                    "/swagger-ui.html", "/swagger-ui/**", "/swagger-ui/index.html",
                    // OpenAPI/API docs endpoints
                    "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml",
                    // Webjars (static resources for Swagger UI)
                    "/webjars/**",
                    // Favicon and other resources
                    "/favicon.ico"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                // Create session when needed and persist it
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            // Handle unauthenticated requests with 401 Unauthorized
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        // If using H2 console during development, allow frames
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        logger.info("Security configured: session policy={}, permitted endpoints=[/api/v1/users/signup,/api/v1/users/signin,/h2-console/**]",
            SessionCreationPolicy.IF_REQUIRED);
        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
