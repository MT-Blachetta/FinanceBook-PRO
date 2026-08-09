package com.financebook.controller;

import com.financebook.dto.request.PaymentItemCreateRequest;
import com.financebook.dto.request.PaymentItemUpdateRequest;
import com.financebook.dto.response.PaymentItemReadResponse;
import com.financebook.service.PaymentItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Payment item controller.
 * Provides CRUD operations for payment items with filtering and pagination support.
 */
@Tag(name = "Payment Items", description = "Payment item management endpoints")
@RestController
@RequestMapping("/payment-items")
@RequiredArgsConstructor
public class PaymentItemController {
    
    private final PaymentItemService paymentItemService;
    
    @Operation(summary = "Create payment item", description = "Create a new payment item with categories and fees")
    @PostMapping
    public ResponseEntity<PaymentItemReadResponse> createPaymentItem(
            @Valid @RequestBody PaymentItemCreateRequest request,
            Authentication authentication) {
        
        PaymentItemReadResponse response = paymentItemService.createPaymentItem(
                request, authentication.getName());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "List payment items", description = "Get all payment items with optional filtering")
    @GetMapping
    public ResponseEntity<List<PaymentItemReadResponse>> listPaymentItems(
            @Parameter(description = "Filter for expenses only")
            @RequestParam(required = false) Boolean expenseOnly,
            
            @Parameter(description = "Filter for incomes only")
            @RequestParam(required = false) Boolean incomeOnly,
            
            @Parameter(description = "Filter by category IDs (OR logic, includes descendants)")
            @RequestParam(required = false) List<Long> categoryIds,

            @Parameter(description = "Only items on or after this date-time (ISO-8601)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,

            @Parameter(description = "Only items on or before this date-time (ISO-8601)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,

            Authentication authentication) {

        List<PaymentItemReadResponse> items = paymentItemService.getAllPaymentItems(
                expenseOnly, incomeOnly, categoryIds, dateFrom, dateTo, authentication.getName());

        return ResponseEntity.ok(items);
    }
    
    @Operation(summary = "Get payment item", description = "Get a single payment item by ID")
    @GetMapping("/{itemId}")
    public ResponseEntity<PaymentItemReadResponse> getPaymentItem(
            @PathVariable Long itemId,
            Authentication authentication) {
        
        PaymentItemReadResponse response = paymentItemService.getPaymentItemById(
                itemId, authentication.getName());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Update payment item", description = "Update an existing payment item")
    @PutMapping("/{itemId}")
    public ResponseEntity<PaymentItemReadResponse> updatePaymentItem(
            @PathVariable Long itemId,
            @Valid @RequestBody PaymentItemUpdateRequest request,
            Authentication authentication) {
        
        PaymentItemReadResponse response = paymentItemService.updatePaymentItem(
                itemId, request, authentication.getName());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Delete payment item", description = "Delete a payment item and refund fees")
    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deletePaymentItem(
            @PathVariable Long itemId,
            Authentication authentication) {
        
        paymentItemService.deletePaymentItem(itemId, authentication.getName());
        
        return ResponseEntity.noContent().build();
    }
}
