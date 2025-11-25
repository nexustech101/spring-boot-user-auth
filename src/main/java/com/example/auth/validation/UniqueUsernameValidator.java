package com.example.auth.validation;

import com.example.auth.repository.UserRepository;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.stereotype.Component;

/**
 * Validator implementation for @UniqueUsername annotation.
 * Checks if a username already exists in the database.
 */
@Component
public class UniqueUsernameValidator implements ConstraintValidator<UniqueUsername, String> {

    private final UserRepository userRepository;

    public UniqueUsernameValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void initialize(UniqueUsername constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null values are valid (handled by @NotBlank)
        if (value == null) {
            return true;
        }

        // Check if username exists in database
        boolean exists = userRepository.existsByUsername(value);
        
        if (exists) {
            // Customize error message
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Username '%s' is already taken", value)
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
