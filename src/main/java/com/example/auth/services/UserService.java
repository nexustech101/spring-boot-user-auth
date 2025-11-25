package com.example.auth.services;

import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import com.example.auth.dto.SigninRequest;
import com.example.auth.utils.InputSanitizer;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Service
public class UserService {
    
    private final UserRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository repository, BCryptPasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    // CRUD Methods

    public Page<User> getAllUsers(Pageable pageable) {
        logger.debug("Fetching paginated users with page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return this.repository.findAll(pageable);
    }

    public Optional<User> getUserById(Long id) {
        return this.repository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        logger.debug("Finding user by username={}", username);
        return this.repository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        logger.debug("Finding user by email={}", email);
        return this.repository.findByEmail(email);
    }

    public Page<User> findByName(String name, Pageable pageable) {
        logger.debug("Finding paginated users by name pattern={} with page={}, size={}", name, pageable.getPageNumber(), pageable.getPageSize());
        return this.repository.findByName(name, pageable);
    }

    public boolean existsByUsername(String username) {
        return this.repository.existsByUsername(username);
    }
    
    public boolean existsByEmail(String email) {
        return this.repository.existsByEmail(email);
    }

    public User createUser(User user) {
        logger.info("Creating user username={}", user.getUsername());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = this.repository.save(user);
        logger.debug("Created user id={}", saved.getId());
        return saved;
    }

    public User updateUserEmail(Long id, String newEmail) {
        logger.info("Updating email for user id={} to {}", id, newEmail);
        return this.repository.findById(id).map(user -> {
            user.setEmail(newEmail);
            User saved = this.repository.save(user);
            logger.debug("Updated email for user id={}", id);
            return saved;
        })
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }

    public User updatePassword(Long id, String newPassword) {
        logger.info("Updating password for user id={}", id);
        return this.repository.findById(id).map(user -> {
            user.setPassword(this.passwordEncoder.encode(newPassword));
            User saved = this.repository.save(user);
            logger.debug("Updated password for user id={}", id);
            return saved;
        })
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }

    public void deleteUser(Long id) {
        logger.info("Deleting user id={}", id);
        Optional<User> userOpt = this.repository.findById(id);
        if (userOpt.isPresent()) {
            this.repository.deleteById(id);
            logger.debug("Deleted user id={}", id);
        } else {
            logger.warn("Attempted to delete non-existing user id={}", id);
        }
        return; // Explicitly return to indicate completion
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        boolean ok = this.passwordEncoder.matches(rawPassword, encodedPassword);
        logger.debug("Password match result={}", ok);
        return ok;
    }

    public boolean isValidPassword(Long id, String rawPassword) {
        Optional<User> userOpt = this.repository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            boolean ok = this.passwordEncoder.matches(rawPassword, user.getPassword());
            logger.debug("Validating password for id={} result={}", id, ok);
            return ok;
        }
        return false;
    }

    public boolean isValidEmail(Long id, String email) {
        Optional<User> userOpt = this.repository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            boolean ok = user.getEmail().equals(email);
            logger.debug("Validating email for id={} result={}", id, ok);
            return ok;
        }
        return false;
    }

    // Persistant authentication methods

    public User signup(User user) {
        logger.info("Attempting signup for username={}", user.getUsername());
        
        // Sanitize inputs
        String sanitizedUsername = InputSanitizer.sanitizeUsername(user.getUsername());
        String sanitizedEmail = InputSanitizer.sanitizeEmail(user.getEmail());
        
        // Validate input safety (defense against injection attacks)
        if (!InputSanitizer.isSafeInput(sanitizedUsername)) {
            logger.warn("Signup rejected: unsafe username pattern");
            throw new BadRequestException("Invalid username format");
        }
        if (!InputSanitizer.isSafeInput(sanitizedEmail)) {
            logger.warn("Signup rejected: unsafe email pattern");
            throw new BadRequestException("Invalid email format");
        }

        User newUser = new User(
            sanitizedUsername,
            this.passwordEncoder.encode(user.getPassword()),
            sanitizedEmail
        );

        User saved = this.repository.save(newUser);
        logger.info("Signup successful for username={} id={}", saved.getUsername(), saved.getId());
        return saved;
    }

    // Authentication user
    public Optional<User> signin(SigninRequest signinRequest) {
        logger.info("Signin attempt for identifier={}", signinRequest.getUsername());
        
        // Sanitize input
        String sanitizedIdentifier = InputSanitizer.sanitizeEmail(signinRequest.getUsername());
        
        // Try username lookup first
        Optional<User> userOpt = this.repository.findByUsername(sanitizedIdentifier);

        // Try email if username lookup fails
        if (!userOpt.isPresent()) {
            userOpt = this.repository.findByEmail(sanitizedIdentifier);
        }

        // Validate password if user was found
        if (userOpt.isPresent() && this.passwordEncoder.matches(signinRequest.getPassword(), userOpt.get().getPassword())) {
            logger.info("Signin successful for username={}", userOpt.get().getUsername());
            return userOpt;
        }

        logger.warn("Signin failed for identifier={}", sanitizedIdentifier);
        return Optional.empty();
    }
}


