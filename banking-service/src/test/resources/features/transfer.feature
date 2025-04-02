Feature: Customer transfers

    Scenario: Internal Transfer Successful
        Given customer is logged into the banking portal for transfers
        And customer navigates to transfer page
        When customer fills out the transfer form
        And customer presses the Continue button
        Then customer should be prompted to enter verification code

    Scenario: Internal Transfer Unsuccessful
        Given customer is logged into the banking portal for transfers
        And customer navigates to transfer page
        When customer does not fill out the transfer form
        And customer presses the Continue button
        Then customer should see an error message

    Scenario: Non Internal Transfer Unsuccessful
        Given customer is logged into the banking portal for transfers
        And customer navigates to payment page
        When customer does not fill out the payment form
        And customer presses Continue button
        Then customer should see an error message