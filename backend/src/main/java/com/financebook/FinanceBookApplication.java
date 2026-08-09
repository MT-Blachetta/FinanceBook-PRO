package com.financebook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for FinanceBook.
 * 
 * A multi-user web application for managing private finances and cash flows.
 * Features JWT authentication, transaction fee engine, hierarchical categories,
 * and a server-side rendered admin panel.
 */
@SpringBootApplication
@EnableJpaAuditing
public class FinanceBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceBookApplication.class, args);
    }
}
