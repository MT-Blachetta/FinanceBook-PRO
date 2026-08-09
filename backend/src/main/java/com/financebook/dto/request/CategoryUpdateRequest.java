package com.financebook.dto.request;

import com.financebook.util.Constants;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Category update request DTO.
 * Mirrors Python's CategoryUpdate schema.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUpdateRequest {
    
    @Size(max = Constants.MAX_CATEGORY_NAME_LENGTH)
    private String name;
    
    private Long typeId;
    
    private Long parentId;
    
    private String iconFile;
}
