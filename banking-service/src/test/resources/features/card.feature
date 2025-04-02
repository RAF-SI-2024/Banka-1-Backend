Feature: Customer Cards

    Scenario: Customer creates a new card
        Given customer is logged into the banking portal for cards
        And customer navigates to card page
        When customer fills out the card form
        And customer presses the Continue button for cards
        Then customer should see a success message for cards

    # Scenario: Customer changes a cards name
    #     Given customer is logged into the banking portal for cards
    #     And customer navigates to card page
    #     When customer selects the card to change the name
    #     And customer fills out the card name form
    #     And customer presses the Change button for cards
    #     Then customer should see a success message for cards

    # Scenario: Customer changes a cards limit

    # Scenario: Customer blocks a card
    #     Given customer is logged into the banking portal for cards
    #     And customer navigates to card page
    #     When customer selects the card to block
    #     And customer presses the Block button for cards
    #     Then customer should see a success message for cards

    