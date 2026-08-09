package com.financebook.controller;

import com.financebook.dto.request.CategoryCreateRequest;
import com.financebook.dto.request.CategoryUpdateRequest;
import com.financebook.dto.response.CategoryReadResponse;
import com.financebook.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Category controller.
 * Provides category management with hierarchical tree operations.
 */
@Tag(name = "Categories", description = "Category management endpoints")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CategoryService categoryService;
    
    @Operation(summary = "Create category", description = "Create a new category")
    @PostMapping
    public ResponseEntity<CategoryReadResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request,
            Authentication authentication) {
        
        CategoryReadResponse response = categoryService.createCategory(
                request, authentication.getName());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "List all categories", description = "Get all categories for the current user")
    @GetMapping
    public ResponseEntity<List<CategoryReadResponse>> listCategories(
            Authentication authentication) {
        
        List<CategoryReadResponse> categories = categoryService.getAllCategories(
                authentication.getName());
        
        return ResponseEntity.ok(categories);
    }
    
    @Operation(summary = "Get category", description = "Get a single category by ID")
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryReadResponse> getCategory(
            @PathVariable Long categoryId,
            Authentication authentication) {
        
        CategoryReadResponse response = categoryService.getCategoryById(
                categoryId, authentication.getName());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get category tree", description = "Get category with its full tree structure")
    @GetMapping("/{categoryId}/tree")
    public ResponseEntity<CategoryReadResponse> getCategoryTree(
            @PathVariable Long categoryId,
            Authentication authentication) {
        
        CategoryReadResponse response = categoryService.getCategoryById(
                categoryId, authentication.getName());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get category descendants", description = "Get all descendant categories")
    @GetMapping("/{categoryId}/descendants")
    public ResponseEntity<List<CategoryReadResponse>> getCategoryDescendants(
            @PathVariable Long categoryId,
            Authentication authentication) {
        
        List<CategoryReadResponse> descendants = categoryService.getDescendants(
                categoryId, authentication.getName());
        
        return ResponseEntity.ok(descendants);
    }
    
    @Operation(summary = "Get categories by type", description = "Get all categories of a specific type")
    @GetMapping("/by-type/{typeId}")
    public ResponseEntity<List<CategoryReadResponse>> getCategoriesByType(
            @PathVariable Long typeId,
            Authentication authentication) {
        
        List<CategoryReadResponse> categories = categoryService.getCategoriesByType(
                typeId, authentication.getName());
        
        return ResponseEntity.ok(categories);
    }
    
    @Operation(summary = "Update category", description = "Update an existing category")
    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryReadResponse> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request,
            Authentication authentication) {

        CategoryReadResponse response = categoryService.updateCategory(
                categoryId, request, authentication.getName());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete category",
            description = "Delete a category; children are re-parented and payment items fall back to UNCLASSIFIED")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long categoryId,
            Authentication authentication) {

        categoryService.deleteCategory(categoryId, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
