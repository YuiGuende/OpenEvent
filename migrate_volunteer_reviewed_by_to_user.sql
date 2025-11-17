-- Migration script để chuyển volunteer_application.reviewed_by từ account_id sang user_id
-- Chạy script này để cập nhật database schema theo logic mới (account -> user)

USE openevent;

-- Bước 1: Xóa foreign key constraint cũ (nếu tồn tại)
-- Lưu ý: Nếu foreign key không tồn tại, bỏ qua lỗi
ALTER TABLE volunteer_application 
DROP FOREIGN KEY fk_volunteer_application_reviewer;

-- Bước 2: Thêm cột mới reviewed_by_user_id (tạm thời cho phép NULL)
-- Kiểm tra xem cột đã tồn tại chưa
SET @column_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'openevent' 
    AND TABLE_NAME = 'volunteer_application' 
    AND COLUMN_NAME = 'reviewed_by_user_id'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE volunteer_application ADD COLUMN reviewed_by_user_id BIGINT NULL AFTER reviewed_by_account_id',
    'SELECT "Column reviewed_by_user_id already exists, skipping..." AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Bước 3: Migrate dữ liệu từ account_id sang user_id
-- Tìm user_id tương ứng với account_id và cập nhật
UPDATE volunteer_application va
INNER JOIN `user` u ON u.account_id = va.reviewed_by_account_id
SET va.reviewed_by_user_id = u.user_id
WHERE va.reviewed_by_account_id IS NOT NULL;

-- Bước 4: Xóa cột cũ reviewed_by_account_id (nếu còn tồn tại)
SET @column_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'openevent' 
    AND TABLE_NAME = 'volunteer_application' 
    AND COLUMN_NAME = 'reviewed_by_account_id'
);

SET @sql = IF(@column_exists > 0,
    'ALTER TABLE volunteer_application DROP COLUMN reviewed_by_account_id',
    'SELECT "Column reviewed_by_account_id does not exist, skipping..." AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Bước 5: Thêm foreign key constraint mới (nếu chưa tồn tại)
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = 'openevent' 
    AND TABLE_NAME = 'volunteer_application' 
    AND CONSTRAINT_NAME = 'fk_volunteer_application_reviewer'
);

SET @sql = IF(@fk_exists = 0,
    'ALTER TABLE volunteer_application ADD CONSTRAINT fk_volunteer_application_reviewer FOREIGN KEY (reviewed_by_user_id) REFERENCES `user` (user_id) ON DELETE SET NULL',
    'SELECT "Foreign key fk_volunteer_application_reviewer already exists, skipping..." AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Bước 6: Tạo index để tối ưu query (nếu chưa tồn tại)
SET @index_exists = (
    SELECT COUNT(*) 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = 'openevent' 
    AND TABLE_NAME = 'volunteer_application' 
    AND INDEX_NAME = 'idx_volunteer_application_reviewer'
);

SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_volunteer_application_reviewer ON volunteer_application(reviewed_by_user_id)',
    'SELECT "Index idx_volunteer_application_reviewer already exists, skipping..." AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Kiểm tra kết quả
SELECT 
    'Migration completed successfully' AS status,
    COUNT(*) AS total_applications,
    COUNT(reviewed_by_user_id) AS applications_with_reviewer
FROM volunteer_application;

