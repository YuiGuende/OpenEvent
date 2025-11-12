-- Script to fix duplicate constraint names
-- Run this BEFORE running schema.sql if you get duplicate constraint errors

-- Drop old constraint if exists (for customer table)
SET @constraint_exists = (
    SELECT COUNT(*) 
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = DATABASE() 
    AND CONSTRAINT_NAME = 'fk_user_account' 
    AND TABLE_NAME = 'customer'
);

SET @sql = IF(@constraint_exists > 0,
    'ALTER TABLE customer DROP FOREIGN KEY fk_user_account',
    'SELECT "Constraint fk_user_account does not exist in customer table"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop old constraint if exists (for customer table - fk_user_org)
SET @constraint_exists = (
    SELECT COUNT(*) 
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = DATABASE() 
    AND CONSTRAINT_NAME = 'fk_user_org' 
    AND TABLE_NAME = 'customer'
);

SET @sql = IF(@constraint_exists > 0,
    'ALTER TABLE customer DROP FOREIGN KEY fk_user_org',
    'SELECT "Constraint fk_user_org does not exist in customer table"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Constraint cleanup completed. You can now run schema.sql';

