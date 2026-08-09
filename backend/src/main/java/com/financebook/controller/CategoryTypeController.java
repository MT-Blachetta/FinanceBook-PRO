package com.financebook.controller;

import com.financebook.dto.request.CategoryTypeCreateRequest;
import com.financebook.dto.response.CategoryTypeReadResponse;
import com.financebook.service.CategoryTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Category type controller.
 * Provides category type management endpoints.
 */
@Tag(name = "Category Types", description = "Category type management endpoints")
@RestController
@RequestMapping("/category-types")
@RequiredArgsConstructor
public class CategoryTypeController {
    
    private final CategoryTypeService categoryTypeService;
    
    @Operation(summary = "Create category type", description = "Create a new category type")
    @PostMapping
    public ResponseEntity<CategoryTypeReadResponse> createCategoryType(
            @Valid @RequestBody CategoryTypeCreateRequest request,
            Authentication authentication) {
        
        CategoryTypeReadResponse response = categoryTypeService.createCategoryType(
                request, authentication.getName());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "List category types", description = "Get all category types for the current user")
    @GetMapping
    public ResponseEntity<List<CategoryTypeReadResponse>> listCategoryTypes(
            Authentication authentication) {

        List<CategoryTypeReadResponse> types = categoryTypeService.getAllCategoryTypes(
                authentication.getName());

        return ResponseEntity.ok(types);
    }

    @Operation(summary = "Delete category type",
            description = "Delete an empty, non-standard category type")
    @DeleteMapping("/{typeId}")
    public ResponseEntity<Void> deleteCategoryType(
            @PathVariable Long typeId,
            Authentication authentication) {

        categoryTypeService.deleteCategoryType(typeId, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
