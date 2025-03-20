Feature: OTP Token Service Management

  Scenario: Successfully generate OTP token
    When generating OTP for transfer id 1001
    Then OTP should be successfully generated and saved

  Scenario: Validate OTP token successfully
    Given OTP "123456" exists and is unused for transfer id 1001
    When validating OTP "123456" for transfer id 1001
    Then OTP validation should succeed

  Scenario: OTP validation fails due to expiration
    Given OTP for transfer id 1002 is expired
    When checking if OTP for transfer id 1002 is expired
    Then OTP expiration check should return true
