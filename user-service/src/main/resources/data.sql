-- Delete previous data
TRUNCATE TABLE customer_permissions CASCADE;
TRUNCATE TABLE employee_permissions CASCADE;
TRUNCATE TABLE customer CASCADE;
TRUNCATE TABLE employee CASCADE;
TRUNCATE TABLE reset_password CASCADE;
TRUNCATE TABLE set_password CASCADE;


ALTER SEQUENCE customer_id_seq RESTART WITH 100;
ALTER SEQUENCE employee_id_seq RESTART WITH 100;

-- ============== Loading Data ==============

-- Admin user password: admin123
INSERT INTO employee (id, first_name, last_name, email, password, is_admin, phone_number,
                      birth_date, gender, department, position, active, address,
                      salt_password, username)
VALUES (1, 'Admin', 'Admin', 'admin@admin.com',
        '$2a$12$kQjtxwqw9hBDCW6Lypc8eO9UC1kNqpGAfbQKc8voUl2bNeM.e.8Hy', TRUE, '1234567890',
        '2000-01-01', 'MALE', 'HR', 'DIRECTOR', TRUE, 'Admin Address',
        'salt', 'admin123');

-- Insert all permissions for admin
INSERT INTO employee_permissions (employee_id, permission)
SELECT 1, unnest(ARRAY[
                     'CREATE_CUSTOMER', 'DELETE_CUSTOMER', 'LIST_CUSTOMER', 'EDIT_CUSTOMER', 'READ_CUSTOMER',
                 'SET_CUSTOMER_PERMISSION', 'SET_EMPLOYEE_PERMISSION', 'DELETE_EMPLOYEE', 'EDIT_EMPLOYEE',
                 'LIST_EMPLOYEE', 'READ_EMPLOYEE', 'CREATE_EMPLOYEE'
                     ]);

-- Employee 1: Petar password: Per@12345
INSERT INTO employee (id, first_name, last_name, email, username, phone_number,
                      birth_date, address, gender, position, department, active,
                      is_admin, salt_password, password)
VALUES (2, 'Petar', 'Petrović', 'petar.petrovic@banka.com', 'perica',
        '+381641001000', '1990-07-07', 'Knez Mihailova 5', 'MALE', 'MANAGER',
        'IT', TRUE, FALSE, 'salt', '$2a$12$PZ0G6ao.1QMAEZlPHoDo0OBYM.Y/rglJJ5rtuyctPEqaLES/elNhW');

-- Permissions for Petar
INSERT INTO employee_permissions (employee_id, permission)
SELECT 2, unnest(ARRAY[
                     'CREATE_CUSTOMER', 'DELETE_CUSTOMER', 'LIST_CUSTOMER', 'EDIT_CUSTOMER', 'READ_CUSTOMER',
                 'SET_CUSTOMER_PERMISSION', 'SET_EMPLOYEE_PERMISSION', 'DELETE_EMPLOYEE', 'EDIT_EMPLOYEE',
                 'LIST_EMPLOYEE', 'READ_EMPLOYEE', 'CREATE_EMPLOYEE'
                     ]);

-- Employee 2: Jovana password: Jovan@12345
INSERT INTO employee (id, first_name, last_name, email, username, phone_number,
                      birth_date, address, gender, position, department, active,
                      is_admin, salt_password, password)
VALUES (3, 'Jovana', 'Jovanović', 'jovana.jovanovic@banka.com', 'jjovanaa',
        '+381641001001', '2000-10-10', 'Knez Mihailova 6', 'FEMALE', 'WORKER',
        'HR', TRUE, FALSE, 'salt', '$2a$12$c2Y5721b8h0B0olC.xrQ.eHw2UR.67NAsDvyEkAbRo/a2dW5Tr.ge');

-- Permissions for Jovana
INSERT INTO employee_permissions (employee_id, permission)
SELECT 3, unnest(ARRAY[
                     'READ_CUSTOMER', 'CREATE_CUSTOMER', 'DELETE_CUSTOMER', 'LIST_CUSTOMER', 'EDIT_CUSTOMER'
                     ]);

-- Employee 3: Nikolina
INSERT INTO employee (id, first_name, last_name, email, username, phone_number,
                      birth_date, address, gender, position, department, active,
                      is_admin, salt_password, password)
VALUES (4, 'Nikolina', 'Jovanović', 'nikolina.jovanovic@banka.com', 'nikolinaaa',
        '+381641001001', '2000-10-10', 'Knez Mihailova 6', 'FEMALE', 'WORKER',
        'SUPERVISOR', TRUE, FALSE, 'salt', '$2a$12$c2Y5721b8h0B0olC.xrQ.eHw2UR.67NAsDvyEkAbRo/a2dW5Tr.ge');

-- Permissions for Nikolina password: Jovan@12345
INSERT INTO employee_permissions (employee_id, permission)
SELECT 4, unnest(ARRAY[
                     'READ_CUSTOMER', 'CREATE_CUSTOMER', 'DELETE_CUSTOMER', 'LIST_CUSTOMER', 'EDIT_CUSTOMER'
                     ]);

-- Employee 4: Milica password: Jovan@12345
INSERT INTO employee (id, first_name, last_name, email, username, phone_number,
                      birth_date, address, gender, position, department, active,
                      is_admin, salt_password, password)
VALUES (5, 'Milica', 'Jovanović', 'milica.jovanovic@banka.com', 'milicaaaa',
        '+381641001001', '2000-10-10', 'Knez Mihailova 6', 'FEMALE', 'WORKER',
        'AGENT', TRUE, FALSE, 'salt', '$2a$12$c2Y5721b8h0B0olC.xrQ.eHw2UR.67NAsDvyEkAbRo/a2dW5Tr.ge');

-- Permissions for Milica
INSERT INTO employee_permissions (employee_id, permission)
SELECT 5, unnest(ARRAY[
                     'READ_CUSTOMER', 'CREATE_CUSTOMER', 'DELETE_CUSTOMER', 'LIST_CUSTOMER', 'EDIT_CUSTOMER'
                     ]);

-- Customer 1: Marko password: M@rko12345
INSERT INTO customer (id, first_name, last_name, email, username, phone_number,
                      birth_date, gender, address, salt_password, password)
VALUES (1, 'Marko', 'Marković', 'marko.markovic@banka.com', 'okram',
        '+381641001002', '2005-12-12', 'MALE', 'Knez Mihailova 7',
        'salt', '$2a$12$xVyIW24AWKBuIx5b/7/Ah.lB0RZE1ZUOnEvPHxibd22binrXucWNe');

-- Permissions for Marko
INSERT INTO customer_permissions (customer_id, permission)
VALUES (1, 'READ_EMPLOYEE');

-- Customer 2: Anastasija password: Anastas12345
INSERT INTO customer (id, first_name, last_name, email, username, phone_number,
                      birth_date, gender, address, salt_password, password)
VALUES (2, 'Anastasija', 'Milinković', 'anastasija.milinkovic@banka.com', 'anastass',
        '+381641001003', '2001-02-02', 'FEMALE', 'Knez Mihailova 8',
        'salt', '$2a$12$2Yfp1FfciGa1R.nlc1bngu24dZMUp2zVfQmE4MxNqF8V2uzozLJ/y');

-- Permissions for Anastasija
INSERT INTO customer_permissions (customer_id, permission)
VALUES (2, 'READ_EMPLOYEE');

-- Customer 3: Jovan password: Jov@njovan1
INSERT INTO customer (id, first_name, last_name, email, username, phone_number,
                      birth_date, gender, address, salt_password, password)
VALUES (3, 'Jovan', 'Pavlovic', 'jpavlovic6521rn@raf.rs', 'jovan',
        '+381641001003', '2001-02-02', 'MALE', 'Knez Mihailova 8',
        'salt', '$2a$12$B4RMQRQx9i5pGBfCDuoXjOcfchISf0XlILBaD31UeSw3pt6SSrT4q');

-- Permissions for Jovan
INSERT INTO customer_permissions (customer_id, permission)
VALUES (3, 'READ_EMPLOYEE');

-- Customer 4: Nemanja password: Nemanjanemanj@1
INSERT INTO customer (id, first_name, last_name, email, username, phone_number,
                      birth_date, gender, address, salt_password, password)
VALUES (4, 'Nemanja', 'Marjanov', 'nmarjanov6121rn@raf.rs', 'nemanja',
        '+381641001123', '2001-02-02', 'MALE', 'Knez Mihailova 8',
        'salt', '$2a$12$qjdsLTEtrH9Duodh6UMNaOyCUD2SWQp6rnlkxRJqZlCDVhF3poMRy');

-- Permissions for Nemanja
INSERT INTO customer_permissions (customer_id, permission)
VALUES (4, 'READ_EMPLOYEE');

-- Customer 5: Nikola password: Nikola12345
INSERT INTO customer (id, first_name, last_name, email, username, phone_number,
                      birth_date, gender, address, salt_password, password)
VALUES (5, 'Nikola', 'Nikolic', 'primer@primer.rs', 'nikkola',
        '+381641001303', '2001-02-02', 'MALE', 'Knez Mihailova 8',
        'salt', '$2a$12$eVZVYiosl7hCVHQVAKRbxeTppP7YPNnk4BjR6qreFjpgDWwmKsEF2');

-- Permissions for Nikola
INSERT INTO customer_permissions (customer_id, permission)
VALUES (5, 'READ_EMPLOYEE');

-- Customer 6: Jelena password: nemanjanemanja
INSERT INTO customer (id, first_name, last_name, email, username, phone_number,
                      birth_date, gender, address, salt_password, password)
VALUES (6, 'Jelena', 'Jovanovic', 'jelena@primer.rs', 'jelena',
        '+381621001003', '2001-02-02', 'FEMALE', 'Knez Mihailova 8',
        'salt', '$2a$12$NcVWqq8vsDVDBLezBJAchepu9WXMUpAfwR0yF6eBjpNYogVoz6wYS');

-- Permissions for Jelena
INSERT INTO customer_permissions (customer_id, permission)
VALUES (6, 'READ_EMPLOYEE');

-- Customer: BANKA (email: bankabanka@banka1.com, password: nemanjanemanja)
INSERT INTO customer (id, first_name, last_name, email, username, phone_number,
                      birth_date, gender, address, salt_password, password)
VALUES (7, 'Banka', 'Banka', 'bankabanka@banka1.com', 'bankabanka',
        '+381640000000', '2025-01-01', 'MALE', 'Bulevar Banka 1',
        'salt', '$2a$12$NcVWqq8vsDVDBLezBJAchepu9WXMUpAfwR0yF6eBjpNYogVoz6wYS');

-- Permissions for BANKA
INSERT INTO customer_permissions (customer_id, permission)
VALUES (7, 'READ_EMPLOYEE');

-- Customer: DRZAVA (email: drzavadrzava@drzava1.com, password: nemanjanemanja)
INSERT INTO customer (id, first_name, last_name, email, username, phone_number,
                      birth_date, gender, address, salt_password, password)
VALUES (8, 'Država', 'Država', 'drzavadrzava@drzava1.com', 'drzavadrzava',
        '+381640000001', '2025-01-01', 'FEMALE', 'Bulevar Država 1',
        'salt', '$2a$12$NcVWqq8vsDVDBLezBJAchepu9WXMUpAfwR0yF6eBjpNYogVoz6wYS');

-- Permissions for DRZAVA
INSERT INTO customer_permissions (customer_id, permission)
VALUES (8, 'READ_EMPLOYEE');

-- ============== Data Loaded ==============

-- Employee 6: Milica password: Jovan@12345
INSERT INTO employee (id, first_name, last_name, email, username, phone_number,
                      birth_date, address, gender, position, department, active,
                      is_admin, salt_password, password)
VALUES (6, 'Milica', 'Jovanović', 'milica.jovanovic2@banka.com', 'milicaaaa2',
        '+381641001001', '2000-10-10', 'Knez Mihailova 6', 'FEMALE', 'WORKER',
        'AGENT', TRUE, FALSE, 'salt', '$2a$12$c2Y5721b8h0B0olC.xrQ.eHw2UR.67NAsDvyEkAbRo/a2dW5Tr.ge');