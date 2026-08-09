package com.financebook.entity;

import com.financebook.util.Constants;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Person or organisation involved in a transaction.
 * 
 * Keeping this in a separate table allows:
 *   • De-duplication of recipient data across many payment items
 *   • Attachment of future metadata (e.g. contact info, logo, IBAN)
 */
@Entity
@Table(name = "recipient")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recipient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = Constants.MAX_RECIPIENT_NAME_LENGTH)
    @Column(nullable = false, length = Constants.MAX_RECIPIENT_NAME_LENGTH)
    private String name;
    
    @Size(max = Constants.MAX_RECIPIENT_ADDRESS_LENGTH)
    @Column(length = Constants.MAX_RECIPIENT_ADDRESS_LENGTH)
    private String address;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
