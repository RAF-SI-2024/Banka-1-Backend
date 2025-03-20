package com.banka1.banking.steps;

import com.banka1.banking.dto.ReceiverDTO;
import com.banka1.banking.models.Receiver;
import com.banka1.banking.repository.ReceiverRepository;
import com.banka1.banking.services.ReceiverService;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

public class ReceiverStepDefinitions {

    @Autowired
    private ReceiverService receiverService;

    @MockBean
    private ReceiverRepository receiverRepository;

    private ReceiverDTO receiverDTO;
    private Receiver receiver;
    private Exception exception;

    @Given("receiver with account number {string} does not exist for account {long}")
    public void receiverDoesNotExist(String accNumber, Long accId) {
        Mockito.when(receiverRepository.existsByOwnerAccountIdAndAccountNumber(accId, accNumber))
                .thenReturn(false);
    }

    @Given("receiver with account number {string} already exists for account {long}")
    public void receiverAlreadyExists(String accNumber, Long accId) {
        Mockito.when(receiverRepository.existsByOwnerAccountIdAndAccountNumber(accId, accNumber))
                .thenReturn(true);
    }

    @When("user creates a receiver with name {string}, address {string}, account number {string} for account {long}")
    public void userCreatesReceiver(String fullName, String address, String accountNumber, Long accountId) {
        receiverDTO = new ReceiverDTO();
        receiverDTO.setFullName(fullName);
        receiverDTO.setAddress(address);
        receiverDTO.setAccountNumber(accountNumber);
        receiverDTO.setOwnerAccountId(accountId);

        try {
            receiver = receiverService.createReceiver(receiverDTO);
        } catch (Exception e) {
            exception = e;
        }
    }

    @Then("receiver should be successfully created")
    public void receiverSuccessfullyCreated() {
        Assertions.assertNotNull(receiver);
        Mockito.verify(receiverRepository).save(Mockito.any(Receiver.class));
    }

    @Then("receiver creation should fail with message {string}")
    public void receiverCreationShouldFail(String expectedMessage) {
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(expectedMessage, exception.getMessage());
    }

    @Given("receiver with id {long} exists")
    public void receiverWithIdExists(Long id) {
        Receiver existingReceiver = new Receiver();
        existingReceiver.setId(id);
        Mockito.when(receiverRepository.findById(id)).thenReturn(Optional.of(existingReceiver));
        Mockito.when(receiverRepository.existsById(id)).thenReturn(true);
    }

    @Given("receiver with id {long} does not exist")
    public void receiverWithIdDoesNotExist(Long id) {
        Mockito.when(receiverRepository.findById(id)).thenReturn(Optional.empty());
        Mockito.when(receiverRepository.existsById(id)).thenReturn(false);
    }

    @When("user updates receiver with id {long} with new account number {string}, name {string}, and address {string}")
    public void userUpdatesReceiver(Long id, String accountNumber, String name, String address) {
        receiverDTO = new ReceiverDTO();
        receiverDTO.setAccountNumber(accountNumber);
        receiverDTO.setFullName(name);
        receiverDTO.setAddress(address);

        try {
            receiver = receiverService.updateReceiver(id, receiverDTO);
        } catch (Exception e) {
            exception = e;
        }
    }

    @Then("receiver should be successfully updated")
    public void receiverSuccessfullyUpdated() {
        Assertions.assertNotNull(receiver);
        Mockito.verify(receiverRepository).save(Mockito.any(Receiver.class));
    }

    @Then("receiver update should fail with message {string}")
    public void receiverUpdateShouldFail(String expectedMessage) {
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(expectedMessage, exception.getMessage());
    }

    @When("user deletes receiver with id {long}")
    public void userDeletesReceiver(Long id) {
        try {
            receiverService.deleteReceiver(id);
        } catch (Exception e) {
            exception = e;
        }
    }

    @Then("receiver should be successfully deleted")
    public void receiverSuccessfullyDeleted() {
        Assertions.assertNull(exception);
        Mockito.verify(receiverRepository).deleteById(Mockito.anyLong());
    }

    @Then("receiver deletion should fail with message {string}")
    public void receiverDeletionShouldFail(String expectedMessage) {
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(expectedMessage, exception.getMessage());
    }
}
