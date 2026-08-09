package com.financebook.util;

import com.financebook.dto.response.*;
import com.financebook.entity.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for mapping entities to DTOs.
 * Centralizes all DTO conversion logic.
 */
@Slf4j
public final class DtoMapper {
    
    // Prevent instantiation
    private DtoMapper() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    // ─── User Mapping ────────────────────────────────────────────────
    
    public static UserReadResponse toUserReadResponse(User user) {
        if (user == null) {
            return null;
        }
        
        return UserReadResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .surname(user.getSurname())
                .prename(user.getPrename())
                .birthDate(user.getBirthDate())
                .phone(user.getPhone())
                .road(user.getRoad())
                .houseNumber(user.getHouseNumber())
                .region(user.getRegion())
                .postal(user.getPostal())
                .city(user.getCity())
                .state(user.getState())
                .isAdmin(user.getIsAdmin())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
    
    // ─── Recipient Mapping ───────────────────────────────────────────
    
    public static RecipientReadResponse toRecipientReadResponse(Recipient recipient) {
        if (recipient == null) {
            return null;
        }
        
        return RecipientReadResponse.builder()
                .id(recipient.getId())
                .name(recipient.getName())
                .address(recipient.getAddress())
                .build();
    }
    
    public static List<RecipientReadResponse> toRecipientReadResponseList(List<Recipient> recipients) {
        if (recipients == null) {
            return Collections.emptyList();
        }
        
        return recipients.stream()
                .map(DtoMapper::toRecipientReadResponse)
                .collect(Collectors.toList());
    }
    
    // ─── Category Mapping ────────────────────────────────────────────
    
    public static CategoryReadResponse toCategoryReadResponse(Category category) {
        if (category == null) {
            return null;
        }
        
        return CategoryReadResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .typeId(category.getType() != null ? category.getType().getId() : null)
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .iconFile(category.getIconFile())
                .children(category.getChildren() != null ? 
                        toCategoryReadResponseList(category.getChildren()) : null)
                .build();
    }
    
    public static List<CategoryReadResponse> toCategoryReadResponseList(List<Category> categories) {
        if (categories == null) {
            return Collections.emptyList();
        }
        
        return categories.stream()
                .map(DtoMapper::toCategoryReadResponse)
                .collect(Collectors.toList());
    }
    
    // ─── CategoryType Mapping ────────────────────────────────────────
    
    public static CategoryTypeReadResponse toCategoryTypeReadResponse(CategoryType categoryType) {
        if (categoryType == null) {
            return null;
        }
        
        return CategoryTypeReadResponse.builder()
                .id(categoryType.getId())
                .name(categoryType.getName())
                .description(categoryType.getDescription())
                .build();
    }
    
    public static List<CategoryTypeReadResponse> toCategoryTypeReadResponseList(List<CategoryType> categoryTypes) {
        if (categoryTypes == null) {
            return Collections.emptyList();
        }
        
        return categoryTypes.stream()
                .map(DtoMapper::toCategoryTypeReadResponse)
                .collect(Collectors.toList());
    }
    
    // ─── PaymentItem Mapping ─────────────────────────────────────────
    
    public static PaymentItemReadResponse toPaymentItemReadResponse(PaymentItem item) {
        if (item == null) {
            return null;
        }
        
        return PaymentItemReadResponse.builder()
                .id(item.getId())
                .amount(item.getAmount())
                .date(item.getDate())
                .periodic(item.getPeriodic())
                .description(item.getDescription())
                .invoicePath(item.getInvoicePath())
                .productImagePath(item.getProductImagePath())
                .recipient(toRecipientReadResponse(item.getRecipient()))
                .recipientId(item.getRecipient() != null ? item.getRecipient().getId() : null)
                .categories(item.getCategories() != null ?
                        item.getCategories().stream()
                                .map(DtoMapper::toCategoryReadResponse)
                                .collect(Collectors.toList()) : Collections.emptyList())
                .standardCategory(toCategoryReadResponse(item.getStandardCategory()))
                .standardCategoryId(item.getStandardCategory() != null ? item.getStandardCategory().getId() : null)
                .transactionFee(item.getTransactionFee())
                .build();
    }
    
    public static List<PaymentItemReadResponse> toPaymentItemReadResponseList(List<PaymentItem> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        
        return items.stream()
                .map(DtoMapper::toPaymentItemReadResponse)
                .collect(Collectors.toList());
    }
}
