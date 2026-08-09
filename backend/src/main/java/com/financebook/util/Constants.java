package com.financebook.util;

/**
 * Application-wide constants for validation and data handling.
 * Mirrors the Python constants.py file.
 */
public final class Constants {
    
    // Prevent instantiation
    private Constants() {
        throw new AssertionError("Constants class should not be instantiated");
    }
    
    // Maximum lengths for text fields
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_RECIPIENT_NAME_LENGTH = 255;
    public static final int MAX_RECIPIENT_ADDRESS_LENGTH = 500;
    public static final int MAX_CATEGORY_NAME_LENGTH = 255;
    
    // User-related field length limits
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MAX_USER_NAME_LENGTH = 100;
    public static final int MAX_USER_PHONE_LENGTH = 30;
    public static final int MAX_USER_ROAD_LENGTH = 200;
    public static final int MAX_USER_HOUSE_NUMBER_LENGTH = 20;
    public static final int MAX_USER_REGION_LENGTH = 100;
    public static final int MAX_USER_POSTAL_LENGTH = 20;
    public static final int MAX_USER_CITY_LENGTH = 100;
    public static final int MAX_USER_STATE_LENGTH = 100;
    
    // Password constraints
    public static final int MIN_PASSWORD_LENGTH = 6;
    
    // File upload constraints
    public static final long MAX_INVOICE_SIZE = 25 * 1024 * 1024; // 25MB
    
    // Fee constraints
    public static final double MIN_FEE_THRESHOLD = 0.01; // Minimum fee to apply

    // Payment amount constraints (DB column is NUMERIC(10,2))
    public static final java.math.BigDecimal MAX_PAYMENT_AMOUNT = new java.math.BigDecimal("99999999.99");

    // Well-known default names created for every user
    public static final String STANDARD_CATEGORY_TYPE_NAME = "standard";
    public static final String UNCLASSIFIED_CATEGORY_NAME = "UNCLASSIFIED";
}
