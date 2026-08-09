package com.financebook.dto.request;

import com.financebook.util.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * User creation request DTO.
 * Mirrors Python's UserCreate schema.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    
    @NotBlank(message = "Username is required")
    @Size(max = Constants.MAX_USERNAME_LENGTH, message = "Username exceeds maximum length")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = Constants.MIN_PASSWORD_LENGTH, message = "Password must be at least 6 characters")
    private String password;
    
    @NotBlank(message = "Surname is required")
    @Size(max = Constants.MAX_USER_NAME_LENGTH, message = "Surname exceeds maximum length")
    private String surname;
    
    @NotBlank(message = "Prename is required")
    @Size(max = Constants.MAX_USER_NAME_LENGTH, message = "Prename exceeds maximum length")
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
}
