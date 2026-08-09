package com.financebook.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Category type read response DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryTypeReadResponse {
    
    private Long id;
    private String name;
    private String description;
}
