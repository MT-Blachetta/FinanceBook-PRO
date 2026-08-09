package com.financebook.config;

import com.financebook.entity.Category;
import com.financebook.entity.CategoryType;
import com.financebook.entity.User;
import com.financebook.repository.CategoryRepository;
import com.financebook.repository.CategoryTypeRepository;
import com.financebook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Database initializer.
 * Creates default admin user, standard category type, and UNCLASSIFIED category.
 * Mirrors Python's initialize_default_data() function.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final CategoryTypeRepository categoryTypeRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${admin.default-password:admin}")
    private String adminDefaultPassword;
    
    @Override
    public void run(String... args) {
        initializeDefaultData();
    }
    
    private void initializeDefaultData() {
        // Create or update default admin user
        User adminUser = userRepository.findByUsername("admin").orElse(null);
        
        if (adminUser == null) {
            log.info("Creating default admin user");
            adminUser = User.builder()
                    .username("admin")
                    .hashedPassword(passwordEncoder.encode(adminDefaultPassword))
                    .surname("Administrator")
                    .prename("System")
                    .isAdmin(true)
                    .isActive(true)
                    .build();
            adminUser = userRepository.save(adminUser);
            log.info("Admin user created successfully with password from configuration");
        }
        // The password of an existing admin user is intentionally left untouched.
        // Re-encoding the configured default and comparing with equals() is always
        // false (bcrypt salts), which used to silently reset a changed admin
        // password back to the configured default on every application start.
        
        // Create standard category type if not exists
        CategoryType standardType = categoryTypeRepository.findByUserIdAndName(adminUser.getId(), "standard")
                .orElse(null);
        
        if (standardType == null) {
            log.info("Creating standard category type");
            standardType = CategoryType.builder()
                    .name("standard")
                    .description("Default category type for basic expense/income classification")
                    .user(adminUser)
                    .build();
            standardType = categoryTypeRepository.save(standardType);
            log.info("Standard category type created successfully");
        }
        
        // Create UNCLASSIFIED category if not exists
        Category unclassified = categoryRepository.findByUserIdAndName(adminUser.getId(), "UNCLASSIFIED")
                .orElse(null);
        
        if (unclassified == null) {
            log.info("Creating UNCLASSIFIED category");
            unclassified = Category.builder()
                    .name("UNCLASSIFIED")
                    .type(standardType)
                    .parent(null)
                    .user(adminUser)
                    .build();
            categoryRepository.save(unclassified);
            log.info("UNCLASSIFIED category created successfully");
        }
        
        log.info("Database initialization complete");
    }
}
