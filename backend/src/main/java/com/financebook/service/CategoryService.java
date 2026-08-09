package com.financebook.service;

import com.financebook.dto.request.CategoryCreateRequest;
import com.financebook.dto.request.CategoryUpdateRequest;
import com.financebook.dto.response.CategoryReadResponse;
import com.financebook.entity.Category;
import com.financebook.entity.CategoryType;
import com.financebook.entity.PaymentItem;
import com.financebook.entity.User;
import com.financebook.exception.ResourceNotFoundException;
import com.financebook.exception.UnauthorizedException;
import com.financebook.exception.ValidationException;
import com.financebook.repository.CategoryRepository;
import com.financebook.repository.CategoryTypeRepository;
import com.financebook.repository.PaymentItemRepository;
import com.financebook.repository.UserRepository;
import com.financebook.util.Constants;
import com.financebook.util.DtoMapper;
import com.financebook.util.NameNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Category service for managing hierarchical category trees.
 * Implements tree operations: descendants, ancestors, depth.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryTypeRepository categoryTypeRepository;
    private final PaymentItemRepository paymentItemRepository;
    private final UserRepository userRepository;

    /**
     * Create a new category with normalized name.
     */
    @Transactional
    public CategoryReadResponse createCategory(CategoryCreateRequest request, String username) {
        User user = getUserByUsername(username);

        String normalizedName = normalizeAndValidateName(request.getName());

        // Check for duplicate name (per user)
        if (categoryRepository.findByUserIdAndName(user.getId(), normalizedName).isPresent()) {
            throw new ValidationException("Category name already exists");
        }

        // Validate type exists and belongs to the user
        CategoryType type = getOwnedType(request.getTypeId(), user);

        // Validate parent if provided: must belong to the user and to the same type
        Category parent = null;
        if (request.getParentId() != null) {
            parent = getOwnedCategory(request.getParentId(), user);
            if (!parent.getType().getId().equals(type.getId())) {
                throw new ValidationException("Parent category must belong to the same category type");
            }
        }

        Category category = Category.builder()
                .name(normalizedName)
                .type(type)
                .parent(parent)
                .iconFile(request.getIconFile())
                .user(user)
                .build();

        category = categoryRepository.save(category);

        return DtoMapper.toCategoryReadResponse(category);
    }

    /**
     * Get all categories for the current user.
     */
    @Transactional(readOnly = true)
    public List<CategoryReadResponse> getAllCategories(String username) {
        User user = getUserByUsername(username);
        List<Category> categories = categoryRepository.findByUserId(user.getId());
        return DtoMapper.toCategoryReadResponseList(categories);
    }

    /**
     * Get a single category by ID.
     */
    @Transactional(readOnly = true)
    public CategoryReadResponse getCategoryById(Long categoryId, String username) {
        User user = getUserByUsername(username);
        Category category = getOwnedCategory(categoryId, user);
        return DtoMapper.toCategoryReadResponse(category);
    }

    /**
     * Get categories by type ID.
     */
    @Transactional(readOnly = true)
    public List<CategoryReadResponse> getCategoriesByType(Long typeId, String username) {
        User user = getUserByUsername(username);
        List<Category> categories = categoryRepository.findByUserIdAndTypeId(user.getId(), typeId);
        return DtoMapper.toCategoryReadResponseList(categories);
    }

    /**
     * Get all descendant categories of a given category.
     */
    @Transactional(readOnly = true)
    public List<CategoryReadResponse> getDescendants(Long categoryId, String username) {
        User user = getUserByUsername(username);
        getOwnedCategory(categoryId, user); // ownership check

        List<Category> descendants = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(categoryId);
        visited.add(categoryId);

        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            for (Category child : categoryRepository.findByParentId(currentId)) {
                if (visited.add(child.getId())) {
                    descendants.add(child);
                    queue.add(child.getId());
                }
            }
        }

        return DtoMapper.toCategoryReadResponseList(descendants);
    }

    /**
     * Get all descendant IDs (including the roots).
     * Used for filtering payment items by category.
     */
    @Transactional(readOnly = true)
    public Set<Long> expandCategoryIds(Set<Long> categoryIds, Long userId) {
        Set<Long> expandedIds = new HashSet<>(categoryIds);

        Deque<Long> queue = new ArrayDeque<>(categoryIds);
        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            for (Category child : categoryRepository.findByParentId(currentId)) {
                if (expandedIds.add(child.getId())) {
                    queue.add(child.getId());
                }
            }
        }

        return expandedIds;
    }

    /**
     * Update an existing category.
     * Validates ownership of all referenced entities and prevents cycles in the
     * category tree (a category can never become its own ancestor).
     */
    @Transactional
    public CategoryReadResponse updateCategory(
            Long categoryId,
            CategoryUpdateRequest request,
            String username) {

        User user = getUserByUsername(username);
        Category category = getOwnedCategory(categoryId, user);

        if (request.getName() != null) {
            String normalizedName = normalizeAndValidateName(request.getName());

            categoryRepository.findByUserIdAndName(user.getId(), normalizedName)
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(categoryId)) {
                            throw new ValidationException("Category name already exists");
                        }
                    });

            category.setName(normalizedName);
        }

        if (request.getTypeId() != null
                && !request.getTypeId().equals(category.getType().getId())) {
            CategoryType type = getOwnedType(request.getTypeId(), user);
            if (!categoryRepository.findByParentId(categoryId).isEmpty()) {
                throw new ValidationException(
                        "Cannot change the type of a category that has child categories");
            }
            category.setType(type);
            if (category.getParent() != null
                    && !category.getParent().getType().getId().equals(type.getId())) {
                // Detach from the old tree: parent belongs to a different taxonomy now
                category.setParent(null);
            }
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(categoryId)) {
                throw new ValidationException("A category cannot be its own parent");
            }
            Category parent = getOwnedCategory(request.getParentId(), user);
            if (!parent.getType().getId().equals(category.getType().getId())) {
                throw new ValidationException("Parent category must belong to the same category type");
            }
            // Cycle guard: the new parent must not be a descendant of this category
            Set<Long> subtree = expandCategoryIds(Set.of(categoryId), user.getId());
            if (subtree.contains(parent.getId())) {
                throw new ValidationException(
                        "Cannot move a category below one of its own descendants");
            }
            category.setParent(parent);
        }

        if (request.getIconFile() != null) {
            category.setIconFile(request.getIconFile());
        }

        category = categoryRepository.save(category);

        return DtoMapper.toCategoryReadResponse(category);
    }

    /**
     * Delete a category.
     *
     * • Children are re-parented to the deleted category's parent (subtrees are
     *   never deleted implicitly).
     * • Payment items lose the link to this category; items whose standard
     *   category pointed at it are reset to the UNCLASSIFIED default.
     * • The UNCLASSIFIED default itself cannot be deleted.
     */
    @Transactional
    public void deleteCategory(Long categoryId, String username) {
        User user = getUserByUsername(username);
        Category category = getOwnedCategory(categoryId, user);

        if (Constants.UNCLASSIFIED_CATEGORY_NAME.equals(category.getName())) {
            throw new ValidationException("The default UNCLASSIFIED category cannot be deleted");
        }

        // Re-parent children
        for (Category child : categoryRepository.findByParentId(categoryId)) {
            child.setParent(category.getParent());
            categoryRepository.save(child);
        }

        Category unclassified = categoryRepository
                .findByUserIdAndName(user.getId(), Constants.UNCLASSIFIED_CATEGORY_NAME)
                .orElse(null);

        // Unlink payment items referencing this category
        for (PaymentItem item : paymentItemRepository.findByLinkedCategoryId(categoryId)) {
            item.getCategories().removeIf(c -> c.getId().equals(categoryId));
            if (item.getCategories().isEmpty() && unclassified != null) {
                item.getCategories().add(unclassified);
            }
            paymentItemRepository.save(item);
        }
        for (PaymentItem item : paymentItemRepository.findByStandardCategoryId(categoryId)) {
            item.setStandardCategory(unclassified);
            if (unclassified != null && item.getCategories().stream()
                    .noneMatch(c -> c.getId().equals(unclassified.getId()))) {
                item.getCategories().add(unclassified);
            }
            paymentItemRepository.save(item);
        }

        categoryRepository.delete(category);
        log.info("Deleted category {} ({})", categoryId, category.getName());
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private String normalizeAndValidateName(String rawName) {
        String normalizedName = NameNormalizer.normalize(rawName);
        if (normalizedName.isEmpty()) {
            throw new ValidationException("Category name cannot be empty");
        }
        if (normalizedName.length() > Constants.MAX_CATEGORY_NAME_LENGTH) {
            throw new ValidationException("Category name exceeds maximum length");
        }
        return normalizedName;
    }

    private Category getOwnedCategory(Long categoryId, User user) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!category.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to access this category");
        }
        return category;
    }

    private CategoryType getOwnedType(Long typeId, User user) {
        CategoryType type = categoryTypeRepository.findById(typeId)
                .orElseThrow(() -> new ResourceNotFoundException("Category type not found"));
        if (!type.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Category type does not belong to you");
        }
        return type;
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
