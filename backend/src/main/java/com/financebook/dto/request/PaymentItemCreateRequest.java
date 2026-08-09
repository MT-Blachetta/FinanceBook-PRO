package com.financebook.dto.request;

import com.financebook.util.Constants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Payment item creation request DTO.
 * Mirrors Python's PaymentItemCreate schema.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentItemCreateRequest {
    
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    
    @NotNull(message = "Date is required")
    private LocalDateTime date;
    
    private Boolean periodic = false;
    
    @Size(max = Constants.MAX_DESCRIPTION_LENGTH, message = "Description exceeds maximum length")
    private String description;
    
    private Long recipientId;
    
    private List<Long> categoryIds;
}
