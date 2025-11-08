-- Create volunteer_application table
-- This table stores volunteer applications for events

CREATE TABLE IF NOT EXISTS volunteer_application (
    volunteer_application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    application_message TEXT,
    host_response TEXT,
    reviewed_by_account_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6),
    reviewed_at DATETIME(6) NULL,
    
    CONSTRAINT fk_volunteer_application_customer 
        FOREIGN KEY (customer_id) REFERENCES customer (customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_volunteer_application_event 
        FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE,
    CONSTRAINT fk_volunteer_application_reviewer 
        FOREIGN KEY (reviewed_by_account_id) REFERENCES account (account_id) ON DELETE SET NULL,
    
    -- Đảm bảo một customer chỉ có thể apply một lần cho một event
    UNIQUE KEY uk_customer_event (customer_id, event_id),
    
    -- Index để tìm nhanh applications theo event và status
    INDEX idx_event_status (event_id, status),
    INDEX idx_customer (customer_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

