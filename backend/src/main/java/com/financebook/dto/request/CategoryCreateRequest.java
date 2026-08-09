package com.financebook.dto.request;

import com.financebook.util.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Category creation request DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreateRequest {
    
    @NotBlank(message = "Category name is required")
    @Size(max = Constants.MAX_CATEGORY_NAME_LENGTH)
    private String name;
    
    @NotNull(message = "Type ID is required")
    private Long typeId;
    
    private Long parentId;
    
    private String iconFile;
}
