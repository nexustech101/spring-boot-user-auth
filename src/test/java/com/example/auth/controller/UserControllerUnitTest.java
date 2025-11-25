package com.example.auth.controller;

import com.example.auth.dto.SignupRequest;
import com.example.auth.dto.UserResponse;
import com.example.auth.model.User;
import com.example.auth.services.UserService;
import com.example.auth.services.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;

import static org.junit.jupiter.api.Assertions.*;

public class UserControllerUnitTest {

    @Test
    void signupReturnsUserResponseWithoutPassword() {
        // Arrange
        UserService mockService = Mockito.mock(UserService.class);
        AuthenticationManager mockAuthManager = Mockito.mock(AuthenticationManager.class);
        RateLimiterService mockRateLimiter = Mockito.mock(RateLimiterService.class);
        UserController controller = new UserController(mockService, mockAuthManager, mockRateLimiter);

        SignupRequest req = new SignupRequest();
        req.setUsername("unit_user");
        req.setPassword("UnitSecret123");
        req.setEmail("unit@example.com");

        User returned = new User();
        returned.setId(42L);
        returned.setUsername("unit_user");
        returned.setEmail("unit@example.com");
        returned.setPassword("encrypted");

        Mockito.when(mockService.signup(Mockito.any(User.class))).thenReturn(returned);

        // Act
        ResponseEntity<UserResponse> resp = controller.signup(req);

        // Assert
        assertEquals(201, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        UserResponse body = resp.getBody();
        assertEquals(42L, body.getId());
        assertEquals("unit_user", body.getUsername());
        assertEquals("unit@example.com", body.getEmail());
        // DTO does not expose password; compile-time design ensures password is not present in UserResponse
    }
}

