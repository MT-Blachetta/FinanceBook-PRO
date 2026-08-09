package com.financebook.controller;

import com.financebook.dto.request.RecipientCreateRequest;
import com.financebook.dto.request.RecipientUpdateRequest;
import com.financebook.dto.response.RecipientReadResponse;
import com.financebook.service.RecipientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Recipient controller.
 * Provides recipient management endpoints.
 */
@Tag(name = "Recipients", description = "Recipient management endpoints")
@RestController
@RequestMapping("/recipients")
@RequiredArgsConstructor
public class RecipientController {
    
    private final RecipientService recipientService;
    
    @Operation(summary = "Create recipient", description = "Create a new recipient")
    @PostMapping
    public ResponseEntity<RecipientReadResponse> createRecipient(
            @Valid @RequestBody RecipientCreateRequest request,
            Authentication authentication) {
        
        RecipientReadResponse response = recipientService.createRecipient(
                request, authentication.getName());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "List recipients", description = "Get all recipients for the current user")
    @GetMapping
    public ResponseEntity<List<RecipientReadResponse>> listRecipients(
            Authentication authentication) {
        
        List<RecipientReadResponse> recipients = recipientService.getAllRecipients(
                authentication.getName());
        
        return ResponseEntity.ok(recipients);
    }
    
    @Operation(summary = "Get recipient", description = "Get a single recipient by ID")
    @GetMapping("/{recipientId}")
    public ResponseEntity<RecipientReadResponse> getRecipient(
            @PathVariable Long recipientId,
            Authentication authentication) {
        
        RecipientReadResponse response = recipientService.getRecipientById(
                recipientId, authentication.getName());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Update recipient", description = "Update an existing recipient")
    @PutMapping("/{recipientId}")
    public ResponseEntity<RecipientReadResponse> updateRecipient(
            @PathVariable Long recipientId,
            @Valid @RequestBody RecipientUpdateRequest request,
            Authentication authentication) {
        
        RecipientReadResponse response = recipientService.updateRecipient(
                recipientId, request, authentication.getName());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete recipient",
            description = "Delete a recipient; payment items keep existing without the recipient link")
    @DeleteMapping("/{recipientId}")
    public ResponseEntity<Void> deleteRecipient(
            @PathVariable Long recipientId,
            Authentication authentication) {

        recipientService.deleteRecipient(recipientId, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
