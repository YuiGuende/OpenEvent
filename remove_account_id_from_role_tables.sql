-- Migration script to remove account_id columns from role tables
-- This script removes the redundant account_id columns since Account relationship
-- is already maintained through User entity (user.account_id)

USE openevent;

-- Remove account_id from admin table
ALTER TABLE admin 
DROP FOREIGN KEY IF EXISTS fk_admin_account;

ALTER TABLE admin 
DROP COLUMN IF EXISTS account_id;

-- Remove account_id from customer table
ALTER TABLE customer 
DROP FOREIGN KEY IF EXISTS fk_customer_account;

ALTER TABLE customer 
DROP COLUMN IF EXISTS account_id;

-- Remove account_id from department table
ALTER TABLE department 
DROP FOREIGN KEY IF EXISTS fk_department_account;

ALTER TABLE department 
DROP COLUMN IF EXISTS account_id;

