package com.example.auth.controller;

import com.example.auth.model.User;
import com.example.auth.services.UserService;
import com.example.auth.services.RateLimiterService;
import com.example.auth.dto.SigninRequest;
import com.example.auth.dto.SignupRequest;
import com.example.auth.dto.UserResponse;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@SuppressWarnings("NullableProblems")
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/v1/users")
public class UserController {
    
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final RateLimiterService rateLimiterService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService, AuthenticationManager authenticationManager, RateLimiterService rateLimiterService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Retrieves all users with pagination support.
     *
     * @param page the page number (0-indexed, default 0)
     * @param size the page size (default 10, max 100)
     * @param sortBy the field to sort by (default "id")
     * @return ResponseEntity containing a Page of UserResponse objects
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(defaultValue = "id") String sortBy
    ) {
        logger.debug("GET /api/v1/users - getAllUsers called with page={}, size={}", page, size);
        
        // Parse pagination parameters using local variables
        int pageNumber = page < 0 ? 0 : page;
        int pageSize = size < 1 ? 10 : Math.min(size, 100); // Max 100 per page
        
        // Create Pageable
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
            pageNumber, 
            pageSize, 
            org.springframework.data.domain.Sort.Direction.ASC,
            sortBy
        );
        
        try {
            Page<User> users = this.userService.getAllUsers(pageable);
            Page<UserResponse> resp = users.map(UserResponse::fromUser);
            return new ResponseEntity<>(resp, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching paginated users: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id the user ID
     * @return ResponseEntity containing the UserResponse if found, or NOT_FOUND if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        logger.debug("GET /api/v1/users/{} - getUserById called", id);
        Optional<User> user = this.userService.getUserById(id);
        return user.map(requestedUser -> new ResponseEntity<>(UserResponse.fromUser(requestedUser), HttpStatus.OK))
                   .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Retrieves a user by their username.
     *
     * @param username the username to search for
     * @return ResponseEntity containing the UserResponse if found, or NOT_FOUND if not found
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        logger.debug("GET /api/v1/users/username/{} - getUserByUsername called", username);
        Optional<User> user = this.userService.findByUsername(username);
        return user.map(requestedUser -> new ResponseEntity<>(UserResponse.fromUser(requestedUser), HttpStatus.OK))
                   .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Retrieves a user by their email address.
     *
     * @param email the email address to search for
     * @return ResponseEntity containing the UserResponse if found, or NOT_FOUND if not found
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        logger.debug("GET /api/v1/users/email/{} - getUserByEmail called", email);
        Optional<User> user = this.userService.findByEmail(email);
        return user.map(requestedUser -> new ResponseEntity<>(UserResponse.fromUser(requestedUser), HttpStatus.OK))
                   .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Searches for users by username pattern with pagination support.
     * Handles large result sets by allowing pagination to avoid loading all matches at once.
     *
     * @param name the username pattern to search for (supports partial matches)
     * @param page the page number (0-indexed, default 0)
     * @param size the page size (default 10, max 100)
     * @param sortBy the field to sort by (default "id")
     * @return ResponseEntity containing a paginated Page of UserResponse objects,
     *         or BAD_REQUEST if an error occurs
     */
    @GetMapping("/search/{name}")
    public ResponseEntity<?> getUsersByName(
        @PathVariable String name,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy
    ) {
        logger.debug("GET /api/v1/users/search/{} - getUsersByName called with page={}, size={}", name, page, size);
        
        // Parse pagination parameters
        int pageNumber = page < 0 ? 0 : page;
        int pageSize = size < 1 ? 10 : Math.min(size, 100); // Max 100 per page
        
        try {
            Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageNumber,
                pageSize,
                org.springframework.data.domain.Sort.Direction.ASC,
                sortBy
            );
            Page<User> users = this.userService.findByName(name, pageable);
            Page<UserResponse> resp = users.map(UserResponse::fromUser);
            return new ResponseEntity<>(resp, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching users by name {}: {}", name, e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Updates a user's email address.
     *
     * @param id the user ID
     * @param newEmail the new email address
     * @return ResponseEntity containing the updated UserResponse
     * @throws ResourceNotFoundException if user not found
     */
    @PutMapping("/{id}/email")
    public ResponseEntity<UserResponse> updateUserEmail(@PathVariable Long id, @RequestParam String newEmail) {
        logger.info("PUT /api/v1/users/{}/email - update email to {}", id, newEmail);
        User updatedUser = this.userService.updateUserEmail(id, newEmail);
        return new ResponseEntity<>(UserResponse.fromUser(updatedUser), HttpStatus.OK);
    }

    /**
     * Updates a user's password.
     *
     * @param id the user ID
     * @param newPassword the new password
     * @return ResponseEntity containing the updated UserResponse
     * @throws ResourceNotFoundException if user not found
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<UserResponse> updateUserPassword(@PathVariable Long id, @RequestParam String newPassword) {
        logger.info("PUT /api/v1/users/{}/password - update password", id);
        User updatedUser = this.userService.updatePassword(id, newPassword);
        return new ResponseEntity<>(UserResponse.fromUser(updatedUser), HttpStatus.OK);
    }

    /**
     * Deletes a user by ID.
     *
     * @param id the user ID to delete
     * @return ResponseEntity with NO_CONTENT status
     * @throws ResourceNotFoundException if user not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        logger.info("DELETE /api/v1/users/{} - deleteUser called", id);
        this.userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Creates a new user account (sign up).
     * Validates input, checks for duplicate username/email, and persists the user.
     *
     * @param signupRequest the signup request containing username, email, and password
     * @return ResponseEntity containing the created UserResponse with CREATED status
     * @throws ConflictException if username or email already exists
     */
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
        logger.info("POST /api/v1/users/signup - signup attempt for username={}", signupRequest.getUsername());
        User newUser = new User(signupRequest.getUsername(), signupRequest.getPassword(), signupRequest.getEmail());
        User createdUser = this.userService.signup(newUser);
        return new ResponseEntity<>(UserResponse.fromUser(createdUser), HttpStatus.CREATED);
    }

    /**
     * Authenticates a user and creates a session.
     * Enforces rate limiting (3 attempts per 10 minutes) and sets JSESSIONID cookie.
     *
     * @param signinRequest the signin request containing username/email and password
     * @param session the HTTP session
     * @return ResponseEntity containing the authenticated UserResponse and JSESSIONID cookie,
     *         or UNAUTHORIZED if credentials are invalid
     * @throws ConflictException if rate limit exceeded (429 TOO_MANY_REQUESTS)
     */
    @PostMapping("/signin")
    public ResponseEntity<UserResponse> signin(@Valid @RequestBody SigninRequest signinRequest, HttpSession session) {
        String username = signinRequest.getUsername();
        logger.info("POST /api/v1/users/signin - signin attempt for identifier={}", username);
        
        // Validate username presence
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Signin attempt with missing username");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        // Check rate limit (3 attempts per 10 minutes)
        if (!rateLimiterService.isAllowed(username)) {
            logger.warn("Signin rate limit exceeded for identifier={}", username);
            return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
        }
        
        try {
            String password = signinRequest.getPassword();
            // Use AuthenticationManager to authenticate credentials
            // This will validate username/password against the user database
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(username, password);
            
            Authentication authentication = authenticationManager.authenticate(authToken);
            
            // Store the authenticated token in SecurityContextHolder
            // Spring Security will automatically create a session and set JSESSIONID cookie
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Optional: fetch user details for response
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                logger.info("Signin successful for username={}, sessionId={}", user.getUsername(), session.getId());
                return new ResponseEntity<>(UserResponse.fromUser(user), HttpStatus.OK);
            } else {
                logger.warn("User not found after successful authentication: {}", username);
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            logger.warn("Signin failed for identifier={}: {}", username, e.getMessage());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Terminates the current user session.
     * Invalidates the HTTP session and clears the security context.
     *
     * @param session the HTTP session to invalidate
     * @return ResponseEntity with NO_CONTENT status
     */
    @PostMapping("/signout")
    public ResponseEntity<?> signout(HttpSession session) {
        // Invalidate session and clear security context
        try {
            session.invalidate();
        } finally {
            SecurityContextHolder.clearContext();
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Retrieves the currently authenticated user's information.
     * Requires active session (JSESSIONID cookie).
     *
     * @param principal the Principal containing the authenticated user's username
     * @param session the HTTP session
     * @return ResponseEntity containing the current UserResponse if authenticated,
     *         or UNAUTHORIZED if no session exists
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Principal principal, HttpSession session) {
        if (principal == null) {
            logger.warn("GET /me - principal is null, sessionId={}", session.getId());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String username = principal.getName();
        logger.info("GET /me - retrieving user for username={}, sessionId={}", username, session.getId());
        Optional<User> user = this.userService.findByUsername(username);
        return user.map(requestedUser -> new ResponseEntity<>(UserResponse.fromUser(requestedUser), HttpStatus.OK))
                   .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
