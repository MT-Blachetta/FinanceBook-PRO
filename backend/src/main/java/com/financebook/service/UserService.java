package com.financebook.service;

import com.financebook.dto.request.UserUpdateRequest;
import com.financebook.dto.response.UserReadResponse;
import com.financebook.entity.User;
import com.financebook.exception.ResourceNotFoundException;
import com.financebook.exception.ValidationException;
import com.financebook.repository.UserRepository;
import com.financebook.util.Constants;
import com.financebook.util.DtoMapper;
import com.financebook.util.NameNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User service for profile management operations.
 * Mirrors Python's user update logic from main.py.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Update user profile.
     * Mirrors Python's update_profile endpoint (PUT /auth/me).
     */
    @Transactional
    public UserReadResponse updateProfile(UserUpdateRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            if (request.getPassword().length() < Constants.MIN_PASSWORD_LENGTH) {
                throw new ValidationException("Password must be at least " + Constants.MIN_PASSWORD_LENGTH + " characters");
            }
            user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        // Update name fields with normalization
        if (request.getSurname() != null) {
            user.setSurname(NameNormalizer.normalize(request.getSurname()));
        }
        if (request.getPrename() != null) {
            user.setPrename(NameNormalizer.normalize(request.getPrename()));
        }
        
        // Update other fields
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getRoad() != null) {
            user.setRoad(request.getRoad());
        }
        if (request.getHouseNumber() != null) {
            user.setHouseNumber(request.getHouseNumber());
        }
        if (request.getRegion() != null) {
            user.setRegion(request.getRegion());
        }
        if (request.getPostal() != null) {
            user.setPostal(request.getPostal());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getState() != null) {
            user.setState(request.getState());
        }
        
        user = userRepository.save(user);
        
        return DtoMapper.toUserReadResponse(user);
    }
}
