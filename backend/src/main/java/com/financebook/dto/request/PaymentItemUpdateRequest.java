package com.financebook.dto.request;

import com.financebook.util.Constants;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Payment item update request DTO.
 * Mirrors Python's PaymentItemUpdate schema.
 * All fields optional for partial updates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentItemUpdateRequest {
    
    private BigDecimal amount;
    
    private LocalDateTime date;
    
    private Boolean periodic;
    
    @Size(max = Constants.MAX_DESCRIPTION_LENGTH)
    private String description;
    
    private Long recipientId;
    
    private List<Long> categoryIds;
}
