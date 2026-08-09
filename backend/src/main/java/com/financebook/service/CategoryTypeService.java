package com.financebook.service;

import com.financebook.dto.request.CategoryTypeCreateRequest;
import com.financebook.dto.response.CategoryTypeReadResponse;
import com.financebook.entity.CategoryType;
import com.financebook.entity.User;
import com.financebook.exception.ResourceNotFoundException;
import com.financebook.exception.UnauthorizedException;
import com.financebook.exception.ValidationException;
import com.financebook.repository.CategoryRepository;
import com.financebook.repository.CategoryTypeRepository;
import com.financebook.repository.UserRepository;
import com.financebook.util.Constants;
import com.financebook.util.DtoMapper;
import com.financebook.util.NameNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Category type service for managing classification dimensions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryTypeService {

    private final CategoryTypeRepository categoryTypeRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * Create a new category type with normalized, per-user unique name.
     */
    @Transactional
    public CategoryTypeReadResponse createCategoryType(
            CategoryTypeCreateRequest request,
            String username) {

        User user = getUserByUsername(username);

        String normalizedName = NameNormalizer.normalize(request.getName());
        if (normalizedName.isEmpty()) {
            throw new ValidationException("Category type name cannot be empty");
        }
        if (normalizedName.length() > Constants.MAX_CATEGORY_NAME_LENGTH) {
            throw new ValidationException("Category type name exceeds maximum length");
        }
        if (categoryTypeRepository.findByUserIdAndName(user.getId(), normalizedName).isPresent()) {
            throw new ValidationException("Category type name already exists");
        }

        CategoryType categoryType = CategoryType.builder()
                .name(normalizedName)
                .description(request.getDescription())
                .user(user)
                .build();

        categoryType = categoryTypeRepository.save(categoryType);

        return DtoMapper.toCategoryTypeReadResponse(categoryType);
    }

    /**
     * Get all category types for the current user.
     */
    @Transactional(readOnly = true)
    public List<CategoryTypeReadResponse> getAllCategoryTypes(String username) {
        User user = getUserByUsername(username);
        List<CategoryType> categoryTypes = categoryTypeRepository.findByUserId(user.getId());
        return DtoMapper.toCategoryTypeReadResponseList(categoryTypes);
    }

    /**
     * Delete a category type. The built-in "standard" type cannot be deleted and
     * a type that still has categories must be emptied first.
     */
    @Transactional
    public void deleteCategoryType(Long typeId, String username) {
        User user = getUserByUsername(username);

        CategoryType type = categoryTypeRepository.findById(typeId)
                .orElseThrow(() -> new ResourceNotFoundException("Category type not found"));
        if (!type.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Category type does not belong to you");
        }
        if (Constants.STANDARD_CATEGORY_TYPE_NAME.equals(type.getName())) {
            throw new ValidationException("The built-in 'standard' category type cannot be deleted");
        }
        if (!categoryRepository.findByUserIdAndTypeId(user.getId(), typeId).isEmpty()) {
            throw new ValidationException(
                    "Category type still has categories - delete or move them first");
        }

        categoryTypeRepository.delete(type);
        log.info("Deleted category type {} ({})", typeId, type.getName());
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
