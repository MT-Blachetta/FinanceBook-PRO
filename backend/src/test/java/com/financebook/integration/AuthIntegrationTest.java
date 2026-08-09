package com.financebook.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authentication and registration flows, including input edge cases.
 */
class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void registrationCreatesDefaultTaxonomy() throws Exception {
        String token = registerAndLogin("auth_defaults");

        MvcResult typesResult = mockMvc.perform(get("/category-types")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode types = parse(typesResult);
        assertThat(types.findValuesAsText("name")).contains("standard");

        MvcResult categoriesResult = mockMvc.perform(get("/categories")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode categories = parse(categoriesResult);
        assertThat(categories.findValuesAsText("name")).contains("UNCLASSIFIED");
    }

    @Test
    void usernameWhitespaceIsNormalized() throws Exception {
        JsonNode user = registerUser("  john   doe  ");
        assertThat(user.get("username").asText()).isEqualTo("john doe");

        // Login must work with the normalized name
        login("john doe", DEFAULT_PASSWORD);
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        registerUser("auth_dup");

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "auth_dup",
                                "password", DEFAULT_PASSWORD,
                                "surname", "T", "prename", "T"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateUsernameWithDifferentWhitespaceIsRejected() throws Exception {
        registerUser("auth dup2");

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "  auth   dup2 ",
                                "password", DEFAULT_PASSWORD,
                                "surname", "T", "prename", "T"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankUsernameIsRejected() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "   ",
                                "password", DEFAULT_PASSWORD,
                                "surname", "T", "prename", "T"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tooLongUsernameIsRejected() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "u".repeat(51),
                                "password", DEFAULT_PASSWORD,
                                "surname", "T", "prename", "T"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "auth_shortpw",
                                "password", "12345",
                                "surname", "T", "prename", "T"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingRequiredFieldsAreRejected() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", "auth_nofields");
        body.put("password", DEFAULT_PASSWORD);
        // surname / prename missing

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithWrongPasswordIsRejectedWithoutDetail() throws Exception {
        registerUser("auth_wrongpw");

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("username", "auth_wrongpw", "password", "nope123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect username or password"));
    }

    @Test
    void loginWithUnknownUserIsRejectedWithSameMessage() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("username", "does_not_exist", "password", "whatever1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect username or password"));
    }

    @Test
    void formEncodedLoginIsSupported() throws Exception {
        registerUser("auth_form");

        mockMvc.perform(post("/auth/login")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "auth_form")
                        .param("password", DEFAULT_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty());
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsCurrentProfile() throws Exception {
        String token = registerAndLogin("auth_me");

        mockMvc.perform(get("/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("auth_me"))
                .andExpect(jsonPath("$.is_admin").value(false));
    }

    @Test
    void profileUpdateChangesPassword() throws Exception {
        String token = registerAndLogin("auth_pwchange");

        mockMvc.perform(put("/auth/me")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("password", "newSecret9"))))
                .andExpect(status().isOk());

        // Old password no longer works, new one does
        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("username", "auth_pwchange", "password", DEFAULT_PASSWORD))))
                .andExpect(status().isUnauthorized());

        login("auth_pwchange", "newSecret9");
    }
}
