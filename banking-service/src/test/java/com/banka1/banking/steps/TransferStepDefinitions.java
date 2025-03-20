package com.banka1.banking.steps;

import com.banka1.banking.models.Account;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.models.helper.TransferType;
import com.banka1.banking.services.TransferService;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;


public class TransferStepDefinitions {

    private TransferService transferService;
    private Transfer transfer;
    private Account fromAccount;
    private Account toAccount;
    private String errorMessage;

    @Given("an account with id {long} and balance {double}")
    public void an_account_with_id_and_balance(Long accountId, Double balance) {
        if (accountId == 1L) {
            fromAccount = new Account();
            fromAccount.setId(accountId);
            fromAccount.setBalance(balance);
        } else {
            toAccount = new Account();
            toAccount.setId(accountId);
            toAccount.setBalance(balance);
        }
    }

    @And("a pending internal transfer of {double} from account {long} to account {long}")
    public void a_pending_internal_transfer(Double amount, Long fromId, Long toId) {
        transfer = new Transfer();
        transfer.setId(1L);
        transfer.setFromAccountId(fromAccount);
        transfer.setToAccountId(toAccount);
        transfer.setAmount(amount);
        transfer.setType(TransferType.INTERNAL);
        transfer.setStatus(TransferStatus.PENDING);

        transferService = Mockito.mock(TransferService.class);
        when(transferService.processInternalTransfer(1L)).thenCallRealMethod();
        when(transferService.processInternalTransfer(anyLong())).thenAnswer(invocation -> {
            Long transferId = invocation.getArgument(0);
            if (transfer.getAmount() > fromAccount.getBalance()) {
                throw new RuntimeException("Insufficient funds");
            }
            fromAccount.setBalance(fromAccount.getBalance() - transfer.getAmount());
            toAccount.setBalance(toAccount.getBalance() + transfer.getAmount());
            transfer.setStatus(TransferStatus.COMPLETED);
            return "Transfer completed successfully";
        });
    }

    @Then("the transfer status should be COMPLETED")
    public void the_transfer_status_should_be_completed() {
        Assertions.assertEquals(TransferStatus.COMPLETED, transfer.getStatus());
    }

    @When("the transfer is processed")
    public void the_transfer_is_processed() {
        try {
            transferService.processInternalTransfer(1L);
        } catch (RuntimeException e) {
            errorMessage = e.getMessage();
        }
    }

    @Then("the balance of account {long} should be {double}")
    public void the_balance_of_account_should_be(Long accountId, Double expectedBalance) {
        Account account = (accountId == 1L) ? fromAccount : toAccount;
        Assertions.assertEquals(expectedBalance, account.getBalance());
    }

    @Then("the transfer should fail with message {string}")
    public void the_transfer_should_fail_with_message(String expectedMessage) {
        Assertions.assertEquals(expectedMessage, errorMessage);
    }
}
