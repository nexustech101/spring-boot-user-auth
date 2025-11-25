package com.example.auth.Config;

import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom UserDetailsService implementation to load users from the database
 * for Spring Security authentication.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user details for username={}", username);
        
        // Try to find user by username first
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        // If not found, try by email (support both username and email login)
        if (!userOpt.isPresent()) {
            userOpt = userRepository.findByEmail(username);
        }
        
        if (!userOpt.isPresent()) {
            logger.warn("User not found: {}", username);
            throw new UsernameNotFoundException("User not found with username or email: " + username);
        }
        
        User user = userOpt.get();
        logger.debug("User found: id={}, username={}", user.getId(), user.getUsername());
        
        // Create authorities (roles)
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // Return Spring Security UserDetails
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
