package com.financebook.entity;

import com.financebook.util.Constants;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Application user with personal details and structured address.
 * 
 * Passwords are stored as BCrypt hashes - never in plaintext.
 * The is_admin flag grants access to the admin management website.
 */
@Entity
@Table(name = "\"user\"")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = Constants.MAX_USERNAME_LENGTH)
    @Column(unique = true, nullable = false, length = Constants.MAX_USERNAME_LENGTH)
    private String username;
    
    @NotBlank
    @Column(nullable = false, name = "hashed_password")
    private String hashedPassword;
    
    // Personal details
    @NotBlank
    @Size(max = Constants.MAX_USER_NAME_LENGTH)
    @Column(nullable = false, length = Constants.MAX_USER_NAME_LENGTH)
    private String surname;
    
    @NotBlank
    @Size(max = Constants.MAX_USER_NAME_LENGTH)
    @Column(nullable = false, length = Constants.MAX_USER_NAME_LENGTH)
    private String prename;
    
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    @Size(max = Constants.MAX_USER_PHONE_LENGTH)
    @Column(length = Constants.MAX_USER_PHONE_LENGTH)
    private String phone;
    
    // Structured address
    @Size(max = Constants.MAX_USER_ROAD_LENGTH)
    @Column(length = Constants.MAX_USER_ROAD_LENGTH)
    private String road;
    
    @Size(max = Constants.MAX_USER_HOUSE_NUMBER_LENGTH)
    @Column(name = "house_number", length = Constants.MAX_USER_HOUSE_NUMBER_LENGTH)
    private String houseNumber;
    
    @Size(max = Constants.MAX_USER_REGION_LENGTH)
    @Column(length = Constants.MAX_USER_REGION_LENGTH)
    private String region;
    
    @Size(max = Constants.MAX_USER_POSTAL_LENGTH)
    @Column(length = Constants.MAX_USER_POSTAL_LENGTH)
    private String postal;
    
    @Size(max = Constants.MAX_USER_CITY_LENGTH)
    @Column(length = Constants.MAX_USER_CITY_LENGTH)
    private String city;
    
    @Size(max = Constants.MAX_USER_STATE_LENGTH)
    @Column(length = Constants.MAX_USER_STATE_LENGTH)
    private String state;
    
    // Role & status
    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin = false;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
