package com.banka1.banking.steps;

import com.banka1.banking.models.OtpToken;
import com.banka1.banking.repository.OtpTokenRepository;
import com.banka1.banking.services.OtpTokenService;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

public class OtpStepDefinitions {

    @Autowired
    private OtpTokenService otpTokenService;

    @MockBean
    private OtpTokenRepository otpTokenRepository;

    private String otp;
    private boolean valid;
    private boolean expired;

    @When("generating OTP for transfer id {long}")
    public void generateOtp(Long transferId) {
        otp = otpTokenService.generateOtp(transferId);
    }

    @Then("OTP should be successfully generated and saved")
    public void otpGenerated() {
        Assertions.assertNotNull(otp);
        Mockito.verify(otpTokenRepository).saveAndFlush(Mockito.any());
    }

    @Given("OTP {string} exists and is unused for transfer id {long}")
    public void otpExistsUnused(String otpCode, Long transferId) {
        OtpToken token = new OtpToken();
        token.setUsed(false);
        Mockito.when(otpTokenRepository.findByTransferIdAndOtpCode(transferId, otpCode))
                .thenReturn(Optional.of(token));
    }

    @When("validating OTP {string} for transfer id {long}")
    public void validateOtp(String otpCode, Long transferId) {
        valid = otpTokenService.isOtpValid(transferId, otpCode);
    }

    @Then("OTP validation should succeed")
    public void otpValidationSucceeds() {
        Assertions.assertTrue(valid);
    }

    @Given("OTP for transfer id {long} is expired")
    public void otpExpired(Long transferId) {
        OtpToken otpToken = new OtpToken();
        otpToken.setExpirationTime(System.currentTimeMillis() - 60000);
        Mockito.when(otpTokenRepository.findByTransferId(transferId))
                .thenReturn(Optional.of(otpToken));
    }

    @When("checking if OTP for transfer id {long} is expired")
    public void checkExpiration(Long transferId) {
        expired = otpTokenService.isOtpExpired(transferId);
    }

    @Then("OTP expiration check should return true")
    public void otpExpiredTrue() {
        Assertions.assertTrue(expired);
    }
}
