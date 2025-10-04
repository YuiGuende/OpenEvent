-- Quick setup script after database reset
-- Run this if you need minimal data to test

-- Ensure database exists
CREATE DATABASE IF NOT EXISTS openevent;
USE openevent;

-- Create minimal account table if not exists
CREATE TABLE IF NOT EXISTS account (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('USER', 'HOST', 'ADMIN') NOT NULL DEFAULT 'USER',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6)
);

-- Create minimal user table if not exists  
CREATE TABLE IF NOT EXISTS user (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    points INT DEFAULT 0,
    organization_id BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_user_account FOREIGN KEY (account_id) REFERENCES account (account_id) ON DELETE CASCADE
);

-- Insert admin account (password: 123456)
INSERT IGNORE INTO account (email, password_hash, role) VALUES 
('admin@gmail.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM5lE2cBFVZDJ.IbqyPO', 'ADMIN');

-- Check if data was inserted
SELECT 'Setup completed. Accounts in database:' as status;
SELECT account_id, email, role FROM account;





