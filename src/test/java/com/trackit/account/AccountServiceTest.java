package com.trackit.account;

import com.trackit.auth.service.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createShouldPersistAccountAndReturnResponse() {
        AccountRequest request = new AccountRequest();
        request.setUserId(1L);
        request.setName("Wallet");
        request.setType(AccountType.CASH);
        request.setBalance(new BigDecimal("100.00"));

        Account saved = new Account();
        saved.setId(10L);
        saved.setUserId(1L);
        saved.setName("Wallet");
        saved.setType(AccountType.CASH);
        saved.setBalance(new BigDecimal("100.00"));

        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(accountRepository.save(org.mockito.ArgumentMatchers.any(Account.class))).thenReturn(saved);

        AccountResponse response = accountService.create(request);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Wallet");
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getType()).isEqualTo(AccountType.CASH);
    }

    @Test
    void createDefaultAccountForNewUserShouldPersistCashWallet() {
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.createDefaultAccountForNewUser(42L);

        verify(accountRepository).save(captor.capture());
        Account a = captor.getValue();
        assertThat(a.getUserId()).isEqualTo(42L);
        assertThat(a.getName()).isEqualTo("Cash");
        assertThat(a.getType()).isEqualTo(AccountType.CASH);
        assertThat(a.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
