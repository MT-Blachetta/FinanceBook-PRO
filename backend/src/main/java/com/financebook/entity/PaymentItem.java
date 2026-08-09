package com.financebook.entity;

import com.financebook.util.Constants;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Core business entity representing a payment transaction.
 * 
 * Conventions:
 *   • Negative amount  → Expense (money out)
 *   • Positive amount  → Income (money in)
 *   • periodic=true marks recurring payments
 */
@Entity
@Table(name = "paymentitem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @NotNull
    @Column(nullable = false)
    private LocalDateTime date;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean periodic = false;
    
    @Size(max = Constants.MAX_DESCRIPTION_LENGTH)
    @Column(length = Constants.MAX_DESCRIPTION_LENGTH)
    private String description;
    
    // Optional attachments
    @Column(name = "invoice_path")
    private String invoicePath;
    
    @Column(name = "product_image_path")
    private String productImagePath;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private Recipient recipient;
    
    // Direct reference to standard category for efficient retrieval
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_category_id")
    private Category standardCategory;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // Many-to-many relationship with categories
    @ManyToMany
    @JoinTable(
        name = "paymentitemcategorylink",
        joinColumns = @JoinColumn(name = "payment_item_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();
    
    // Transient field for transaction fee (not persisted)
    @Transient
    private BigDecimal transactionFee;
}
