package com.financebook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Fee configuration for a user.
 * 
 * Each user has at most one active fee plan. The plan can be defined in two modes:
 *   • "table" - an amount table with per-interval chart-based fees
 *   • "formula" - a single mathematical expression f(x, y)
 * 
 * When mode is "table", amountTableJson holds the sorted list of lower-limit amounts
 * (e.g. [0, 100, 500]), and intervalDataJson holds a dict keyed by interval start value
 * containing maxFee, clicked points, and regression coefficients.
 */
@Entity
@Table(name = "transactionfeeplan")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFeePlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String mode = "table";  // "table" or "formula"
    
    @Column(name = "formula_text", columnDefinition = "TEXT")
    private String formulaText;
    
    @Column(name = "amount_table_json", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String amountTableJson = "[0]";
    
    @Column(name = "interval_data_json", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String intervalDataJson = "{}";
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
