-- Migration script để thêm hỗ trợ OAuth2 cho bảng account
-- Chạy script này để cập nhật database schema

USE openevent;

-- Cập nhật cột password_hash để cho phép NULL (cho OAuth users)
ALTER TABLE account 
MODIFY COLUMN password_hash VARCHAR(255) NULL;

-- Thêm cột oauth_provider để lưu provider (GOOGLE, FACEBOOK, etc.)
ALTER TABLE account 
ADD COLUMN oauth_provider VARCHAR(50) NULL;

-- Thêm cột oauth_provider_id để lưu ID từ OAuth provider
ALTER TABLE account 
ADD COLUMN oauth_provider_id VARCHAR(255) NULL;

-- Tạo index cho OAuth provider ID để tìm kiếm nhanh hơn
CREATE INDEX idx_account_oauth_provider_id ON account(oauth_provider_id);

-- Tạo composite index cho provider và provider_id
CREATE INDEX idx_account_oauth_provider ON account(oauth_provider, oauth_provider_id);

