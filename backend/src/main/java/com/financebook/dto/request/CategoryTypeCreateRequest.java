package com.financebook.dto.request;

import com.financebook.util.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Category type creation request DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTypeCreateRequest {
    
    @NotBlank(message = "Category type name is required")
    @Size(max = Constants.MAX_CATEGORY_NAME_LENGTH)
    private String name;
    
    @Size(max = Constants.MAX_DESCRIPTION_LENGTH)
    private String description;
}
