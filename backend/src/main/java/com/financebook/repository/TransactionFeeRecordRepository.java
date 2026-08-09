package com.financebook.repository;

import com.financebook.entity.TransactionFeeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TransactionFeeRecord entity operations.
 */
@Repository
public interface TransactionFeeRecordRepository extends JpaRepository<TransactionFeeRecord, Long> {
    
    Optional<TransactionFeeRecord> findByPaymentItemId(Long paymentItemId);
    
    List<TransactionFeeRecord> findByUserId(Long userId);
    
    List<TransactionFeeRecord> findByPaymentItemIdIn(List<Long> paymentItemIds);
    
    void deleteByPaymentItemId(Long paymentItemId);
}
