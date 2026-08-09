package com.financebook.service;

import com.financebook.dto.request.UserUpdateRequest;
import com.financebook.dto.response.UserReadResponse;
import com.financebook.entity.*;
import com.financebook.exception.ResourceNotFoundException;
import com.financebook.exception.UnauthorizedException;
import com.financebook.exception.ValidationException;
import com.financebook.repository.*;
import com.financebook.util.Constants;
import com.financebook.util.DtoMapper;
import com.financebook.util.NameNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin service for user management and statistics.
 * Mirrors Python's admin.py functionality.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final UserRepository userRepository;
    private final PaymentItemRepository paymentItemRepository;
    private final RecipientRepository recipientRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Get all users (admin only).
     * Mirrors Python's admin_list_users endpoint.
     */
    @Transactional(readOnly = true)
    public List<UserReadResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(DtoMapper::toUserReadResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get a user by ID (admin only).
     * Mirrors Python's admin_get_user endpoint.
     */
    @Transactional(readOnly = true)
    public UserReadResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return DtoMapper.toUserReadResponse(user);
    }
    
    /**
     * Update a user (admin only).
     * Mirrors Python's admin_update_user endpoint.
     */
    @Transactional
    public UserReadResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
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
        if (request.getBirthDate() != null) user.setBirthDate(request.getBirthDate());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getRoad() != null) user.setRoad(request.getRoad());
        if (request.getHouseNumber() != null) user.setHouseNumber(request.getHouseNumber());
        if (request.getRegion() != null) user.setRegion(request.getRegion());
        if (request.getPostal() != null) user.setPostal(request.getPostal());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getState() != null) user.setState(request.getState());
        
        user = userRepository.save(user);
        
        return DtoMapper.toUserReadResponse(user);
    }
    
    /**
     * Deactivate a user account (admin only).
     * Mirrors Python's admin_deactivate_user endpoint.
     * Does not delete data, just sets is_active = false.
     */
    @Transactional
    public Map<String, String> deactivateUser(Long userId, Long adminUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Safety guard: admins cannot deactivate their own account
        if (user.getId().equals(adminUserId)) {
            throw new ValidationException("Cannot deactivate your own admin account");
        }
        
        user.setIsActive(false);
        userRepository.save(user);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "User '" + user.getUsername() + "' has been deactivated");
        
        return response;
    }
    
    /**
     * Get dashboard statistics.
     * Mirrors Python's admin dashboard data.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findByIsActiveTrue().size();
        long totalPayments = paymentItemRepository.count();
        long totalRecipients = recipientRepository.count();
        long totalCategories = categoryRepository.count();
        
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("paymentItems", totalPayments);
        stats.put("recipients", totalRecipients);
        stats.put("categories", totalCategories);
        
        return stats;
    }
}
