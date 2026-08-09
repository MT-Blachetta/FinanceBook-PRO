package com.financebook.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * JWT authentication response.
 * Returns access token after successful login.
 */
@Data
@AllArgsConstructor
public class JwtResponse {
    
    private String accessToken;
    private String tokenType = "bearer";
    
    public JwtResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
