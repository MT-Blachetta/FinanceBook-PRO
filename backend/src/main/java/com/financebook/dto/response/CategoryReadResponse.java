package com.financebook.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Category read response DTO.
 * Supports hierarchical tree structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryReadResponse {
    
    private Long id;
    private String name;
    private Long typeId;
    private Long parentId;
    private String iconFile;
    
    // For tree representation
    private List<CategoryReadResponse> children;
}
