package com.financebook.entity;

import com.financebook.util.Constants;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * A user-defined classification dimension.
 * 
 * Examples of types:
 *   • "Spending Area" (Food, Rent, ...)
 *   • "Payment Method" (Cash, Credit Card, ...)
 *   • "VAT Rate" (19%, 7%, 0%)
 */
@Entity
@Table(name = "categorytype")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = Constants.MAX_CATEGORY_NAME_LENGTH)
    @Column(nullable = false, length = Constants.MAX_CATEGORY_NAME_LENGTH)
    private String name;
    
    @Size(max = Constants.MAX_DESCRIPTION_LENGTH)
    @Column(length = Constants.MAX_DESCRIPTION_LENGTH)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
