package com.financebook.dto.request;

import com.financebook.util.Constants;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * User update request DTO.
 * Mirrors Python's UserUpdate schema.
 * All fields optional for partial updates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    
    @Size(max = Constants.MAX_USER_NAME_LENGTH)
    private String surname;
    
    @Size(max = Constants.MAX_USER_NAME_LENGTH)
    private String prename;
    
    private LocalDate birthDate;
    
    @Size(max = Constants.MAX_USER_PHONE_LENGTH)
    private String phone;
    
    @Size(max = Constants.MAX_USER_ROAD_LENGTH)
    private String road;
    
    @Size(max = Constants.MAX_USER_HOUSE_NUMBER_LENGTH)
    private String houseNumber;
    
    @Size(max = Constants.MAX_USER_REGION_LENGTH)
    private String region;
    
    @Size(max = Constants.MAX_USER_POSTAL_LENGTH)
    private String postal;
    
    @Size(max = Constants.MAX_USER_CITY_LENGTH)
    private String city;
    
    @Size(max = Constants.MAX_USER_STATE_LENGTH)
    private String state;
    
    @Size(min = Constants.MIN_PASSWORD_LENGTH)
    private String password; // Will be hashed before storage
}
