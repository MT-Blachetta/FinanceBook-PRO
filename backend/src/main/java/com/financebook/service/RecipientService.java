package com.financebook.service;

import com.financebook.dto.request.RecipientCreateRequest;
import com.financebook.dto.request.RecipientUpdateRequest;
import com.financebook.dto.response.RecipientReadResponse;
import com.financebook.entity.Recipient;
import com.financebook.entity.User;
import com.financebook.exception.ResourceNotFoundException;
import com.financebook.exception.UnauthorizedException;
import com.financebook.exception.ValidationException;
import com.financebook.entity.PaymentItem;
import com.financebook.repository.PaymentItemRepository;
import com.financebook.repository.RecipientRepository;
import com.financebook.repository.UserRepository;
import com.financebook.util.Constants;
import com.financebook.util.DtoMapper;
import com.financebook.util.NameNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Recipient service for managing transaction counterparties.
 * Implements name normalization and uniqueness validation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipientService {
    
    private final RecipientRepository recipientRepository;
    private final PaymentItemRepository paymentItemRepository;
    private final UserRepository userRepository;
    
    /**
     * Create a new recipient with normalized name.
     * Mirrors Python's create_recipient endpoint.
     */
    @Transactional
    public RecipientReadResponse createRecipient(RecipientCreateRequest request, String username) {
        User user = getUserByUsername(username);
        
        // Normalize and validate name
        String normalizedName = NameNormalizer.normalize(request.getName());
        if (normalizedName.isEmpty()) {
            throw new ValidationException("Recipient name cannot be empty");
        }
        if (normalizedName.length() > Constants.MAX_RECIPIENT_NAME_LENGTH) {
            throw new ValidationException("Recipient name exceeds maximum length");
        }
        
        // Check for duplicate name (per user)
        if (recipientRepository.findByUserIdAndName(user.getId(), normalizedName).isPresent()) {
            throw new ValidationException("Recipient name already exists");
        }
        
        // Create recipient
        Recipient recipient = Recipient.builder()
                .name(normalizedName)
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .user(user)
                .build();
        
        recipient = recipientRepository.save(recipient);
        
        return DtoMapper.toRecipientReadResponse(recipient);
    }
    
    /**
     * Get all recipients for the current user.
     */
    @Transactional(readOnly = true)
    public List<RecipientReadResponse> getAllRecipients(String username) {
        User user = getUserByUsername(username);
        List<Recipient> recipients = recipientRepository.findByUserId(user.getId());
        return DtoMapper.toRecipientReadResponseList(recipients);
    }
    
    /**
     * Get a single recipient by ID.
     */
    @Transactional(readOnly = true)
    public RecipientReadResponse getRecipientById(Long recipientId, String username) {
        User user = getUserByUsername(username);
        
        Recipient recipient = recipientRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));
        
        // Verify ownership
        if (!recipient.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to access this recipient");
        }
        
        return DtoMapper.toRecipientReadResponse(recipient);
    }
    
    /**
     * Update an existing recipient.
     * Mirrors Python's update_recipient endpoint.
     */
    @Transactional
    public RecipientReadResponse updateRecipient(
            Long recipientId, 
            RecipientUpdateRequest request, 
            String username) {
        
        User user = getUserByUsername(username);
        
        Recipient recipient = recipientRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));
        
        // Verify ownership
        if (!recipient.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to update this recipient");
        }
        
        // Update name if provided
        if (request.getName() != null) {
            String normalizedName = NameNormalizer.normalize(request.getName());
            if (normalizedName.isEmpty()) {
                throw new ValidationException("Recipient name cannot be empty");
            }
            
            // Check for duplicate name (excluding current recipient)
            recipientRepository.findByUserIdAndName(user.getId(), normalizedName)
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(recipientId)) {
                            throw new ValidationException("Recipient name already exists");
                        }
                    });
            
            recipient.setName(normalizedName);
        }
        
        // Update address if provided
        if (request.getAddress() != null) {
            recipient.setAddress(request.getAddress().trim().isEmpty() ? null : request.getAddress().trim());
        }
        
        recipient = recipientRepository.save(recipient);
        
        return DtoMapper.toRecipientReadResponse(recipient);
    }
    
    /**
     * Delete a recipient. Payment items that reference it keep existing but
     * lose the recipient link (payments are historical facts and must survive
     * address-book cleanups).
     */
    @Transactional
    public void deleteRecipient(Long recipientId, String username) {
        User user = getUserByUsername(username);

        Recipient recipient = recipientRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));
        if (!recipient.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to delete this recipient");
        }

        for (PaymentItem item : paymentItemRepository.findByRecipientId(recipientId)) {
            item.setRecipient(null);
            paymentItemRepository.save(item);
        }

        recipientRepository.delete(recipient);
        log.info("Deleted recipient {} ({})", recipientId, recipient.getName());
    }

    /**
     * Get user by username.
     */
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
