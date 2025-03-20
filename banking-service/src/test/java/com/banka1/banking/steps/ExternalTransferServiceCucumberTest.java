package com.banka1.banking.steps;

import com.banka1.banking.models.Account;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.models.helper.TransferType;
import com.banka1.banking.services.TransferService;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class ExternalTransferServiceCucumberTest {

    private TransferService transferService;
    private Transfer transfer;
    private Account fromAccount;
    private Account toAccount;
    private String errorMessage;

    @Given("there is an external transfer request")
    public void there_is_an_external_transfer_request() {
        transfer = new Transfer();
        transfer.setId(1L);
        transfer.setType(TransferType.EXTERNAL);
        transfer.setStatus(TransferStatus.PENDING);
        transferService = Mockito.mock(TransferService.class);
    }

    @And("the sender has enough balance")
    public void the_sender_has_enough_balance() {
        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setBalance(1000.0);

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setBalance(500.0);

        transfer.setFromAccountId(fromAccount);
        transfer.setToAccountId(toAccount);
        transfer.setAmount(200.0);
    }

    @And("the sender does not have enough balance")
    public void the_sender_does_not_have_enough_balance() {
        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setBalance(50.0); // Nedovoljno sredstava

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setBalance(500.0);

        transfer.setFromAccountId(fromAccount);
        transfer.setToAccountId(toAccount);
        transfer.setAmount(200.0);
    }

    @When("the external transfer is processed")

    public void the_external_transfer_is_processed() {
        try {
            // FIXME: remove mocking when external transfers exist
            when(transferService.processExternalTransfer(anyLong())).thenAnswer(invocation -> {
                double fee = 5.0; // Provizija
                if (transfer.getAmount() + fee > fromAccount.getBalance()) {
                    throw new RuntimeException("Insufficient balance for transfer");
                }
                fromAccount.setBalance(fromAccount.getBalance() - (transfer.getAmount() + fee));
                toAccount.setBalance(toAccount.getBalance() + transfer.getAmount());
                transfer.setStatus(TransferStatus.COMPLETED);
                return "External transfer completed successfully";
            });
            transferService.processExternalTransfer(1L);
        } catch (RuntimeException e) {
            transfer.setStatus(TransferStatus.FAILED);
            errorMessage = e.getMessage();
        }
    }

    @Then("the transfer status should be {string}")
    public void the_transfer_status_should_be(String expectedStatus) {
        Assertions.assertEquals(TransferStatus.valueOf(expectedStatus), transfer.getStatus());
    }

    @Then("the sender's balance should decrease")
    public void the_sender_s_balance_should_decrease() {
        Assertions.assertTrue(fromAccount.getBalance() < 1000.0);
    }

    @Then("the receiver's balance should increase")
    public void the_receiver_s_balance_should_increase() {
        Assertions.assertTrue(toAccount.getBalance() > 500.0);
    }

    @Then("an error message {string} should be returned")
    public void an_error_message_should_be_returned(String expectedMessage) {
        Assertions.assertEquals(expectedMessage, errorMessage);
    }
}
