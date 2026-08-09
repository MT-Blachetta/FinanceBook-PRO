package com.financebook.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * User read response DTO.
 * Mirrors Python's UserRead schema.
 * Never exposes password hash.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReadResponse {
    
    private Long id;
    private String username;
    private String surname;
    private String prename;
    private LocalDate birthDate;
    private String phone;
    private String road;
    private String houseNumber;
    private String region;
    private String postal;
    private String city;
    private String state;
    private Boolean isAdmin;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
