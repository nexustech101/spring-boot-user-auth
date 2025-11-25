package com.example.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerErrorCasesTest {

    @Autowired
    private WebApplicationContext wac;

    private org.springframework.test.web.servlet.MockMvc getMockMvc() {
        return org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void signupConflictReturns409() throws Exception {
        String json = "{\"username\":\"conflict_user\",\"password\":\"Pwd12345!\",\"email\":\"conflict@example.com\"}";

        // First signup should succeed
        getMockMvc().perform(post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        // Second signup with same username/email should return 409
        getMockMvc().perform(post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void updateNonExistentUserReturns404() throws Exception {
        // Try to update email for a user id that does not exist
        getMockMvc().perform(put("/api/v1/users/99999/email")
                .param("newEmail", "noone@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
