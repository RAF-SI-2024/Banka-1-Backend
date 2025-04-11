package com.banka1.banking.aspect;

import com.banka1.banking.dto.*;
import com.banka1.banking.dto.request.CreateAccountDTO;
import com.banka1.banking.dto.request.CreateLoanDTO;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Card;
import com.banka1.banking.models.Loan;
import com.banka1.banking.models.Receiver;
import com.banka1.banking.services.*;
import com.banka1.banking.services.implementation.AuthService;
import com.banka1.banking.utils.ResponseMessage;
import com.banka1.banking.utils.ResponseTemplate;
import com.banka1.banking.utils.ThrowingFunction;
import com.banka1.common.model.Position;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.IntStream;

@Aspect
@Configuration
@EnableAspectJAutoProxy
public class AuthAspect {
    private final AuthService authService;
    private final AccountService accountService;
    private final LoanService loanService;
    private final CardService cardService;
    private final TransferService transferService;
    private final ReceiverService receiverService;

    public AuthAspect(AuthService authService, AccountService accountService, LoanService loanService, CardService cardService, TransferService transferService, ReceiverService receiverService) {
        this.authService = authService;
        this.accountService = accountService;
        this.loanService = loanService;
        this.cardService = cardService;
        this.transferService = transferService;
        this.receiverService = receiverService;
    }

    // Izvlači token iz prosledjenog autorizacionog parametra
    private String parseAuthToken(String[] parameterNames, Object[] args) {
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals("authorization") && args[i] != null) {
                return authService.getToken(args[i].toString());
            }
        }
        return null;
    }

    private String getAuthTokenFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");
            return authService.getToken(authHeader);
        }
        return null;
    }

    private Object doAuth(MethodSignature methodSignature, ProceedingJoinPoint joinPoint, ThrowingFunction<Claims, Object> callback) throws Throwable {
        String token = parseAuthToken(methodSignature.getParameterNames(), joinPoint.getArgs());
        if(token == null)
            token = getAuthTokenFromRequest();

        if(token == null) {
            // Token ne postoji, tj. korisnik nije ulogovan
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.UNAUTHORIZED), false, null, ResponseMessage.INVALID_LOGIN.toString());
        }

        // Token je blacklist-ovan
        // Trenutno nemoguće proveriti

        Claims claims = authService.parseToken(token);
        if(claims == null) {
            // Token postoji ali nije autentičan, verovatno je pokušaj neodobrenog pristupa
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.UNAUTHORIZED), false, null, ResponseMessage.INVALID_LOGIN.toString());
        }

        return callback.apply(claims);
    }

    private boolean accountDataOk(MethodSignature methodSignature, JoinPoint joinPoint, Long userId) {
        OptionalInt maybeAccountIdIndex = IntStream.range(0, methodSignature.getParameterNames().length).filter(i -> methodSignature.getParameterNames()[i].compareTo("accountId") == 0).findFirst();
        if (maybeAccountIdIndex.isPresent()) {
            Account account = accountService.findById(Long.valueOf(joinPoint.getArgs()[maybeAccountIdIndex.getAsInt()].toString()));
            if (account != null) {
                return Objects.equals(userId, account.getOwnerID());
            }
        } else {
            Class<?>[] types = methodSignature.getParameterTypes();
            Object[] values = joinPoint.getArgs();

            boolean encounteredDto = false;

            for (int i = 0; i < types.length; i++) {
                if(types[i] == CreateAccountDTO.class) {
                    if(!Objects.equals(((CreateAccountDTO) values[i]).getOwnerID(), userId))
                        return false;
                    encounteredDto = true;
                } else if(types[i] == ExchangeMoneyTransferDTO.class) {
                    if(!Objects.equals(accountService.findById(((ExchangeMoneyTransferDTO) values[i]).getAccountFrom()).getOwnerID(), userId))
                        return false;
                    encounteredDto = true;
                } else if(types[i] == OtpTokenDTO.class) {
                    if(!Objects.equals(transferService.findById(((OtpTokenDTO) values[i]).getTransferId()).getFromAccountId().getOwnerID(), userId))
                        return false;
                    encounteredDto = true;
                } else if(types[i] == ReceiverDTO.class) {
//                   // if(!Objects.equals(accountService.findById(((ReceiverDTO) values[i]).getCustomerId()), userId))
                    Long customerId = ((ReceiverDTO) values[i]).getCustomerId();
                    if (!Objects.equals(customerId, userId))
                        return false;
                    encounteredDto = true;
                } else if(types[i] == InternalTransferDTO.class) {
                    if(!Objects.equals(accountService.findById(((InternalTransferDTO) values[i]).getFromAccountId()).getOwnerID(), userId))
                        return false;
                    encounteredDto = true;
                } else if(types[i] == MoneyTransferDTO.class) {
                    if(!Objects.equals(accountService.findByAccountNumber(((MoneyTransferDTO) values[i]).getFromAccountNumber()).getOwnerID(), userId))
                        return false;
                    encounteredDto = true;
                } else if(types[i] == CreateCardDTO.class) {
                    if(!Objects.equals(accountService.findById(((CreateCardDTO) values[i]).getAccountID()).getOwnerID(), userId))
                        return false;
                    encounteredDto = true;
                } else if(types[i] == CreateLoanDTO.class) {
                    if(!Objects.equals(accountService.findById(((CreateLoanDTO) values[i]).getAccountId()).getOwnerID(), userId))
                        return false;
                    encounteredDto = true;
                }
            }
            return encounteredDto;
        }
        return false;
    }

    @Around("@annotation(com.banka1.banking.aspect.Authorization)")
    public Object authorizeAction(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        System.out.println("Autorizing 1");

        return doAuth(methodSignature, joinPoint, (claims) -> {
            System.out.println("Autorizing 2");
            Authorization authorization = method.getAnnotation(Authorization.class);

            if(!authorization.customerOnlyOperation()) {
                if(claims.get("isEmployed", Boolean.class)) {
                    // Zaposleni
                    return joinPoint.proceed();
                }
            }

            System.out.println("Autorizing 3");

            if(!authorization.employeeOnlyOperation()) {
                System.out.println("Autorizing 3.1");
                return joinPoint.proceed();
            }

            System.out.println("Autorizing 4");

            // ako je admin
            if(!authorization.disallowAdminFallback() && Objects.equals(claims.get("isAdmin", Boolean.class), true)) {
                return joinPoint.proceed();
            }

            System.out.println("Autorizing 5");

            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.FORBIDDEN), false, null, ResponseMessage.FORBIDDEN.toString());
        });
    }

    @Around("@annotation(com.banka1.banking.aspect.AccountAuthorization)")
    public Object authorizeAccountAction(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        return doAuth(methodSignature, joinPoint, (claims) -> {
            AccountAuthorization authorization = method.getAnnotation(AccountAuthorization.class);

            if(!authorization.customerOnlyOperation()) {
                if(claims.get("isEmployed", Boolean.class)) {
                    // Zaposleni
                    return joinPoint.proceed();
                }
            }

            if(!authorization.employeeOnlyOperation()) {
                OptionalInt maybeUserIdIndex = IntStream.range(0, methodSignature.getParameterNames().length).filter(i -> methodSignature.getParameterNames()[i].compareTo("userId") == 0).findFirst();
                if (maybeUserIdIndex.isPresent()) {
                    Long givenId = Long.parseLong(joinPoint.getArgs()[maybeUserIdIndex.getAsInt()].toString());
                    if (Objects.equals(claims.get("id", Long.class), givenId))
                        return joinPoint.proceed();
                }

                if(accountDataOk(methodSignature, joinPoint, claims.get("id", Long.class)))
                    return joinPoint.proceed();
            }

            // ako je admin
            if(!authorization.disallowAdminFallback() && Objects.equals(claims.get("isAdmin", Boolean.class), true)) {
                return joinPoint.proceed();
            }

            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.FORBIDDEN), false, null, ResponseMessage.FORBIDDEN.toString());
        });
    }

    @Around("@annotation(com.banka1.banking.aspect.CardAuthorization)")
    public Object authorizeCardAction(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        return doAuth(methodSignature, joinPoint, (claims) -> {
            CardAuthorization authorization = method.getAnnotation(CardAuthorization.class);

            if(!authorization.customerOnlyOperation()) {
                if(claims.get("isEmployed", Boolean.class)) {
                    // Zaposleni
                    return joinPoint.proceed();
                }
            }

            if(!authorization.employeeOnlyOperation()) {
                if(accountDataOk(methodSignature, joinPoint, claims.get("id", Long.class)))
                    return joinPoint.proceed();

                OptionalInt maybeCardIdIndex = IntStream.range(0, methodSignature.getParameterNames().length).filter(i -> methodSignature.getParameterNames()[i].compareTo("cardId") == 0).findFirst();
                if (maybeCardIdIndex.isPresent()) {
                    Card card = cardService.findById(Long.valueOf(joinPoint.getArgs()[maybeCardIdIndex.getAsInt()].toString()));
                    if (Objects.equals(claims.get("id", Long.class), card.getAccount().getOwnerID()))
                        return joinPoint.proceed();
                }
            }

            // ako je admin
            if(!authorization.disallowAdminFallback() && Objects.equals(claims.get("isAdmin", Boolean.class), true)) {
                return joinPoint.proceed();
            }

            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.FORBIDDEN), false, null, ResponseMessage.FORBIDDEN.toString());
        });
    }

    @Around("@annotation(com.banka1.banking.aspect.LoanAuthorization)")
    public Object authorizeLoanAction(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        return doAuth(methodSignature, joinPoint, (claims) -> {
            LoanAuthorization authorization = method.getAnnotation(LoanAuthorization.class);

            if(!authorization.customerOnlyOperation()) {
                if(claims.get("isEmployed", Boolean.class)) {
                    // Zaposleni
                    return joinPoint.proceed();
                }
            }

            if(!authorization.employeeOnlyOperation()) {
                if(accountDataOk(methodSignature, joinPoint, claims.get("id", Long.class)))
                    return joinPoint.proceed();

                OptionalInt maybeLoanIdIndex = IntStream.range(0, methodSignature.getParameterNames().length).filter(i -> methodSignature.getParameterNames()[i].compareTo("loanId") == 0).findFirst();
                if(maybeLoanIdIndex.isPresent()) {
                    Loan loan = loanService.getLoanDetails(Long.valueOf(joinPoint.getArgs()[maybeLoanIdIndex.getAsInt()].toString()));
                    if(loan != null) {
                        if (Objects.equals(claims.get("id", Long.class), loan.getAccount().getOwnerID()))
                            return joinPoint.proceed();
                    }
                }
            }

            // ako je admin
            if(!authorization.disallowAdminFallback() && Objects.equals(claims.get("isAdmin", Boolean.class), true)) {
                return joinPoint.proceed();
            }

            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.FORBIDDEN), false, null, ResponseMessage.FORBIDDEN.toString());
        });
    }

    @Around("@annotation(com.banka1.banking.aspect.ReceiverAuthorization)")
    public Object authorizeReceiverAction(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        return doAuth(methodSignature, joinPoint, (claims) -> {
            ReceiverAuthorization authorization = method.getAnnotation(ReceiverAuthorization.class);

            if(!authorization.customerOnlyOperation()) {
                if(claims.get("isEmployed", Boolean.class)) {
                    // Zaposleni
                    System.out.println("Zaposleni, proceed pozvan");
                    return joinPoint.proceed();
                }
            }

            if(!authorization.employeeOnlyOperation()) {
                if(accountDataOk(methodSignature, joinPoint, claims.get("id", Long.class)))
                    return joinPoint.proceed();

                OptionalInt maybeReceiverIdIndex = IntStream.range(0, methodSignature.getParameterNames().length).filter(i -> methodSignature.getParameterNames()[i].compareTo("receiverId") == 0).findFirst();
                if (maybeReceiverIdIndex.isPresent()) {
                    Receiver receiver = receiverService.findById(Long.valueOf(joinPoint.getArgs()[maybeReceiverIdIndex.getAsInt()].toString()));
//                    if (Objects.equals(claims.get("id", Long.class), accountService.findById(receiver.getCustomerId())))
                    if (Objects.equals(claims.get("id", Long.class), receiver.getCustomerId()))
                        return joinPoint.proceed();
                }
            }

            // ako je admin
            if(!authorization.disallowAdminFallback() && Objects.equals(claims.get("isAdmin", Boolean.class), true)) {
                return joinPoint.proceed();
            }

            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.FORBIDDEN), false, null, ResponseMessage.FORBIDDEN.toString());
        });
    }
}
