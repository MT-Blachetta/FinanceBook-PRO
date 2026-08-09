package com.financebook.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File upload configuration.
 * Creates necessary directories on application startup.
 */
@Slf4j
@Configuration
public class FileUploadConfig {
    
    @Value("${file.upload.icons-dir:./icons}")
    private String iconsDir;
    
    @Value("${file.upload.invoices-dir:./app/invoices}")
    private String invoicesDir;
    
    @PostConstruct
    public void init() throws IOException {
        // Create icons directory
        Path iconsPath = Paths.get(iconsDir);
        if (!Files.exists(iconsPath)) {
            Files.createDirectories(iconsPath);
            log.info("Created icons directory: {}", iconsPath.toAbsolutePath());
        }
        
        // Create invoices directory
        Path invoicesPath = Paths.get(invoicesDir);
        if (!Files.exists(invoicesPath)) {
            Files.createDirectories(invoicesPath);
            log.info("Created invoices directory: {}", invoicesPath.toAbsolutePath());
        }
    }
}
