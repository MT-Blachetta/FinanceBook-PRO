package com.financebook.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Recipient read response DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipientReadResponse {
    
    private Long id;
    private String name;
    private String address;
}
