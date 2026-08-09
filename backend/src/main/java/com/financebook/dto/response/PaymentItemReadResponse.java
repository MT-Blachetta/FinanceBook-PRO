package com.financebook.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Payment item read response DTO.
 * Mirrors Python's PaymentItemRead schema.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentItemReadResponse {
    
    private Long id;
    private BigDecimal amount;
    private LocalDateTime date;
    private Boolean periodic;
    private String description;
    private String invoicePath;
    private String productImagePath;
    
    // Relationships
    private RecipientReadResponse recipient;
    private Long recipientId;
    private List<CategoryReadResponse> categories;
    private CategoryReadResponse standardCategory;
    private Long standardCategoryId;
    
    // Computed field
    private BigDecimal transactionFee;
}
