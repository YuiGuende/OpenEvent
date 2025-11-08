-- Migration script to add User table and refactor role entities
-- This script migrates from Account -> Role entities to Account -> User -> Role entities

-- Step 1: Create user table
CREATE TABLE IF NOT EXISTS `user` (
    `user_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `account_id` BIGINT NOT NULL UNIQUE,
    `name` VARCHAR(100),
    `phone_number` VARCHAR(20),
    `avatar` VARCHAR(500),
    `date_of_birth` DATETIME(6),
    `address` VARCHAR(500),
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT `fk_user_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`account_id`) ON DELETE CASCADE,
    INDEX `idx_user_account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 2: Migrate data from Customer/Admin/Department to User
-- For each account that has a Customer, Admin, or Department, create a User record

-- Insert Users from Customer table
INSERT INTO `user` (`account_id`, `name`, `phone_number`, `created_at`, `updated_at`)
SELECT 
    c.`account_id`,
    COALESCE(c.`name`, a.`email`) as `name`,
    c.`phone_number`,
    NOW() as `created_at`,
    NOW() as `updated_at`
FROM `customer` c
INNER JOIN `account` a ON c.`account_id` = a.`account_id`
WHERE NOT EXISTS (
    SELECT 1 FROM `user` u WHERE u.`account_id` = c.`account_id`
);

-- Insert Users from Admin table (if not already exists)
INSERT INTO `user` (`account_id`, `name`, `phone_number`, `created_at`, `updated_at`)
SELECT 
    ad.`account_id`,
    COALESCE(ad.`name`, a.`email`) as `name`,
    ad.`phone_number`,
    NOW() as `created_at`,
    NOW() as `updated_at`
FROM `admin` ad
INNER JOIN `account` a ON ad.`account_id` = a.`account_id`
WHERE NOT EXISTS (
    SELECT 1 FROM `user` u WHERE u.`account_id` = ad.`account_id`
);

-- Insert Users from Department table (if not already exists)
INSERT INTO `user` (`account_id`, `name`, `phone_number`, `created_at`, `updated_at`)
SELECT 
    d.`account_id`,
    COALESCE(d.`department_name`, a.`email`) as `name`,
    NULL as `phone_number`,
    NOW() as `created_at`,
    NOW() as `updated_at`
FROM `department` d
INNER JOIN `account` a ON d.`account_id` = a.`account_id`
WHERE NOT EXISTS (
    SELECT 1 FROM `user` u WHERE u.`account_id` = d.`account_id`
);

-- Step 3: Add user_id column to customer table
ALTER TABLE `customer`
ADD COLUMN `user_id` BIGINT NULL AFTER `customer_id`,
ADD INDEX `idx_customer_user_id` (`user_id`);

-- Step 4: Migrate user_id to customer table
UPDATE `customer` c
INNER JOIN `user` u ON c.`account_id` = u.`account_id`
SET c.`user_id` = u.`user_id`;

-- Step 5: Make user_id NOT NULL and add foreign key
ALTER TABLE `customer`
MODIFY COLUMN `user_id` BIGINT NOT NULL,
ADD CONSTRAINT `fk_customer_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE;

-- Step 6: Drop old account_id foreign key from customer (keep column for now for backward compatibility)
-- ALTER TABLE `customer` DROP FOREIGN KEY `fk_user_account`; -- Commented out to keep for migration period

-- Step 7: Add user_id column to host table
ALTER TABLE `host`
ADD COLUMN `user_id` BIGINT NULL AFTER `host_id`,
ADD INDEX `idx_host_user_id` (`user_id`);

-- Step 8: Migrate user_id to host table from customer
UPDATE `host` h
INNER JOIN `customer` c ON h.`customer_id` = c.`customer_id`
INNER JOIN `user` u ON c.`user_id` = u.`user_id`
SET h.`user_id` = u.`user_id`;

-- Step 9: Make user_id NOT NULL and add foreign key for host
ALTER TABLE `host`
MODIFY COLUMN `user_id` BIGINT NOT NULL,
ADD CONSTRAINT `fk_host_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE;

-- Step 10: Make customer_id nullable in host (since it's now optional)
ALTER TABLE `host`
MODIFY COLUMN `customer_id` BIGINT NULL;

-- Step 11: Add user_id column to admin table
ALTER TABLE `admin`
ADD COLUMN `user_id` BIGINT NULL AFTER `admin_id`,
ADD INDEX `idx_admin_user_id` (`user_id`);

-- Step 12: Migrate user_id to admin table
UPDATE `admin` ad
INNER JOIN `user` u ON ad.`account_id` = u.`account_id`
SET ad.`user_id` = u.`user_id`;

-- Step 13: Make user_id NOT NULL and add foreign key for admin
ALTER TABLE `admin`
MODIFY COLUMN `user_id` BIGINT NOT NULL,
ADD CONSTRAINT `fk_admin_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
DROP FOREIGN KEY `fk_admin_account`; -- Remove old FK

-- Step 14: Add user_id column to department table
ALTER TABLE `department`
ADD COLUMN `user_id` BIGINT NULL AFTER `account_id`,
ADD INDEX `idx_department_user_id` (`user_id`);

-- Step 15: Migrate user_id to department table
UPDATE `department` d
INNER JOIN `user` u ON d.`account_id` = u.`account_id`
SET d.`user_id` = u.`user_id`;

-- Step 16: Make user_id the primary key for department (since it uses @MapsId)
-- First, drop the old primary key constraint
ALTER TABLE `department`
DROP PRIMARY KEY;

-- Set user_id as primary key
ALTER TABLE `department`
MODIFY COLUMN `user_id` BIGINT NOT NULL PRIMARY KEY,
ADD CONSTRAINT `fk_department_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE;

-- Drop old account_id column from department (after migration period, you can drop this)
-- ALTER TABLE `department` DROP COLUMN `account_id`; -- Commented out for safety

-- Step 17: Remove phone_number from account table (moved to user)
-- ALTER TABLE `account` DROP COLUMN `phone_number`; -- Commented out for safety, do this after migration

-- Step 18: Add indexes for performance
CREATE INDEX `idx_user_created_at` ON `user` (`created_at`);
CREATE INDEX `idx_user_phone_number` ON `user` (`phone_number`);

-- Step 19: Remove duplicate fields from customer table (name, email, phone_number should be in user)
-- These can be dropped later after confirming everything works
-- ALTER TABLE `customer` DROP COLUMN `name`; -- Commented out for safety
-- ALTER TABLE `customer` DROP COLUMN `email`; -- Commented out for safety
-- ALTER TABLE `customer` DROP COLUMN `phone_number`; -- Commented out for safety

SELECT 'Migration completed successfully!' as status;

