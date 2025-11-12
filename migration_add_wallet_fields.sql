-- Migration: Add bank account and KYC fields to host_wallets table
-- Run this SQL script to update the database schema

ALTER TABLE host_wallets
ADD COLUMN IF NOT EXISTS bank_account_number VARCHAR(50) NULL,
ADD COLUMN IF NOT EXISTS bank_code VARCHAR(20) NULL,
ADD COLUMN IF NOT EXISTS account_holder_name VARCHAR(100) NULL,
ADD COLUMN IF NOT EXISTS kyc_verified BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS kyc_name VARCHAR(100) NULL;

-- Update existing wallets to have kyc_verified = false if NULL
UPDATE host_wallets
SET kyc_verified = FALSE
WHERE kyc_verified IS NULL;

