package com.payment.backend.service;

import com.payment.backend.dto.TransactionResponse;
import com.payment.backend.entity.Account;
import com.payment.backend.entity.Transaction;
import com.payment.backend.entity.User;
import com.payment.backend.exception.InsufficientFundsException;
import com.payment.backend.exception.ResourceNotFoundException;
import com.payment.backend.repository.AccountRepository;
import com.payment.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public Account createAccount(Long userId) {
        User user = new User();
        user.setId(userId);

        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setBalance(BigDecimal.ZERO);
        account.setUser(user);
        return accountRepository.save(account);
    }

    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with number: " + accountNumber));
    }

    public List<Account> getAccountsByUser(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    @Transactional
    public Account deposit(String accountNumber, BigDecimal amount) {
        Account account = getAccountByNumber(accountNumber);
        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account);
    }

    @Transactional
    public TransactionResponse transfer(String senderAccountNumber,
                                        String receiverAccountNumber,
                                        BigDecimal amount) {

        Account sender = getAccountByNumber(senderAccountNumber);
        Account receiver = getAccountByNumber(receiverAccountNumber);

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds: balance is " + sender.getBalance()
                            + ", attempted transfer of " + amount);
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transaction transaction = new Transaction();
        transaction.setSenderAccount(sender);
        transaction.setReceiverAccount(receiver);
        transaction.setAmount(amount);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);

        Transaction saved = transactionRepository.save(transaction);

        return TransactionResponse.from(saved);
    }

    private String generateAccountNumber() {
        return "ACC" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
