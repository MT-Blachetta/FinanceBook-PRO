package com.financebook.controller;

import com.financebook.dto.request.LoginRequest;
import com.financebook.dto.request.UserCreateRequest;
import com.financebook.dto.request.UserUpdateRequest;
import com.financebook.dto.response.JwtResponse;
import com.financebook.dto.response.UserReadResponse;
import com.financebook.service.AuthService;
import com.financebook.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller.
 * Provides /auth/* endpoints for user authentication and profile management.
 */
@Tag(name = "Authentication", description = "User authentication and registration")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final UserService userService;
    
    @Operation(summary = "User login", description = "Authenticate with username/password and receive JWT token")
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "User login (Form)", description = "Authenticate with form-encoded username/password")
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<JwtResponse> loginForm(
            @RequestParam String username, 
            @RequestParam String password) {
        
        LoginRequest loginRequest = new LoginRequest(username, password);
        JwtResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "User registration", description = "Register a new user account")
    @PostMapping("/register")
    public ResponseEntity<UserReadResponse> register(@Valid @RequestBody UserCreateRequest request) {
        UserReadResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get current user", description = "Return the profile of the currently authenticated user")
    @GetMapping("/me")
    public ResponseEntity<UserReadResponse> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        UserReadResponse response = authService.getCurrentUser(username);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Update current user", description = "Update the profile of the currently authenticated user")
    @PutMapping("/me")
    public ResponseEntity<UserReadResponse> updateProfile(
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {
        
        UserReadResponse response = userService.updateProfile(request, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
