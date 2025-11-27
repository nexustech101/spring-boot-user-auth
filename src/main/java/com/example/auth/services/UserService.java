package com.example.auth.services;

import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import com.example.auth.dto.SigninRequest;
import com.example.auth.utils.InputSanitizer;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.EnableCaching;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Service for user management and authentication.
 * Implements Redis caching with separate regions for ID, username, and email lookups.
 * Passwords are encrypted using BCrypt.
 */
@EnableCaching
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

    /**
     * Retrieves all users with pagination.
     *
     * @param pageable pagination parameters
     * @return page of users
     */
    public Page<User> getAllUsers(Pageable pageable) {
        logger.debug("Fetching paginated users with page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return this.repository.findAll(pageable);
    }

    // Reads (Redis)
    /**
     * Retrieves a user by ID.
     * Cached in 'usersById' region with 5-minute TTL.
     *
     * @param id the user ID
     * @return Optional containing the user if found
     */
    @Cacheable(cacheNames = "usersById", key = "#id", unless = "#result == null")
    public Optional<User> getUserById(Long id) {
        return this.repository.findById(id);
    }

    /**
     * Finds a user by username.
     * Cached in 'usersByUsername' region with 10-minute TTL.
     *
     * @param username the username
     * @return Optional containing the user if found
     */
    @Cacheable(cacheNames = "usersByUsername", key = "#username", unless = "#result == null")
    public Optional<User> findByUsername(String username) {
        logger.debug("Finding user by username={}", username);
        return this.repository.findByUsername(username);
    }

    /**
     * Finds a user by email.
     * Cached in 'usersByEmail' region with 10-minute TTL.
     *
     * @param email the email address
     * @return Optional containing the user if found
     */
    @Cacheable(cacheNames = "usersByEmail", key = "#email", unless = "#result == null")
    public Optional<User> findByEmail(String email) {
        logger.debug("Finding user by email={}", email);
        return this.repository.findByEmail(email);
    }

    /**
     * Searches for users by name pattern with pagination.
     *
     * @param name the name pattern
     * @param pageable pagination parameters
     * @return page of matching users
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = "usersById", key = "#result.id", condition = "#result != null"),
        @CacheEvict(cacheNames = "usersByUsername", key = "#result.username", condition = "#result != null"),
        @CacheEvict(cacheNames = "usersByEmail", key = "#result.email", condition = "#result != null")
    })
    public Page<User> findByName(String name, Pageable pageable) {
        logger.debug("Finding paginated users by name pattern={} with page={}, size={}", name, pageable.getPageNumber(), pageable.getPageSize());
        return this.repository.findByName(name, pageable);
    }

    /**
     * Checks if a username exists.
     *
     * @param username the username to check
     * @return true if exists
     */
    public boolean existsByUsername(String username) {
        return this.repository.existsByUsername(username);
    }
    
    /**
     * Checks if an email exists.
     *
     * @param email the email to check
     * @return true if exists
     */
    public boolean existsByEmail(String email) {
        return this.repository.existsByEmail(email);
    }

    // Writes — evict affected entries (Redis)
    /**
     * Creates a new user.
     * Password is automatically encrypted with BCrypt.
     *
     * @param user the user to create
     * @return the created user
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = "usersById", key = "#result.id", condition = "#result != null"),
        @CacheEvict(cacheNames = "usersByUsername", key = "#result.username", condition = "#result != null"),
        @CacheEvict(cacheNames = "usersByEmail", key = "#result.email", condition = "#result != null")
    })
    public User createUser(User user) {
        logger.info("Creating user username={}", user.getUsername());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = this.repository.save(user);
        logger.debug("Created user id={}", saved.getId());
        return saved;
    }

    /**
     * Updates a user's email address.
     *
     * @param id the user ID
     * @param newEmail the new email
     * @return the updated user
     * @throws ResourceNotFoundException if user not found
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = "usersById", key = "#id"),
        @CacheEvict(cacheNames = "usersByUsername", allEntries = true),
        @CacheEvict(cacheNames = "usersByEmail", allEntries = true)
    })
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

    /**
     * Updates a user's password.
     * Password is automatically encrypted with BCrypt.
     *
     * @param id the user ID
     * @param newPassword the new password
     * @return the updated user
     * @throws ResourceNotFoundException if user not found
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = "usersById", key = "#id"),
        @CacheEvict(cacheNames = "usersByUsername", allEntries = true)
    })
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

    /**
     * Deletes a user by ID.
     *
     * @param id the user ID to delete
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = "usersById", key = "#id"),
        @CacheEvict(cacheNames = "usersByUsername", allEntries = true),
        @CacheEvict(cacheNames = "usersByEmail", allEntries = true)
    })
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

    /**
     * Checks if a raw password matches an encoded password.
     *
     * @param rawPassword the plain-text password
     * @param encodedPassword the BCrypt-encoded password
     * @return true if passwords match
     */
    public boolean checkPassword(String rawPassword, String encodedPassword) {
        boolean ok = this.passwordEncoder.matches(rawPassword, encodedPassword);
        logger.debug("Password match result={}", ok);
        return ok;
    }

    /**
     * Validates if a password is correct for a user.
     *
     * @param id the user ID
     * @param rawPassword the password to validate
     * @return true if password is correct
     */
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

    /**
     * Validates if an email matches a user's email.
     *
     * @param id the user ID
     * @param email the email to validate
     * @return true if email matches
     */
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

    /**
     * Registers a new user with input sanitization.
     * Sanitizes username and email, encrypts password with BCrypt.
     *
     * @param user the user to register
     * @return the created user
     * @throws BadRequestException if input is unsafe
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = "usersById", key = "#result.id", condition = "#result != null"),
        @CacheEvict(cacheNames = "usersByUsername", key = "#result.username", condition = "#result != null"),
        @CacheEvict(cacheNames = "usersByEmail", key = "#result.email", condition = "#result != null")
    })
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
    /**
     * Authenticates a user by username or email.
     * Supports both username and email as identifier.
     *
     * @param signinRequest contains username/email and password
     * @return Optional containing the user if authenticated
     */
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


