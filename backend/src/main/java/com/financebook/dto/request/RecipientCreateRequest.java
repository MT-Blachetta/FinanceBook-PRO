package com.financebook.dto.request;

import com.financebook.util.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Recipient creation request DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipientCreateRequest {
    
    @NotBlank(message = "Recipient name is required")
    @Size(max = Constants.MAX_RECIPIENT_NAME_LENGTH)
    private String name;
    
    @Size(max = Constants.MAX_RECIPIENT_ADDRESS_LENGTH)
    private String address;
}
