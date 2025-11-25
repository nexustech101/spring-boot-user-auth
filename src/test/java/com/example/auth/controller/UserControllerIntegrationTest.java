package com.example.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc getMockMvc() {
        return org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void signupDoesNotReturnPassword() throws Exception {
        String json = "{\"username\":\"itest_user\",\"password\":\"SuperSecret123\",\"email\":\"itest@example.com\"}";

        getMockMvc().perform(post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.username").value("itest_user"))
                .andExpect(jsonPath("$.email").value("itest@example.com"));
    }
}
