package com.financebook.util;

/**
 * Utility class for name normalization.
 * Mirrors Python's _normalize_name() function.
 */
public final class NameNormalizer {
    
    // Prevent instantiation
    private NameNormalizer() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Normalize user-provided names by collapsing whitespace.
     * 
     * First strips leading/trailing whitespace, then collapses internal runs
     * of whitespace characters (spaces, tabs, newlines) into single spaces.
     * 
     * @param rawName the raw input name
     * @return normalized name with collapsed whitespace
     */
    public static String normalize(String rawName) {
        if (rawName == null) {
            return "";
        }
        
        // Trim leading/trailing whitespace, then collapse internal whitespace
        return rawName.trim().replaceAll("\\s+", " ");
    }
}
