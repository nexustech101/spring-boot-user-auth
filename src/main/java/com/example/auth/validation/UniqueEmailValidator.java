package com.example.auth.validation;

import com.example.auth.repository.UserRepository;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.stereotype.Component;

/**
 * Validator implementation for @UniqueEmail annotation.
 * Checks if an email already exists in the database.
 */
@Component
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository userRepository;

    public UniqueEmailValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void initialize(UniqueEmail constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null values are valid (handled by @NotBlank)
        if (value == null) {
            return true;
        }

        // Check if email exists in database
        boolean exists = userRepository.existsByEmail(value);
        
        if (exists) {
            // Customize error message
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Email '%s' is already registered", value)
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
