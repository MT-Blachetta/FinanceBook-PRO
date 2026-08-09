package com.financebook.service;

import com.financebook.dto.request.PaymentItemCreateRequest;
import com.financebook.dto.request.PaymentItemUpdateRequest;
import com.financebook.dto.response.PaymentItemReadResponse;
import com.financebook.entity.*;
import com.financebook.exception.ResourceNotFoundException;
import com.financebook.exception.UnauthorizedException;
import com.financebook.exception.ValidationException;
import com.financebook.repository.*;
import com.financebook.util.Constants;
import com.financebook.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Payment item service with CRUD operations.
 * Implements category validation, fee calculation, and multi-user isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentItemService {

    private final PaymentItemRepository paymentItemRepository;
    private final RecipientRepository recipientRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryTypeRepository categoryTypeRepository;
    private final UserRepository userRepository;
    private final TransactionFeeRecordRepository feeRecordRepository;
    private final FeeEngineService feeEngineService;
    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;

    /**
     * Resolved categories for a payment item: the validated category set and the
     * category of the user's "standard" type (if one was selected).
     */
    private record ResolvedCategories(Set<Category> categories, Category standardCategory) {
    }

    /**
     * Create a new payment item with categories and fee application.
     */
    @Transactional
    public PaymentItemReadResponse createPaymentItem(
            PaymentItemCreateRequest request,
            String username) {

        User user = getUserByUsername(username);
        validateAmount(request.getAmount());

        Recipient recipient = request.getRecipientId() != null
                ? getOwnedRecipient(request.getRecipientId(), user)
                : null;

        ResolvedCategories resolved = resolveCategories(request.getCategoryIds(), user);

        PaymentItem item = PaymentItem.builder()
                .amount(request.getAmount())
                .date(request.getDate())
                .periodic(request.getPeriodic() != null ? request.getPeriodic() : false)
                .description(request.getDescription())
                .recipient(recipient)
                .standardCategory(resolved.standardCategory())
                .user(user)
                .categories(resolved.categories())
                .build();

        item = paymentItemRepository.save(item);

        BigDecimal fee = feeEngineService.createFeeRecord(item, user.getId());

        PaymentItemReadResponse response = DtoMapper.toPaymentItemReadResponse(item);
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            response.setTransactionFee(fee);
        }

        return response;
    }

    /**
     * Get all payment items with optional filtering by direction (expense/income),
     * categories (descendants included) and date range.
     */
    @Transactional(readOnly = true)
    public List<PaymentItemReadResponse> getAllPaymentItems(
            Boolean expenseOnly,
            Boolean incomeOnly,
            List<Long> categoryIds,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String username) {

        User user = getUserByUsername(username);

        if (Boolean.TRUE.equals(expenseOnly) && Boolean.TRUE.equals(incomeOnly)) {
            throw new ValidationException("Choose only one filter: expense_only or income_only");
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new ValidationException("date_from must not be after date_to");
        }

        List<PaymentItem> items;

        if (categoryIds != null && !categoryIds.isEmpty()) {
            // Expand category list to include all descendants
            Set<Long> expandedIds = categoryService.expandCategoryIds(
                    new HashSet<>(categoryIds), user.getId());

            if (Boolean.TRUE.equals(expenseOnly)) {
                items = paymentItemRepository.findExpensesByUserIdAndCategoryIdsOrderByDateDesc(
                        user.getId(), expandedIds);
            } else if (Boolean.TRUE.equals(incomeOnly)) {
                items = paymentItemRepository.findIncomesByUserIdAndCategoryIdsOrderByDateDesc(
                        user.getId(), expandedIds);
            } else {
                items = paymentItemRepository.findByUserIdAndCategoryIdsOrderByDateDesc(
                        user.getId(), expandedIds);
            }
        } else if (Boolean.TRUE.equals(expenseOnly)) {
            items = paymentItemRepository.findExpensesByUserIdOrderByDateDesc(user.getId());
        } else if (Boolean.TRUE.equals(incomeOnly)) {
            items = paymentItemRepository.findIncomesByUserIdOrderByDateDesc(user.getId());
        } else {
            items = paymentItemRepository.findByUserIdOrderByDateDesc(user.getId());
        }

        if (dateFrom != null || dateTo != null) {
            items = items.stream()
                    .filter(i -> (dateFrom == null || !i.getDate().isBefore(dateFrom))
                            && (dateTo == null || !i.getDate().isAfter(dateTo)))
                    .collect(Collectors.toList());
        }

        attachTransactionFees(items);

        return DtoMapper.toPaymentItemReadResponseList(items);
    }

    /**
     * Get a single payment item by ID.
     */
    @Transactional(readOnly = true)
    public PaymentItemReadResponse getPaymentItemById(Long itemId, String username) {
        User user = getUserByUsername(username);
        PaymentItem item = getOwnedPaymentItem(itemId, user);

        feeRecordRepository.findByPaymentItemId(itemId)
                .ifPresent(record -> item.setTransactionFee(record.getFeeAmount()));

        return DtoMapper.toPaymentItemReadResponse(item);
    }

    /**
     * Update an existing payment item. When the amount changes, the transaction
     * fee is recomputed: the previous fee record is refunded and a new fee is
     * applied to the new gross amount.
     */
    @Transactional
    public PaymentItemReadResponse updatePaymentItem(
            Long itemId,
            PaymentItemUpdateRequest request,
            String username) {

        User user = getUserByUsername(username);
        PaymentItem item = getOwnedPaymentItem(itemId, user);

        boolean amountChanged = request.getAmount() != null
                && item.getAmount().compareTo(request.getAmount()) != 0;

        if (request.getAmount() != null) {
            validateAmount(request.getAmount());
            item.setAmount(request.getAmount());
        }
        if (request.getDate() != null) {
            item.setDate(request.getDate());
        }
        if (request.getPeriodic() != null) {
            item.setPeriodic(request.getPeriodic());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }

        if (request.getRecipientId() != null) {
            item.setRecipient(getOwnedRecipient(request.getRecipientId(), user));
        }

        if (request.getCategoryIds() != null) {
            ResolvedCategories resolved = resolveCategories(request.getCategoryIds(), user);
            item.getCategories().clear();
            item.getCategories().addAll(resolved.categories());
            item.setStandardCategory(resolved.standardCategory());
        }

        item = paymentItemRepository.save(item);

        BigDecimal fee;
        if (amountChanged) {
            fee = feeEngineService.recomputeFeeRecord(item, user.getId());
        } else {
            fee = feeRecordRepository.findByPaymentItemId(itemId)
                    .map(TransactionFeeRecord::getFeeAmount)
                    .orElse(BigDecimal.ZERO);
        }

        PaymentItemReadResponse response = DtoMapper.toPaymentItemReadResponse(item);
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            response.setTransactionFee(fee);
        }

        log.info("Updated payment item {}", itemId);
        return response;
    }

    /**
     * Delete a payment item, its invoice file, and refund its transaction fee.
     */
    @Transactional
    public void deletePaymentItem(Long itemId, String username) {
        User user = getUserByUsername(username);
        PaymentItem item = getOwnedPaymentItem(itemId, user);

        if (item.getInvoicePath() != null) {
            fileStorageService.deleteInvoiceFileQuietly(item.getInvoicePath());
        }

        feeEngineService.refundFeeRecord(itemId);
        paymentItemRepository.delete(item);

        log.info("Deleted payment item {}", itemId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    /**
     * Validate and resolve the requested category IDs for the given user.
     * Enforces ownership and the "one category per type" rule. When no IDs are
     * given, the user's default UNCLASSIFIED category is assigned (if present).
     */
    private ResolvedCategories resolveCategories(List<Long> categoryIds, User user) {
        Long standardTypeId = categoryTypeRepository
                .findByUserIdAndName(user.getId(), Constants.STANDARD_CATEGORY_TYPE_NAME)
                .map(CategoryType::getId)
                .orElse(null);

        Set<Category> categories = new HashSet<>();
        Category standardCategory = null;

        if (categoryIds != null && !categoryIds.isEmpty()) {
            Set<Long> seenTypes = new HashSet<>();

            for (Long catId : categoryIds) {
                Category category = categoryRepository.findById(catId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Category with id " + catId + " not found"));

                if (!category.getUser().getId().equals(user.getId())) {
                    throw new UnauthorizedException("Category " + catId + " does not belong to you");
                }
                if (!seenTypes.add(category.getType().getId())) {
                    throw new ValidationException("Only one category per type is allowed");
                }

                categories.add(category);
                if (category.getType().getId().equals(standardTypeId)) {
                    standardCategory = category;
                }
            }
        } else {
            Category defaultCat = categoryRepository
                    .findByUserIdAndName(user.getId(), Constants.UNCLASSIFIED_CATEGORY_NAME)
                    .orElse(null);
            if (defaultCat != null) {
                categories.add(defaultCat);
                if (defaultCat.getType().getId().equals(standardTypeId)) {
                    standardCategory = defaultCat;
                }
            }
        }

        return new ResolvedCategories(categories, standardCategory);
    }

    /** Attach persisted fee amounts to a list of items in a single query. */
    private void attachTransactionFees(List<PaymentItem> items) {
        if (items.isEmpty()) {
            return;
        }
        List<Long> itemIds = items.stream().map(PaymentItem::getId).collect(Collectors.toList());
        Map<Long, BigDecimal> feeMap = feeRecordRepository.findByPaymentItemIdIn(itemIds).stream()
                .collect(Collectors.toMap(
                        r -> r.getPaymentItem().getId(),
                        TransactionFeeRecord::getFeeAmount));

        for (PaymentItem item : items) {
            BigDecimal fee = feeMap.get(item.getId());
            if (fee != null) {
                item.setTransactionFee(fee);
            }
        }
    }

    /** The DB column is NUMERIC(10,2): reject amounts that would not fit or that carry sub-cent digits. */
    private void validateAmount(BigDecimal amount) {
        if (amount.scale() > 2) {
            throw new ValidationException("Amount must not have more than two decimal places");
        }
        if (amount.abs().compareTo(Constants.MAX_PAYMENT_AMOUNT) > 0) {
            throw new ValidationException("Amount exceeds the supported maximum of "
                    + Constants.MAX_PAYMENT_AMOUNT);
        }
    }

    private PaymentItem getOwnedPaymentItem(Long itemId, User user) {
        PaymentItem item = paymentItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        if (!item.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to access this item");
        }
        return item;
    }

    private Recipient getOwnedRecipient(Long recipientId, User user) {
        Recipient recipient = recipientRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));
        if (!recipient.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Recipient does not belong to you");
        }
        return recipient;
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
