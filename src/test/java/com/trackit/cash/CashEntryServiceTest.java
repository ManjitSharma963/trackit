package com.trackit.cash;

import com.trackit.account.Account;
import com.trackit.account.AccountRepository;
import com.trackit.auth.service.CurrentUserProvider;
import com.trackit.budget.service.BudgetService;
import com.trackit.cash.dto.CashEntryRequest;
import com.trackit.cash.model.CashDirection;
import com.trackit.cash.model.CashEntry;
import com.trackit.cash.repository.CashEntryRepository;
import com.trackit.cash.service.CashEntryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashEntryServiceTest {

    @Mock
    private CashEntryRepository cashEntryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private CashEntryService cashEntryService;

    @Test
    void createFillsMissingAccountIdFromNewestOwnedAccount() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(7L);

        Account wallet = new Account();
        wallet.setId(99L);
        wallet.setUserId(7L);
        wallet.setBalance(new BigDecimal("100.00"));
        when(accountRepository.findByUserIdOrderByIdDesc(7L)).thenReturn(List.of(wallet));
        when(accountRepository.findById(99L)).thenReturn(java.util.Optional.of(wallet));

        CashEntry saved = new CashEntry();
        saved.setId(1L);
        saved.setUserId(7L);
        saved.setAccountId(99L);
        saved.setDirection(CashDirection.EXPENSE);
        saved.setAmount(new BigDecimal("5.00"));
        saved.setEntryDate(LocalDate.of(2026, 4, 26));
        when(cashEntryRepository.save(any(CashEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        CashEntryRequest request = new CashEntryRequest();
        request.setDirection(CashDirection.EXPENSE);
        request.setAmount(new BigDecimal("5.00"));
        request.setCategory("Snacks");
        request.setEntryDate(LocalDate.of(2026, 4, 26));
        request.setRecurring(false);

        cashEntryService.create(request);

        ArgumentCaptor<CashEntry> captor = ArgumentCaptor.forClass(CashEntry.class);
        verify(cashEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getAccountId()).isEqualTo(99L);
    }
}
