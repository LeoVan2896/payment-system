package com.payment.backend.service;

import com.payment.backend.entity.Account;
import com.payment.backend.entity.Transaction;
import com.payment.backend.repository.AccountRepository;
import com.payment.backend.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account senderAccount;
    private Account receiverAccount;

    @BeforeEach
    void setUp() {
        senderAccount = new Account();
        senderAccount.setId(1L);
        senderAccount.setAccountNumber("ACC-SENDER-001");
        senderAccount.setBalance(new BigDecimal("500.00"));
        senderAccount.setStatus(Account.AccountStatus.ACTIVE);

        receiverAccount = new Account();
        receiverAccount.setId(2L);
        receiverAccount.setAccountNumber("ACC-RECEIVER-001");
        receiverAccount.setBalance(new BigDecimal("100.00"));
        receiverAccount.setStatus(Account.AccountStatus.ACTIVE);
    }

    // ---------- createAccount ----------

    @Test
    void createAccount_givenValidUserId_savesAndReturnsAccount() {
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.createAccount(42L);

        assertThat(result).isNotNull();
        assertThat(result.getAccountNumber()).startsWith("ACC");
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_givenUserId_setsUserIdOnAccount() {
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.createAccount(99L);

        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(99L);
    }

    // ---------- getAccountByNumber ----------

    @Test
    void getAccountByNumber_givenValidNumber_returnsAccount() {
        when(accountRepository.findByAccountNumber("ACC-SENDER-001"))
                .thenReturn(Optional.of(senderAccount));

        Account result = accountService.getAccountByNumber("ACC-SENDER-001");

        assertThat(result.getAccountNumber()).isEqualTo("ACC-SENDER-001");
    }

    @Test
    void getAccountByNumber_givenInvalidNumber_throwsRuntimeException() {
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountByNumber("INVALID"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
    }

    // ---------- getAccountsByUser ----------

    @Test
    void getAccountsByUser_givenUserId_returnsAccountList() {
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(senderAccount));

        List<Account> results = accountService.getAccountsByUser(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAccountNumber()).isEqualTo("ACC-SENDER-001");
    }

    @Test
    void getAccountsByUser_givenUserWithNoAccounts_returnsEmptyList() {
        when(accountRepository.findByUserId(999L)).thenReturn(List.of());

        List<Account> results = accountService.getAccountsByUser(999L);

        assertThat(results).isEmpty();
    }

    // ---------- deposit ----------

    @Test
    void deposit_givenValidAmountAndAccount_increasesBalance() {
        when(accountRepository.findByAccountNumber("ACC-SENDER-001"))
                .thenReturn(Optional.of(senderAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.deposit("ACC-SENDER-001", new BigDecimal("200.00"));

        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
    }

    @Test
    void deposit_givenZeroAmount_balanceUnchanged() {
        when(accountRepository.findByAccountNumber("ACC-SENDER-001"))
                .thenReturn(Optional.of(senderAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.deposit("ACC-SENDER-001", BigDecimal.ZERO);

        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void deposit_givenInvalidAccountNumber_throwsRuntimeException() {
        when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.deposit("INVALID", new BigDecimal("100.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
    }

    // ---------- transfer ----------

    @Test
    void transfer_givenSufficientFunds_debitsSenderCreditReceiver() {
        when(accountRepository.findByAccountNumber("ACC-SENDER-001"))
                .thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumber("ACC-RECEIVER-001"))
                .thenReturn(Optional.of(receiverAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction tx = accountService.transfer("ACC-SENDER-001", "ACC-RECEIVER-001", new BigDecimal("200.00"));

        assertThat(senderAccount.getBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(receiverAccount.getBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(tx.getStatus()).isEqualTo(Transaction.TransactionStatus.COMPLETED);
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void transfer_givenInsufficientFunds_throwsRuntimeException() {
        when(accountRepository.findByAccountNumber("ACC-SENDER-001"))
                .thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumber("ACC-RECEIVER-001"))
                .thenReturn(Optional.of(receiverAccount));

        assertThatThrownBy(() ->
                accountService.transfer("ACC-SENDER-001", "ACC-RECEIVER-001", new BigDecimal("9999.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    void transfer_givenExactBalance_succeedsWithZeroRemainingBalance() {
        when(accountRepository.findByAccountNumber("ACC-SENDER-001"))
                .thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumber("ACC-RECEIVER-001"))
                .thenReturn(Optional.of(receiverAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.transfer("ACC-SENDER-001", "ACC-RECEIVER-001", new BigDecimal("500.00"));

        assertThat(senderAccount.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void transfer_givenInvalidSenderAccount_throwsRuntimeException() {
        when(accountRepository.findByAccountNumber("BAD-SENDER"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                accountService.transfer("BAD-SENDER", "ACC-RECEIVER-001", new BigDecimal("100.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void transfer_givenInvalidReceiverAccount_throwsRuntimeException() {
        when(accountRepository.findByAccountNumber("ACC-SENDER-001"))
                .thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumber("BAD-RECEIVER"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                accountService.transfer("ACC-SENDER-001", "BAD-RECEIVER", new BigDecimal("100.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void transfer_givenValidTransfer_savesBothAccounts() {
        when(accountRepository.findByAccountNumber("ACC-SENDER-001"))
                .thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumber("ACC-RECEIVER-001"))
                .thenReturn(Optional.of(receiverAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.transfer("ACC-SENDER-001", "ACC-RECEIVER-001", new BigDecimal("100.00"));

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }
}
