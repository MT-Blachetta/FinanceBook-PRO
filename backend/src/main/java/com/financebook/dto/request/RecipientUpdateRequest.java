package com.financebook.dto.request;

import com.financebook.util.Constants;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Recipient update request DTO.
 * Mirrors Python's RecipientUpdate schema.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipientUpdateRequest {
    
    @Size(max = Constants.MAX_RECIPIENT_NAME_LENGTH)
    private String name;
    
    @Size(max = Constants.MAX_RECIPIENT_ADDRESS_LENGTH)
    private String address;
}
