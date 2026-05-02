CREATE TABLE users(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255),
    first_name VARCHAR(255),
    last_name  VARCHAR(255)
);

CREATE TABLE phones
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT,
    phone_number VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE addresses
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id   BIGINT,
    user_city VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

INSERT INTO users (email, first_name, last_name) VALUES
('john.doe@example.com', 'John', 'Doe'),
('jane.smith@example.com', 'Jane', 'Smith'),
('bob.wilson@example.com', 'Bob', 'Wilson');

INSERT INTO phones (user_id, phone_number) VALUES
(1, '+1-555-0101'),
(1, '+1-555-0102'),
(2, '+1-555-0201'),
(3, '+1-555-0301');

INSERT INTO addresses (user_id, user_city) VALUES
(1, 'New York'),
(1, 'Los Angeles'),
(2, 'Chicago'),
(3, 'Houston');