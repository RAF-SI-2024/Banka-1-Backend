-- Delete previous data
TRUNCATE TABLE installment CASCADE;
TRUNCATE TABLE loan CASCADE;
TRUNCATE TABLE receiver CASCADE;
TRUNCATE TABLE transaction CASCADE;
TRUNCATE TABLE account CASCADE;
TRUNCATE TABLE company CASCADE;
TRUNCATE TABLE currency CASCADE;
TRUNCATE TABLE rate_change CASCADE;

-- Reset sequences after truncating tables
ALTER SEQUENCE account_id_seq RESTART WITH 100;
ALTER SEQUENCE company_id_seq RESTART WITH 100;
ALTER SEQUENCE currency_id_seq RESTART WITH 1;
ALTER SEQUENCE exchange_pair_id_seq RESTART WITH 1;
ALTER SEQUENCE installment_id_seq RESTART WITH 1;
ALTER SEQUENCE loan_id_seq RESTART WITH 1;
ALTER SEQUENCE otp_token_id_seq RESTART WITH 1;
ALTER SEQUENCE receiver_id_seq RESTART WITH 1;
ALTER SEQUENCE transfer_id_seq RESTART WITH 1;
ALTER SEQUENCE transaction_id_seq RESTART WITH 1;


-- Populate currencies
INSERT INTO currency (code, name, country, symbol)
VALUES ('RSD', 'Serbian Dinar', 'Serbia', 'дин.');
INSERT INTO currency (code, name, country, symbol)
VALUES ('EUR', 'Euro', 'European Union', '€');
INSERT INTO currency (code, name, country, symbol)
VALUES ('USD', 'US Dollar', 'United States', '$');
INSERT INTO currency (code, name, country, symbol)
VALUES ('GBP', 'British Pound', 'United Kingdom', '£');
INSERT INTO currency (code, name, country, symbol)
VALUES ('CHF', 'Swiss Franc', 'Switzerland', 'Fr');
INSERT INTO currency (code, name, country, symbol)
VALUES ('JPY', 'Japanese Yen', 'Japan', '¥');
INSERT INTO currency (code, name, country, symbol)
VALUES ('CAD', 'Canadian Dollar', 'Canada', 'C$');
INSERT INTO currency (code, name, country, symbol)
VALUES ('AUD', 'Australian Dollar', 'Australia', 'A$');

INSERT INTO company (id, name, address, vat_number, company_number, bas, ownerid)
VALUES
    (1, 'Naša Banka', 'Bulevar Banka 1', '111111111', '11111111','BANK',7),
    (2, 'Naša Država', 'Bulevar Država 1', '222222222', '22222222','COUNTRY',8);

INSERT INTO account (id,account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES
-- RSD - domaća valuta - 10 milijardi
(1,'111000100000000199', 10000000000.0, 1, 10000000.0, 100000000.0, 0.0, 0.0, 'RSD',
 2029030500000, 2025030500000, 1, 0.0, 0.0, 7, 'ACTIVE', 'BANK', 'STANDARD'),

-- Ostale valute - 10 miliona
(2,'111000100000000299', 10000000.0, 1, 1000000.0, 1000000.0, 0.0, 0.0, 'EUR',
 2029030500000, 2025030500000, 1, 0.0, 0.0, 7, 'ACTIVE', 'BANK', 'STANDARD'),

(3,'111000100000000399', 10000000.0, 1, 1000000.0, 1000000.0, 0.0, 0.0, 'USD',
 2029030500000, 2025030500000, 1, 0.0, 0.0, 7, 'ACTIVE', 'BANK', 'STANDARD'),

(4,'111000100000000499', 10000000.0, 1, 1000000.0, 1000000.0, 0.0, 0.0, 'CHF',
 2029030500000, 2025030500000, 1, 0.0, 0.0, 7, 'ACTIVE', 'BANK', 'STANDARD'),

(5,'111000100000000599', 10000000.0, 1, 1000000.0, 1000000.0, 0.0, 0.0, 'GBP',
 2029030500000, 2025030500000, 1, 0.0, 0.0, 7, 'ACTIVE', 'BANK', 'STANDARD'),

(6,'111000100000000699', 10000000.0, 1, 1000000.0, 1000000.0, 0.0, 0.0, 'JPY',
 2029030500000, 2025030500000, 1, 0.0, 0.0, 7, 'ACTIVE', 'BANK', 'STANDARD'),

(7,'111000100000000799', 10000000.0, 1, 1000000.0, 1000000.0, 0.0, 0.0, 'CAD',
 2029030500000, 2025030500000, 1, 0.0, 0.0, 7, 'ACTIVE', 'BANK', 'STANDARD'),

(8,'111000100000000899', 10000000.0, 1, 1000000.0, 1000000.0, 0.0, 0.0, 'AUD',
 2029030500000, 2025030500000, 1, 0.0, 0.0, 7, 'ACTIVE', 'BANK', 'STANDARD'),
-- RSD - domaća valuta za nasu drzavu
(9,'111000100000001199', 10000000000.0, 2, 10000000.0, 100000000.0, 0.0, 0.0, 'RSD',
 2029030500000, 2025030500000, 1, 0.0, 0.0, 8, 'ACTIVE', 'COUNTRY', 'STANDARD');

-- User accounts - Marko (ID: 1)
INSERT INTO account (id,account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES (10,'111000112345678910', 5000.0, NULL, 5000.0, 1000.0, 0.0, 0.0, 'RSD',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 1, 'ACTIVE', 'CURRENT', 'STANDARD');

-- User accounts - Anastasija (ID: 2)
INSERT INTO account (id,account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES (11,'111000111225344510', 2500.0, NULL, 500.0, 500.0, 0.0, 0.0, 'RSD',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 2, 'ACTIVE', 'CURRENT', 'STANDARD');


-- User accounts - Jovan (ID: 3)
INSERT INTO account (account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES ('111000100000000110', 100000.0, NULL, 10000.0, 100000.0, 0.0, 0.0, 'RSD',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 3, 'ACTIVE', 'CURRENT', 'STANDARD');

INSERT INTO account (account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES ('111000100011000110', 1000000.0, NULL, 0.0, 0.0, 0.0, 0.0, 'RSD',
        1630454400000, 2025030500000, 2, 0.0, 0.0, 3, 'ACTIVE', 'CURRENT', 'SAVINGS');

INSERT INTO account (account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES ('111000100000000120', 1000.0, NULL, 200.0, 10000.0, 0.0, 0.0, 'EUR',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 3, 'ACTIVE', 'FOREIGN_CURRENCY', 'STANDARD');

INSERT INTO account (account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES ('111000100220000120', 1000.0, NULL, 100.0, 1000.0, 0.0, 0.0, 'EUR',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 3, 'ACTIVE', 'FOREIGN_CURRENCY', 'PENSION');

INSERT INTO account (account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES ('111000100000000320', 1000.0, NULL, 200.0, 10000.0, 0.0, 0.0, 'USD',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 3, 'ACTIVE', 'FOREIGN_CURRENCY', 'STANDARD');

-- User accounts - Nemanja (ID: 4)
INSERT INTO account (account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES ('111000100000000210', 100000.0, NULL, 10000.0, 100000.0, 0.0, 0.0, 'RSD',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 4, 'ACTIVE', 'CURRENT', 'STANDARD');

-- User accounts - Nikola (ID: 5)
INSERT INTO account (id,account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES (18,'111000177655544310', 2500.0, NULL, 500.0, 500.0, 0.0, 0.0, 'RSD',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 5, 'ACTIVE', 'CURRENT', 'STANDARD');

-- User accounts - Jelena (ID: 6)
INSERT INTO account (id,account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES (19,'111000122344455610', 20000.0, NULL, 10000.0, 5000.0, 0.0, 0.0, 'RSD',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 6, 'ACTIVE', 'CURRENT', 'STANDARD');

-- User accounts - Banka (ID: 7)
INSERT INTO account (id,account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES (20,'111000122344475610', 2000000.0, 1, 10000.0, 5000.0, 0.0, 0.0, 'RSD',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 6, 'ACTIVE', 'CURRENT', 'STANDARD');

-- User accounts - Drzava (ID: 8)
INSERT INTO account (id,account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES (21,'111000122344457610', 20000000.0, 2, 10000.0, 5000.0, 0.0, 0.0, 'RSD',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 6, 'ACTIVE', 'CURRENT', 'STANDARD');

-- Receivers
INSERT INTO receiver (owner_account_id, account_number, first_name, last_name)
VALUES (1, '111000100000000210', 'Nemanja', 'Marjanov');

INSERT INTO receiver (owner_account_id, account_number, first_name, last_name)
VALUES (1, '111000100330222210', 'Nikola', 'Nikolic');

INSERT INTO receiver (owner_account_id, account_number, first_name, last_name)
VALUES (1, '111000100335672210', 'Jelena', 'Jovanovic');

INSERT INTO receiver (owner_account_id, account_number, first_name, last_name)
VALUES (3, '111000100000000220', 'Nemanja', 'Marjanov');

INSERT INTO receiver (owner_account_id, account_number, first_name, last_name)
VALUES (3, '111000100366112220', 'Jelena', 'Jovanovic');

-- Loans
INSERT INTO loan (number_of_installments, loan_type, currency_type, interest_type,
                  payment_status, nominal_rate, effective_rate, loan_amount,
                  created_date, allowed_date, monthly_payment, next_payment_date,
                  remaining_amount, loan_reason, account_id, phone_number)
VALUES (3, 'CASH', 'RSD', 'FIXED', 'PENDING', 5.5, 6.0, 500000.0,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP),
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP + INTERVAL '7 days'),
        22000.0, TO_CHAR(CURRENT_TIMESTAMP + INTERVAL '30 days', 'YYYY-MM-DD'),
        500000.0, 'Home renovation', 100, '+123456789');

INSERT INTO loan (number_of_installments, loan_type, currency_type, interest_type,
                  payment_status, nominal_rate, effective_rate, loan_amount,
                  created_date, allowed_date, monthly_payment, next_payment_date,
                  remaining_amount, loan_reason, account_id, phone_number)
VALUES (3, 'CASH', 'RSD', 'FIXED', 'PENDING', 5.5, 6.0, 550000.0,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP),
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP + INTERVAL '7 days'),
        22000.0, TO_CHAR(CURRENT_TIMESTAMP + INTERVAL '30 days', 'YYYY-MM-DD'),
        500000.0, 'Home renovation, attempt 2', 100, '+123456789');

-- User accounts - Marko (ID: 1)
INSERT INTO loan (number_of_installments, loan_type, currency_type, interest_type,
                  payment_status, nominal_rate, effective_rate, loan_amount,
                  created_date, allowed_date, monthly_payment, next_payment_date,
                  remaining_amount, loan_reason, account_id, phone_number)
VALUES (3, 'CASH', 'RSD', 'FIXED', 'PENDING', 5.5, 6.0, 500000.0,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) ,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP + INTERVAL '7 days'),
        22000.0, TO_CHAR(CURRENT_TIMESTAMP + INTERVAL '30 days', 'YYYY-MM-DD'),
        500000.0, 'Home renovation', 100, '+123456789');

-- User accounts - Anastasija (ID: 2)
INSERT INTO loan (number_of_installments, loan_type, currency_type, interest_type,
                  payment_status, nominal_rate, effective_rate, loan_amount,
                  created_date, allowed_date, monthly_payment, next_payment_date,
                  remaining_amount, loan_reason, account_id, phone_number)
VALUES (24, 'CASH', 'RSD', 'FIXED', 'PENDING', 5.5, 6.0, 550000.0,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) ,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP + INTERVAL '7 days'),
        22916.0, TO_CHAR(CURRENT_TIMESTAMP + INTERVAL '30 days', 'YYYY-MM-DD'),
        500000.0, 'Medical Expenses', 2, '+123456789');

-- User accounts - Jovan (ID: 3)
INSERT INTO loan (number_of_installments, loan_type, currency_type, interest_type,
                  payment_status, nominal_rate, effective_rate, loan_amount,
                  created_date, allowed_date, monthly_payment, next_payment_date,
                  remaining_amount, loan_reason, account_id, phone_number)
VALUES (12, 'CASH', 'RSD', 'FIXED', 'PENDING', 5.5, 6.0, 5000.0,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) ,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP + INTERVAL '7 days'),
        416.0, TO_CHAR(CURRENT_TIMESTAMP + INTERVAL '30 days', 'YYYY-MM-DD'),
        5000.0, 'Travel', 3, '+123456789');

-- User accounts - Nemanja  (ID: 4)
INSERT INTO loan (number_of_installments, loan_type, currency_type, interest_type,
                  payment_status, nominal_rate, effective_rate, loan_amount,
                  created_date, allowed_date, monthly_payment, next_payment_date,
                  remaining_amount, loan_reason, account_id, phone_number)
VALUES (2, 'CASH', 'RSD', 'FIXED', 'PENDING', 5.5, 6.0, 5000.0,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) ,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP + INTERVAL '7 days'),
        2500.0, TO_CHAR(CURRENT_TIMESTAMP + INTERVAL '30 days', 'YYYY-MM-DD'),
        5000.0, 'Investment', 4, '+123456789');

-- User accounts - Nikola  (ID: 5)
INSERT INTO loan (number_of_installments, loan_type, currency_type, interest_type,
                  payment_status, nominal_rate, effective_rate, loan_amount,
                  created_date, allowed_date, monthly_payment, next_payment_date,
                  remaining_amount, loan_reason, account_id, phone_number)
VALUES (12, 'CASH', 'RSD', 'FIXED', 'PAID_OFF', 5.5, 6.0, 120000.0,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) ,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP + INTERVAL '7 days'),
        10000.0, TO_CHAR(CURRENT_TIMESTAMP + INTERVAL '30 days', 'YYYY-MM-DD'),
        0.0, 'Starting a Business ', 5, '+123456789');

INSERT INTO loan (number_of_installments, loan_type, currency_type, interest_type,
                  payment_status, nominal_rate, effective_rate, loan_amount,
                  created_date, allowed_date, monthly_payment, next_payment_date,
                  remaining_amount, loan_reason, account_id, phone_number)
VALUES (3, 'CASH', 'RSD', 'FIXED', 'PENDING', 5.5, 6.0, 550000.0,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) ,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP + INTERVAL '7 days'),
        22000.0, TO_CHAR(CURRENT_TIMESTAMP + INTERVAL '30 days', 'YYYY-MM-DD'),
        500000.0, 'Home renovation, attempt 2', 100, '+123456789');

-- User accounts - Jelena  (ID: 6)
INSERT INTO loan (number_of_installments, loan_type, currency_type, interest_type,
                  payment_status, nominal_rate, effective_rate, loan_amount,
                  created_date, allowed_date, monthly_payment, next_payment_date,
                  remaining_amount, loan_reason, account_id, phone_number)
VALUES (12, 'CASH', 'RSD', 'FIXED', 'APPROVED', 5.5, 6.0, 120000.0,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) ,
        EXTRACT(EPOCH FROM CURRENT_TIMESTAMP + INTERVAL '7 days'),
        10000.0, TO_CHAR(CURRENT_TIMESTAMP + INTERVAL '30 days', 'YYYY-MM-DD'),
        120000.0, 'Starting a Business ', 6, '+123456789');

-- Accounts for Marko Marković (ID: 1)
DELETE FROM account WHERE ownerid = 1;

-- Standard RSD Current account (matches Jovan's first account)
INSERT INTO account (account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES ('111000100000000101', 100000.0, NULL, 10000.0, 100000.0, 0.0, 0.0, 'RSD',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 1, 'ACTIVE', 'CURRENT', 'STANDARD');
-- RSD Savings account (matches Jovan's second account)
INSERT INTO account (account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES ('111000100011000101', 1000000.0, NULL, 0.0, 0.0, 0.0, 0.0, 'RSD',
        1630454400000, 2025030500000, 2, 0.0, 0.0, 1, 'ACTIVE', 'CURRENT', 'SAVINGS');

-- EUR Foreign Currency account (matches Jovan's third account)
INSERT INTO account (account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES ('111000100000000121', 1000.0, NULL, 200.0, 10000.0, 0.0, 0.0, 'EUR',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 1, 'ACTIVE', 'FOREIGN_CURRENCY', 'STANDARD');

-- EUR Foreign Currency Pension account (matches Jovan's fourth account)
INSERT INTO account (account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES ('111000100220000121', 1000.0, NULL, 100.0, 1000.0, 0.0, 0.0, 'EUR',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 1, 'ACTIVE', 'FOREIGN_CURRENCY', 'PENSION');

-- USD Foreign Currency account (matches Jovan's fifth account)
INSERT INTO account (account_number, balance, company_id, daily_limit, monthly_limit,
                     daily_spent, monthly_spent, currency_type, expiration_date, created_date,
                     employeeid, monthly_maintenance_fee, reserved_balance, ownerid,
                     status, type, subtype)
VALUES ('111000100000000321', 1000.0, NULL, 200.0, 10000.0, 0.0, 0.0, 'USD',
        1630454400000, 2025030500000, 1, 0.0, 0.0, 1, 'ACTIVE', 'FOREIGN_CURRENCY', 'STANDARD');
-- ID: 1
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,1000.0,1,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'VISA',180,'BANKA',4098745621983456,'DEBIT');
--ID: 2
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,3000.0,2,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'VISA',111,'BANKA',4712563490806352,'DEBIT');
--ID:3
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,10000.0,3,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'MASTERCARD',334,'BANKA',5112345678901234,'DEBIT');
--ID:4
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,10000.0,4,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'MASTERCARD',445,'BANKA',5223456789012345,'DEBIT');
--ID:5
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,10000.0,5,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'MASTERCARD',444,'BANKA',5334567890123456,'DEBIT');
--ID:6
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,3000.0,6,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'VISA',123,'BANKA',4712563490806352,'DEBIT');
--ID:7
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,5000.0,7,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'VISA',678,'BANKA',4109876543289672,'DEBIT');
--ID:8
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,45600.0,8,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'VISA',766,'BANKA',4938291045067853,'DEBIT');
--ID:9
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,60000.0,9,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'VISA',866,'BANKA',4539127081265341,'DEBIT');
--ID:11
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,300000.0,11,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'VISA',966,'BANKA',4002783468102943,'DEBIT');
--ID:12
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,450000.0,100,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'MASTERCARD',670,'BANKA',5123456789012346,'DEBIT');
--ID:13
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,34500.0,101,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'MASTERCARD',234,'BANKA',5234567890123457,'DEBIT');
--ID:14
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,56700.0,102,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'MASTERCARD',235,'BANKA',5345678901234568,'DEBIT');
--ID:15
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,3400.0,103,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'MASTERCARD',236,'BANKA',5456789012345679,'DEBIT');
--ID:16
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,13000.0,104,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'DINA_CARD',444,'BANKA',9891123456789012,'DEBIT');
--ID:17
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,23000.0,105,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'DINA_CARD',445,'BANKA',9891567890123456,'DEBIT');
--ID:18
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,33000.0,18,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'DINA_CARD',466,'BANKA',9891987654321098,'DEBIT');
--ID:19
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,43000.0,19,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'AMERICAN_EXPRESS',998,'BANKA',341234567890123,'DEBIT');
--ID:20
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,43000.0,20,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'AMERICAN_EXPRESS',997,'BANKA',371987654321098,'DEBIT');
--ID:21
INSERT INTO card(ACTIVE, BLOCKED, CARD_LIMIT, ACCOUNT_ID, AUTHORIZED_PERSON_ID, CREATED_AT, EXPIRATION_DATE, CARD_BRAND, CARD_CVV, CARD_NAME, CARD_NUMBER, CARD_TYPE)
VALUES (TRUE,FALSE,34000.0,21,NULL,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*365,'AMERICAN_EXPRESS',996,'BANKA',349876543210987,'DEBIT');


--- TRANSFERI I TRANSAKCIJE

INSERT INTO transfer(amount, completed_at, created_at, from_account_id, from_currency_id, to_account_id, to_currency_id, adress, note, otp, payment_code, payment_description, payment_reference, receiver, status, type)
VALUES (1000.0, EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*2, EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000, 100, 1, 20, 1, 'Ustanicka 1', 'Payment for services', '123', '234', 'Payment for services', '94', 'Jelena Jovanovic', 'COMPLETED', 'INTERNAL');

-- Jovan (ID:3 Racun:1 ) -> Jelena (ID:6)
INSERT INTO transfer(amount, completed_at, created_at, from_account_id, from_currency_id, to_account_id, to_currency_id, adress, note, otp, payment_code, payment_description, payment_reference, receiver, status, type)
VALUES (1000.0, EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*2, EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000, 100, 1, 20, 1, 'Ustanicka 1', 'Payment for services', '123', '234', 'Payment for services', '94', 'Jelena Jovanovic', 'COMPLETED', 'INTERNAL');

INSERT INTO transaction(amount, final_amount, fee, bank_only, currency_id, from_account_id, timestamp, to_account_id,transfer_id, description)
VALUES (1000.0,1000.0,0.0,false,1,100,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,20,2,'Payment for services');
INSERT INTO transaction(amount, final_amount, fee, bank_only, currency_id, from_account_id, timestamp, to_account_id,transfer_id, description)
VALUES (1000.0,1000.0,0.0,false,1,100,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,20,2,'Payment for services');

-- Anastasija -> Jovan
INSERT INTO transfer(amount, completed_at, created_at, from_account_id, from_currency_id, to_account_id, to_currency_id, adress, note, otp, payment_code, payment_description, payment_reference, receiver, status, type)
VALUES (30000.0, EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000*2, EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000, 11, 1, 100, 1, 'Milana Mijalkovica 1', 'Payment for shopping', '233', '234', 'Payment for shopping', '95', null, 'COMPLETED', 'INTERNAL');

INSERT INTO transaction(amount, final_amount, fee, bank_only, currency_id, from_account_id, timestamp, to_account_id,transfer_id, description)
VALUES (30000.0,30000.0,0.0,false,1,11,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,100,3,'Payment for shopping');
INSERT INTO transaction(amount, final_amount, fee, bank_only, currency_id, from_account_id, timestamp, to_account_id,transfer_id, description)
VALUES (1000.0,1000.0,0.0,false,1,11,EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000,100,3,'Payment for shopping');

