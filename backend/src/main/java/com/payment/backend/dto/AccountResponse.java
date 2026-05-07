package com.payment.backend.dto;

import com.payment.backend.entity.Account;

public record AccountResponse(
        Long id,
        String accountNumber,
        java.math.BigDecimal balance,
        String status
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getStatus().name()
        );
    }
}
