package com.financebook.service;

import com.financebook.dto.response.ImportCsvSummary;
import com.financebook.entity.*;
import com.financebook.exception.ResourceNotFoundException;
import com.financebook.exception.ValidationException;
import com.financebook.repository.*;
import com.financebook.util.Constants;
import com.financebook.util.NameNormalizer;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Optional;

/**
 * CSV import for payment items.
 *
 * Expected columns (semicolon separated, one header line):
 * amount;date;description;Recipient name;Recipient address;standard_category name;periodic
 *
 * Rows that cannot be parsed are skipped and counted; the import never fails
 * as a whole because of a single bad line.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final PaymentItemRepository paymentItemRepository;
    private final RecipientRepository recipientRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryTypeRepository categoryTypeRepository;
    private final UserRepository userRepository;

    @Transactional
    public ImportCsvSummary importCsv(MultipartFile file, String username)
            throws IOException, CsvValidationException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (file.isEmpty()) {
            throw new ValidationException("CSV file is empty");
        }

        int createdPayments = 0;
        int createdRecipients = 0;
        int updatedRecipients = 0;
        int createdCategories = 0;
        int skippedRows = 0;

        // Configure CSV parser with semicolon separator
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(';')
                .withIgnoreQuotations(false)
                .build();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                .withCSVParser(parser)
                .withSkipLines(1) // Skip header
                .build()) {

            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 7) {
                    log.warn("Skipping invalid CSV line (expected 7 columns, got {})", line.length);
                    skippedRows++;
                    continue;
                }

                try {
                    BigDecimal amount = new BigDecimal(line[0].trim());
                    if (amount.scale() > 2 || amount.abs().compareTo(Constants.MAX_PAYMENT_AMOUNT) > 0) {
                        throw new ValidationException("Amount out of range: " + amount);
                    }
                    LocalDate parsedDate = LocalDate.parse(line[1].trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                    String description = truncate(line[2], Constants.MAX_DESCRIPTION_LENGTH);
                    String recipientName = NameNormalizer.normalize(line[3]);
                    String recipientAddress = truncate(line[4], Constants.MAX_RECIPIENT_ADDRESS_LENGTH);
                    String categoryName = NameNormalizer.normalize(line[5]);
                    boolean periodic = Boolean.parseBoolean(line[6].trim());

                    if (recipientName.length() > Constants.MAX_RECIPIENT_NAME_LENGTH
                            || categoryName.length() > Constants.MAX_CATEGORY_NAME_LENGTH) {
                        throw new ValidationException("Name exceeds maximum length");
                    }

                    // Handle Recipient
                    Recipient recipient = null;
                    if (!recipientName.isBlank()) {
                        Optional<Recipient> existingRecipient =
                                recipientRepository.findByUserIdAndName(user.getId(), recipientName);
                        if (existingRecipient.isPresent()) {
                            recipient = existingRecipient.get();
                            // Update address if provided and currently empty
                            if (recipientAddress != null && !recipientAddress.isBlank() &&
                                    (recipient.getAddress() == null || recipient.getAddress().isBlank())) {
                                recipient.setAddress(recipientAddress);
                                recipientRepository.save(recipient);
                                updatedRecipients++;
                            }
                        } else {
                            recipient = recipientRepository.save(Recipient.builder()
                                    .name(recipientName)
                                    .address(recipientAddress == null || recipientAddress.isBlank()
                                            ? null : recipientAddress)
                                    .user(user)
                                    .build());
                            createdRecipients++;
                        }
                    }

                    // Handle Category (always attached under the "standard" type)
                    Category category = null;
                    if (!categoryName.isBlank()) {
                        Optional<Category> existingCategory =
                                categoryRepository.findByUserIdAndName(user.getId(), categoryName);
                        if (existingCategory.isPresent()) {
                            category = existingCategory.get();
                        } else {
                            CategoryType standardType = categoryTypeRepository
                                    .findByUserIdAndName(user.getId(), Constants.STANDARD_CATEGORY_TYPE_NAME)
                                    .orElseGet(() -> categoryTypeRepository.save(CategoryType.builder()
                                            .name(Constants.STANDARD_CATEGORY_TYPE_NAME)
                                            .description("Default category type for imports")
                                            .user(user)
                                            .build()));

                            category = categoryRepository.save(Category.builder()
                                    .name(categoryName)
                                    .type(standardType)
                                    .user(user)
                                    .build());
                            createdCategories++;
                        }
                    }

                    PaymentItem paymentItem = PaymentItem.builder()
                            .amount(amount)
                            .date(parsedDate.atStartOfDay())
                            .description(description)
                            .periodic(periodic)
                            .user(user)
                            .recipient(recipient)
                            .standardCategory(category)
                            .categories(new HashSet<>())
                            .build();

                    if (category != null) {
                        paymentItem.getCategories().add(category);
                    }

                    paymentItemRepository.save(paymentItem);
                    createdPayments++;

                } catch (Exception e) {
                    log.warn("Skipping CSV line, could not process it: {}", e.getMessage());
                    skippedRows++;
                }
            }
        }

        return ImportCsvSummary.builder()
                .created_payments(createdPayments)
                .created_recipients(createdRecipients)
                .updated_recipients(updatedRecipients)
                .created_categories(createdCategories)
                .skipped_rows(skippedRows)
                .build();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
