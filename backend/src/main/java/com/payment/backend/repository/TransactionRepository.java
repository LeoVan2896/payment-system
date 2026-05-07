package com.payment.backend.repository;

import com.payment.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySenderAccountId(Long accountId);
    List<Transaction> findByReceiverAccountId(Long accountId);
}
