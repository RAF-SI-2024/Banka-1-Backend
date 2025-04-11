package com.banka1.banking.service;

import com.banka1.banking.aspect.*;
import com.banka1.banking.dto.*;
import com.banka1.banking.dto.request.CreateAccountDTO;
import com.banka1.banking.models.*;
import com.banka1.banking.services.*;
import com.banka1.banking.services.implementation.AuthService;
import com.banka1.common.model.Position;
import io.jsonwebtoken.Claims;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Base64;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTests {
    @Mock
    private AuthService authService;
    @Mock
    private AccountService accountService;
    @Mock
    private LoanService loanService;
    @Mock
    private CardService cardService;
    @Mock
    private ReceiverService receiverService;
    @Mock
    private TransferService transferService;
    @InjectMocks
    private AuthAspect authAspect;
    @Mock
    private Claims claims;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private MethodSignature methodSignature;
    @Mock
    private Method method;
    @Mock
    private AccountAuthorization accountAuthorization;
    @Mock
    private LoanAuthorization loanAuthorization;
    @Mock
    private CardAuthorization cardAuthorization;
    @Mock
    private ReceiverAuthorization receiverAuthorization;
    @Mock
    private Account account;
    @Mock
    private Receiver receiver;
    @Mock
    private Card card;
    @Mock
    private Loan loan;
    @Mock
    private Transfer transfer;
    @Mock
    private CreateAccountDTO createAccountDTO;
    @Mock
    private ExchangeMoneyTransferDTO exchangeMoneyTransferDTO;
    @Mock
    private OtpTokenDTO otpTokenDTO;
    @Mock
    private ReceiverDTO receiverDTO;
    @Mock
    private InternalTransferDTO internalTransferDTO;
    @Mock
    private MoneyTransferDTO moneyTransferDTO;
    @Mock
    private CreateCardDTO createCardDTO;


    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(authService, "secret", Base64.getEncoder().encodeToString("TEST_SECRET_12345678_ABC!_00defgh".getBytes()));
        ReflectionTestUtils.setField(authService, "jwtExpiration", 100000);
    }

    @Test
    void authorizationTest_invalidToken() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { "Bearer NevalidanToken" });

        authAspect.authorizeAccountAction(joinPoint);
        authAspect.authorizeCardAction(joinPoint);
        authAspect.authorizeLoanAction(joinPoint);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void authorizationTest_blankToken() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { null });

        authAspect.authorizeAccountAction(joinPoint);
        authAspect.authorizeCardAction(joinPoint);
        authAspect.authorizeLoanAction(joinPoint);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void authorizationTest_validTokenNoAccountAccess() throws Throwable {
        when(claims.get("id", Long.class)).thenReturn(1L);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[] { "Bearer ValidanToken", 1L });

        when(authService.parseToken(notNull(String.class))).thenReturn(claims);

        when(accountService.findById(1L)).thenReturn(account);
        //when(loanService.findById("1")).thenReturn(loan);
        when(cardService.findById(1L)).thenReturn(card);
        when(receiverService.findById(1L)).thenReturn(receiver);

        when(account.getOwnerID()).thenReturn(2L);
        when(receiver.getCustomerId()).thenReturn(3L);
        //when(loan.getAccount()).thenReturn(account);
        when(card.getAccount()).thenReturn(account);

        when(accountAuthorization.customerOnlyOperation()).thenReturn(true);
        when(cardAuthorization.customerOnlyOperation()).thenReturn(true);
        //when(loanAuthorization.customerOnlyOperation()).thenReturn(true);
        when(receiverAuthorization.customerOnlyOperation()).thenReturn(true);
        when(accountAuthorization.disallowAdminFallback()).thenReturn(true);
        when(cardAuthorization.disallowAdminFallback()).thenReturn(true);
        //when(loanAuthorization.disallowAdminFallback()).thenReturn(true);
        when(receiverAuthorization.disallowAdminFallback()).thenReturn(true);

        when(method.getAnnotation(AccountAuthorization.class)).thenReturn(accountAuthorization);
        when(method.getAnnotation(CardAuthorization.class)).thenReturn(cardAuthorization);
        //when(method.getAnnotation(LoanAuthorization.class)).thenReturn(loanAuthorization);
        when(method.getAnnotation(ReceiverAuthorization.class)).thenReturn(receiverAuthorization);

        when(methodSignature.getParameterTypes()).thenReturn(new Class[] { String.class, Long.class });

        when(methodSignature.getMethod()).thenReturn(method);
        when(authService.getToken(notNull(String.class))).thenReturn("ValidanToken");

        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "accountId" });
        authAspect.authorizeAccountAction(joinPoint);

        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "cardId" });
        authAspect.authorizeCardAction(joinPoint);

        //when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "loanId" });
        //authAspect.authorizeLoanAction(joinPoint);

        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "receiverId" });
        authAspect.authorizeReceiverAction(joinPoint);

        verify(joinPoint, never()).proceed();
    }

    @Test
    void authorizationTest_validTokenEmployeeAccess() throws Throwable {
        when(claims.get("isEmployed", Boolean.class)).thenReturn(true);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[] { "Bearer ValidanToken", 1L });

        when(authService.parseToken(notNull(String.class))).thenReturn(claims);

        when(accountAuthorization.customerOnlyOperation()).thenReturn(false);
        when(cardAuthorization.customerOnlyOperation()).thenReturn(false);
        //when(loanAuthorization.customerOnlyOperation()).thenReturn(false);
        when(receiverAuthorization.customerOnlyOperation()).thenReturn(false);

        when(method.getAnnotation(AccountAuthorization.class)).thenReturn(accountAuthorization);
        when(method.getAnnotation(CardAuthorization.class)).thenReturn(cardAuthorization);
        //when(method.getAnnotation(LoanAuthorization.class)).thenReturn(loanAuthorization);
        when(method.getAnnotation(ReceiverAuthorization.class)).thenReturn(receiverAuthorization);

        when(methodSignature.getMethod()).thenReturn(method);
        when(authService.getToken(notNull(String.class))).thenReturn("ValidanToken");

        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "accountId" });
        authAspect.authorizeAccountAction(joinPoint);

        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "cardId" });
        authAspect.authorizeCardAction(joinPoint);

        //when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "loanId" });
        //authAspect.authorizeLoanAction(joinPoint);

        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "receiverId" });
        authAspect.authorizeReceiverAction(joinPoint);

        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void authorizationTest_validTokenAdminAccess() throws Throwable {
        when(claims.get("id", Long.class)).thenReturn(1L);
        when(claims.get("isAdmin", Boolean.class)).thenReturn(true);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[] { "Bearer ValidanToken", 1L });

        when(authService.parseToken(notNull(String.class))).thenReturn(claims);

        when(accountService.findById(1L)).thenReturn(account);
        //when(loanService.findById("1")).thenReturn(loan);
        when(cardService.findById(1L)).thenReturn(card);
        when(receiverService.findById(1L)).thenReturn(receiver);

        when(account.getOwnerID()).thenReturn(2L);
        //when(loan.getAccount()).thenReturn(account);
        when(card.getAccount()).thenReturn(account);
        when(receiver.getCustomerId()).thenReturn(3L);

        when(accountAuthorization.customerOnlyOperation()).thenReturn(true);
        when(cardAuthorization.customerOnlyOperation()).thenReturn(true);
        //when(loanAuthorization.customerOnlyOperation()).thenReturn(true);
        when(receiverAuthorization.customerOnlyOperation()).thenReturn(true);
        when(accountAuthorization.disallowAdminFallback()).thenReturn(false);
        when(cardAuthorization.disallowAdminFallback()).thenReturn(false);
        //when(loanAuthorization.disallowAdminFallback()).thenReturn(false);
        when(receiverAuthorization.disallowAdminFallback()).thenReturn(false);

        when(method.getAnnotation(AccountAuthorization.class)).thenReturn(accountAuthorization);
        when(method.getAnnotation(CardAuthorization.class)).thenReturn(cardAuthorization);
        //when(method.getAnnotation(LoanAuthorization.class)).thenReturn(loanAuthorization);
        when(method.getAnnotation(ReceiverAuthorization.class)).thenReturn(receiverAuthorization);

        when(methodSignature.getParameterTypes()).thenReturn(new Class[] { String.class, Long.class });

        when(methodSignature.getMethod()).thenReturn(method);
        when(authService.getToken(notNull(String.class))).thenReturn("ValidanToken");

        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "accountId" });
        authAspect.authorizeAccountAction(joinPoint);

        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "cardId" });
        authAspect.authorizeCardAction(joinPoint);

        //when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "loanId" });
        //authAspect.authorizeLoanAction(joinPoint);

        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "receiverId" });
        authAspect.authorizeReceiverAction(joinPoint);

        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void authorizationTest_validTokenAccountAccess() throws Throwable {
        when(claims.get("id", Long.class)).thenReturn(1L);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[] { "Bearer ValidanToken", 1L });

        when(authService.parseToken(notNull(String.class))).thenReturn(claims);

        when(accountService.findById(1L)).thenReturn(account);
        //when(loanService.findById("1")).thenReturn(loan);
        when(cardService.findById(1L)).thenReturn(card);
        when(receiverService.findById(1L)).thenReturn(receiver);

        when(account.getOwnerID()).thenReturn(1L);
        //when(loan.getAccount()).thenReturn(account);
        when(card.getAccount()).thenReturn(account);
        when(receiver.getCustomerId()).thenReturn(1L);

        when(accountAuthorization.customerOnlyOperation()).thenReturn(true);
        when(cardAuthorization.customerOnlyOperation()).thenReturn(true);
        //when(loanAuthorization.customerOnlyOperation()).thenReturn(true);
        when(receiverAuthorization.customerOnlyOperation()).thenReturn(true);

        when(method.getAnnotation(AccountAuthorization.class)).thenReturn(accountAuthorization);
        when(method.getAnnotation(CardAuthorization.class)).thenReturn(cardAuthorization);
        //when(method.getAnnotation(LoanAuthorization.class)).thenReturn(loanAuthorization);
        when(method.getAnnotation(ReceiverAuthorization.class)).thenReturn(receiverAuthorization);

        when(methodSignature.getParameterTypes()).thenReturn(new Class[] { String.class, Long.class });

        when(methodSignature.getMethod()).thenReturn(method);
        when(authService.getToken(notNull(String.class))).thenReturn("ValidanToken");

        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "accountId" });
        authAspect.authorizeAccountAction(joinPoint);

        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "cardId" });
        authAspect.authorizeCardAction(joinPoint);

        //when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "loanId" });
        //authAspect.authorizeLoanAction(joinPoint);

        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "receiverId" });
        authAspect.authorizeReceiverAction(joinPoint);

        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void authorizationTest_validTokenMatchingId() throws Throwable {
        when(claims.get("id", Long.class)).thenReturn(1L);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "userId" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { "Bearer ValidanToken", 1L });

        when(authService.parseToken(notNull(String.class))).thenReturn(claims);

        when(accountAuthorization.customerOnlyOperation()).thenReturn(true);

        when(method.getAnnotation(AccountAuthorization.class)).thenReturn(accountAuthorization);

        when(methodSignature.getMethod()).thenReturn(method);
        when(authService.getToken(notNull(String.class))).thenReturn("ValidanToken");

        authAspect.authorizeAccountAction(joinPoint);
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void authorizationTest_validTokenUsingDTO() throws Throwable {
        when(claims.get("id", Long.class)).thenReturn(1L);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[] {
                "Bearer ValidanToken",
                createAccountDTO,
                exchangeMoneyTransferDTO,
                otpTokenDTO,
                receiverDTO,
                internalTransferDTO,
                moneyTransferDTO,
                createCardDTO,
        });

        when(createAccountDTO.getOwnerID()).thenReturn(1L);
        when(exchangeMoneyTransferDTO.getAccountFrom()).thenReturn(1L);
        when(otpTokenDTO.getTransferId()).thenReturn(1L);
        when(receiverDTO.getCustomerId()).thenReturn(1L);
        when(internalTransferDTO.getFromAccountId()).thenReturn(1L);
        when(moneyTransferDTO.getFromAccountNumber()).thenReturn("1");
        when(createCardDTO.getAccountID()).thenReturn(1L);

        when(authService.parseToken(notNull(String.class))).thenReturn(claims);

        when(accountService.findById(1L)).thenReturn(account);
        when(accountService.findByAccountNumber("1")).thenReturn(account);
        //when(loanService.findById("1")).thenReturn(loan);
        when(transferService.findById(1L)).thenReturn(transfer);

        when(account.getOwnerID()).thenReturn(1L);
        //when(loan.getAccount()).thenReturn(account);
        when(transfer.getFromAccountId()).thenReturn(account);

        when(accountAuthorization.customerOnlyOperation()).thenReturn(true);
        when(cardAuthorization.customerOnlyOperation()).thenReturn(true);
        //when(loanAuthorization.customerOnlyOperation()).thenReturn(true);
        when(receiverAuthorization.customerOnlyOperation()).thenReturn(true);

        when(method.getAnnotation(AccountAuthorization.class)).thenReturn(accountAuthorization);
        when(method.getAnnotation(CardAuthorization.class)).thenReturn(cardAuthorization);
        //when(method.getAnnotation(LoanAuthorization.class)).thenReturn(loanAuthorization);
        when(method.getAnnotation(ReceiverAuthorization.class)).thenReturn(receiverAuthorization);

        when(methodSignature.getParameterTypes()).thenReturn(new Class[] {
                String.class,
                CreateAccountDTO.class,
                ExchangeMoneyTransferDTO.class,
                OtpTokenDTO.class,
                ReceiverDTO.class,
                InternalTransferDTO.class,
                MoneyTransferDTO.class,
                CreateCardDTO.class
        });

        when(methodSignature.getMethod()).thenReturn(method);
        when(authService.getToken(notNull(String.class))).thenReturn("ValidanToken");

        when(methodSignature.getParameterNames()).thenReturn(new String[] {
                "authorization",
                "createAccountDto",
                "exchangeMoneyTransferDto",
                "otpTokenDto",
                "receiverDto",
                "internalTransferDto",
                "moneyTransferDto",
                "createCardDto",
        });

        authAspect.authorizeAccountAction(joinPoint);
        authAspect.authorizeCardAction(joinPoint);
        //authAspect.authorizeLoanAction(joinPoint);
        authAspect.authorizeReceiverAction(joinPoint);

        verify(joinPoint, times(3)).proceed();
    }
}
