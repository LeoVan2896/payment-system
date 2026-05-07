package com.payment.backend.dto;

import com.payment.backend.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String senderAccountNumber,
        String receiverAccountNumber,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getSenderAccount().getAccountNumber(),
                transaction.getReceiverAccount().getAccountNumber(),
                transaction.getAmount(),
                transaction.getStatus().name(),
                transaction.getCreatedAt()
        );
    }
}
