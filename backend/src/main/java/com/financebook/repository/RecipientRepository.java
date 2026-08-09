package com.financebook.repository;

import com.financebook.entity.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Recipient entity operations.
 */
@Repository
public interface RecipientRepository extends JpaRepository<Recipient, Long> {
    
    List<Recipient> findByUserId(Long userId);
    
    Optional<Recipient> findByUserIdAndName(Long userId, String name);
    
    Optional<Recipient> findByIdAndUserId(Long id, Long userId);
}
