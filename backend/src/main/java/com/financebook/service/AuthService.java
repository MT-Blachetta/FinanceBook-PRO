package com.financebook.service;

import com.financebook.dto.request.LoginRequest;
import com.financebook.dto.request.UserCreateRequest;
import com.financebook.dto.response.JwtResponse;
import com.financebook.dto.response.UserReadResponse;
import com.financebook.entity.CategoryType;
import com.financebook.entity.Category;
import com.financebook.entity.User;
import com.financebook.exception.ValidationException;
import com.financebook.repository.CategoryRepository;
import com.financebook.repository.CategoryTypeRepository;
import com.financebook.repository.UserRepository;
import com.financebook.security.JwtTokenProvider;
import com.financebook.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service.
 * Handles user login and registration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final CategoryTypeRepository categoryTypeRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    
    /**
     * Authenticate user and generate JWT token.
     * Mirrors Python's login endpoint.
     */
    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        
        return new JwtResponse(jwt);
    }
    
    /**
     * Register new user with default category setup.
     * Mirrors Python's register endpoint.
     */
    @Transactional
    public UserReadResponse register(UserCreateRequest request) {
        // Normalize and validate username
        String normalizedUsername = normalizeText(request.getUsername());
        if (normalizedUsername.isEmpty()) {
            throw new ValidationException("Username cannot be empty");
        }
        if (normalizedUsername.length() > Constants.MAX_USERNAME_LENGTH) {
            throw new ValidationException("Username exceeds " + Constants.MAX_USERNAME_LENGTH + " characters");
        }
        
        // Check for duplicate username
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ValidationException("Username already taken");
        }
        
        // Validate password length
        if (request.getPassword().length() < Constants.MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + Constants.MIN_PASSWORD_LENGTH + " characters");
        }
        
        // Create user
        User user = User.builder()
                .username(normalizedUsername)
                .hashedPassword(passwordEncoder.encode(request.getPassword()))
                .surname(normalizeText(request.getSurname()))
                .prename(normalizeText(request.getPrename()))
                .birthDate(request.getBirthDate())
                .phone(request.getPhone())
                .road(request.getRoad())
                .houseNumber(request.getHouseNumber())
                .region(request.getRegion())
                .postal(request.getPostal())
                .city(request.getCity())
                .state(request.getState())
                .isAdmin(false)
                .isActive(true)
                .build();
        
        user = userRepository.save(user);
        
        // Create default "standard" category type for new user
        CategoryType standardType = CategoryType.builder()
                .name("standard")
                .description("Default category type for basic expense/income classification")
                .user(user)
                .build();
        standardType = categoryTypeRepository.save(standardType);
        
        // Create default "UNCLASSIFIED" category
        Category unclassified = Category.builder()
                .name("UNCLASSIFIED")
                .type(standardType)
                .parent(null)
                .user(user)
                .build();
        categoryRepository.save(unclassified);
        
        return mapToUserReadResponse(user);
    }
    
    /**
     * Get current authenticated user.
     */
    public UserReadResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ValidationException("User not found"));
        
        return mapToUserReadResponse(user);
    }
    
    /**
     * Normalize text by collapsing whitespace.
     * Mirrors Python's _normalize_name().
     */
    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ");
    }
    
    /**
     * Map User entity to UserReadResponse DTO.
     */
    private UserReadResponse mapToUserReadResponse(User user) {
        return UserReadResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .surname(user.getSurname())
                .prename(user.getPrename())
                .birthDate(user.getBirthDate())
                .phone(user.getPhone())
                .road(user.getRoad())
                .houseNumber(user.getHouseNumber())
                .region(user.getRegion())
                .postal(user.getPostal())
                .city(user.getCity())
                .state(user.getState())
                .isAdmin(user.getIsAdmin())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
