CREATE DATABASE IF NOT EXISTS openevent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE openevent;

-- 1. Bảng gốc
CREATE TABLE account
(
    account_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN','CUSTOMER','HOST') NOT NULL
);

CREATE TABLE organization
(
    org_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    address     VARCHAR(300),
    created_at  DATETIME(6) NOT NULL,
    description VARCHAR(1000),
    email       VARCHAR(100),
    org_name    VARCHAR(150) NOT NULL,
    phone       VARCHAR(20),
    updated_at  DATETIME(6),
    website     VARCHAR(200),
    representative_id BIGINT
);

CREATE TABLE place
(
    place_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    building   ENUM('ALPHA','BETA','NONE') NOT NULL,
    place_name VARCHAR(150) NOT NULL
);

CREATE TABLE speaker
(
    speaker_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    default_role ENUM('ARTIST','MC','OTHER','PERFORMER','SINGER','SPEAKER') NOT NULL,
    image_url    VARCHAR(255),
    name         VARCHAR(100) NOT NULL,
    profile      VARCHAR(255)
);

CREATE TABLE subscription_plans
(
    plan_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)   NOT NULL,
    price           DECIMAL(10, 2) NOT NULL,
    duration_months INT            NOT NULL
);

-- 2. Bảng phụ thuộc account / organization
CREATE TABLE admin
(
    admin_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    email        VARCHAR(100),
    name         VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20),
    account_id   BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_admin_account FOREIGN KEY (account_id) REFERENCES account (account_id)
);

CREATE TABLE customer
(
    customer_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(100),
    organization_id BIGINT,
    phone_number    VARCHAR(20),
    points          INT NOT NULL,
    account_id      BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_user_account FOREIGN KEY (account_id) REFERENCES account (account_id),
    CONSTRAINT fk_user_org FOREIGN KEY (organization_id) REFERENCES organization (org_id)
);

-- Add foreign key constraint for organization representative
ALTER TABLE organization ADD CONSTRAINT fk_org_customer FOREIGN KEY (representative_id) REFERENCES customer (customer_id);

-- User Sessions Table
CREATE TABLE user_sessions
(
    session_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_token    VARCHAR(255) NOT NULL UNIQUE,
    account_id       BIGINT NOT NULL,
    ip_address       VARCHAR(45),
    user_agent       VARCHAR(500),
    created_at       DATETIME(6) NOT NULL,
    last_accessed_at DATETIME(6) NOT NULL,
    expires_at       DATETIME(6) NOT NULL,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    device_info      VARCHAR(200),
    CONSTRAINT fk_session_account FOREIGN KEY (account_id) REFERENCES account (account_id) ON DELETE CASCADE
);

-- 3. Event và các bảng liên quan
CREATE TABLE event
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    version          BIGINT DEFAULT 0,
    event_type       VARCHAR(31)  NOT NULL,
    event_title      VARCHAR(150) NOT NULL,
    description      TEXT,
    image_url        LONGTEXT,
    capacity         INT,
    public_date      DATETIME(6),
    starts_at        DATETIME(6) NOT NULL,
    ends_at          DATETIME(6) NOT NULL,
    enroll_deadline  DATETIME(6) NOT NULL,
    status           ENUM('CANCEL','DRAFT','FINISH','ONGOING','PUBLIC') NOT NULL,
    points           INT DEFAULT 0,
    benefits         TEXT,
    learning_objects TEXT,
    poster           BOOLEAN DEFAULT FALSE,
    host_id          BIGINT,
    org_id           BIGINT,
    
    -- Thời gian thay đổi trạng thái
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    draft_at         DATETIME(6),
    public_at        DATETIME(6),
    ongoing_at       DATETIME(6),
    finish_at        DATETIME(6),
    cancel_at        DATETIME(6),
    
    parent_event_id  BIGINT,
    CONSTRAINT fk_event_parent FOREIGN KEY (parent_event_id) REFERENCES event (id)
);

CREATE TABLE event_sequence
(
    next_val BIGINT
);

CREATE TABLE event_place
(
    event_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    CONSTRAINT fk_eventplace_event FOREIGN KEY (event_id) REFERENCES event (id),
    CONSTRAINT fk_eventplace_place FOREIGN KEY (place_id) REFERENCES place (place_id)
);

CREATE TABLE event_schedule
(
    schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity    VARCHAR(255),
    end_time    DATETIME(6),
    start_time  DATETIME(6),
    event_id    BIGINT NOT NULL,
    CONSTRAINT fk_schedule_event FOREIGN KEY (event_id) REFERENCES event (id)
);

-- Event-specific tables
CREATE TABLE music_event
(
    event_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    music_type      VARCHAR(100),
    genre           VARCHAR(100),
    performer_count INT,
    CONSTRAINT fk_music_event FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE
);

CREATE TABLE workshop_event
(
    event_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    materials_link    VARCHAR(500),
    max_participants  INT,
    skill_level       VARCHAR(50),
    prerequisites     TEXT,
    CONSTRAINT fk_workshop_event FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE
);

CREATE TABLE competition_event
(
    event_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    prize_pool       VARCHAR(255),
    competition_type VARCHAR(255),
    rules            TEXT,
    CONSTRAINT fk_competition_event FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE
);

CREATE TABLE conference_event
(
    event_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    conference_type  VARCHAR(100),
    max_attendees    INT,
    agenda           TEXT,
    CONSTRAINT fk_conference_event FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE
);

CREATE TABLE festival_event
(
    event_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    culture          VARCHAR(100),
    traditions       TEXT,
    activities       TEXT,
    CONSTRAINT fk_festival_event FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE
);

CREATE TABLE event_speaker
(
    event_id   BIGINT NOT NULL,
    speaker_id BIGINT NOT NULL,
    role       ENUM('ARTIST','MC','OTHER','PERFORMER','SINGER','SPEAKER') NOT NULL DEFAULT 'SPEAKER',
    CONSTRAINT fk_eventspeaker_event FOREIGN KEY (event_id) REFERENCES event (id),
    CONSTRAINT fk_eventspeaker_speaker FOREIGN KEY (speaker_id) REFERENCES speaker (speaker_id)
);

CREATE TABLE event_image
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id    BIGINT NOT NULL,
    url         VARCHAR(255) NOT NULL,
    order_index INT DEFAULT 1,
    main_poster BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_eventimage_event FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE
);

-- 4.1. Ticket Types (Host tạo loại vé cho Event)
CREATE TABLE ticket_type
(
    ticket_type_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id         BIGINT NOT NULL,
    name             VARCHAR(100) NOT NULL,
    description      VARCHAR(1000) DEFAULT NULL,
    price            DECIMAL(38,2) NOT NULL,
    total_quantity   INT NOT NULL,
    sold_quantity    INT NOT NULL DEFAULT 0,
    sale             DECIMAL(38,2) DEFAULT 0,
    start_sale_date  DATETIME(6) DEFAULT NULL,
    end_sale_date    DATETIME(6) DEFAULT NULL,
    CONSTRAINT fk_tickettype_event FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE
);

-- 4. Host và Guest (mối quan hệ User-Event)
CREATE TABLE host
(
    host_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at  DATETIME(6) NOT NULL,
    organize_id BIGINT,
    customer_id BIGINT NOT NULL,
    host_discount_percent DECIMAL(5,2) DEFAULT 0.00,
    CONSTRAINT fk_host_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_host_org FOREIGN KEY (organize_id) REFERENCES organization (org_id)
);

-- Add foreign key constraints for event table after related tables are created
ALTER TABLE event ADD CONSTRAINT fk_event_host FOREIGN KEY (host_id) REFERENCES host (host_id);
ALTER TABLE event ADD CONSTRAINT fk_event_org FOREIGN KEY (org_id) REFERENCES organization (org_id);

-- Bảng event_guests: mối quan hệ User tham gia Event (Guest)
CREATE TABLE event_guests
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    status ENUM('ACTIVE', 'LEFT', 'REMOVED') NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_eventguest_user FOREIGN KEY (customer_id) REFERENCES customer (customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_eventguest_event FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE,
    UNIQUE KEY UK_user_event_guest (customer_id, event_id)
);

CREATE TABLE host_subscriptions
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    host_id    BIGINT NOT NULL,
    plan_id    BIGINT NOT NULL,
    start_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    end_date   DATETIME,
    CONSTRAINT fk_sub_host FOREIGN KEY (host_id) REFERENCES host (host_id),
    CONSTRAINT fk_sub_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans (plan_id)
);

-- 5. Voucher System (Discount Codes) - Must be created before orders
CREATE TABLE vouchers
(
    voucher_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    code              VARCHAR(20) NOT NULL UNIQUE,
    discount_amount   DECIMAL(10,2) NOT NULL,
    quantity          INT NOT NULL DEFAULT 1,
    status            ENUM('ACTIVE','EXPIRED','DISABLED') NOT NULL DEFAULT 'ACTIVE',
    created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at        DATETIME(6) DEFAULT NULL,
    description       VARCHAR(500) DEFAULT NULL,
    created_by        BIGINT DEFAULT NULL,
    CONSTRAINT fk_voucher_creator FOREIGN KEY (created_by) REFERENCES account (account_id) ON DELETE SET NULL
);

-- 6. Order System (Simplified Model - No OrderItem)
CREATE TABLE orders
(
    order_id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id              BIGINT NOT NULL,
    event_id                 BIGINT NOT NULL,
    ticket_type_id           BIGINT NOT NULL,
    status                   ENUM('PENDING','CONFIRMED','PAID','CANCELLED','EXPIRED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    
    -- Pricing fields
    original_price           DECIMAL(10,2) NOT NULL,
    host_discount_percent    DECIMAL(5,2) DEFAULT 0,
    host_discount_amount     DECIMAL(10,2) DEFAULT 0,
    voucher_discount_amount  DECIMAL(10,2) DEFAULT 0,
    total_amount             DECIMAL(10,2) NOT NULL DEFAULT 0,
    
    -- Voucher information
    voucher_id               BIGINT DEFAULT NULL,
    voucher_code             VARCHAR(20) DEFAULT NULL,
    
    participant_name         VARCHAR(100) DEFAULT NULL,
    participant_email        VARCHAR(100) DEFAULT NULL,
    participant_phone        VARCHAR(20) DEFAULT NULL,
    participant_organization VARCHAR(150) DEFAULT NULL,
    notes                    VARCHAR(1000) DEFAULT NULL,
    created_at               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at               DATETIME(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_order_user FOREIGN KEY (customer_id) REFERENCES customer (customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_event FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_tickettype FOREIGN KEY (ticket_type_id) REFERENCES ticket_type (ticket_type_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers (voucher_id) ON DELETE SET NULL
);

-- Voucher Usage Tracking
CREATE TABLE voucher_usage
(
    usage_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    voucher_id        BIGINT NOT NULL,
    order_id          BIGINT NOT NULL,
    used_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    discount_applied  DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_voucherusage_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers (voucher_id) ON DELETE CASCADE,
    CONSTRAINT fk_voucherusage_order FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE,
    UNIQUE KEY UK_voucher_order (voucher_id, order_id)
);

-- 7. Payment System (PayOS Integration)
-- Simplified payment system using only Orders (no OrderItem or Ticket)

-- Bảng payments chỉ hỗ trợ Order-based flow với PayOS
-- order_id: Liên kết với orders
-- payos_payment_id: ID thanh toán từ PayOS
-- transaction_id: Mã giao dịch từ PayOS
CREATE TABLE payments
(
    payment_id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id                BIGINT NOT NULL,
    payos_payment_id        BIGINT DEFAULT NULL,
    payment_link_id         VARCHAR(100) DEFAULT NULL,
    checkout_url            VARCHAR(500) DEFAULT NULL,
    qr_code                 VARCHAR(500) DEFAULT NULL,
    transaction_id          VARCHAR(255) DEFAULT NULL,
    amount                  DECIMAL(10,2) NOT NULL,
    currency                VARCHAR(3) NOT NULL DEFAULT 'VND',
    status                  ENUM('PENDING','PAID','CANCELLED','EXPIRED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    description             VARCHAR(500) DEFAULT NULL,
    return_url              VARCHAR(500) DEFAULT NULL,
    cancel_url              VARCHAR(500) DEFAULT NULL,
    expired_at              DATETIME(6) DEFAULT NULL,
    paid_at                 DATETIME(6) DEFAULT NULL,
    cancelled_at            DATETIME(6) DEFAULT NULL,
    payos_signature         VARCHAR(500) DEFAULT NULL,
    created_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE KEY UK_payment_order (order_id)
);

-- Migration completed: Simplified Order system (no OrderItem, no Ticket)

-- 7. Reports / Notifications / Requests
CREATE TABLE reports
(
    report_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    event_id   BIGINT NOT NULL,
    content    TEXT   NOT NULL,
    type       ENUM('SPAM','ABUSE','OTHER') NOT NULL,
    status     ENUM('PENDING','SEEN','RESOLVED') DEFAULT 'PENDING',
    seen       BOOLEAN  DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_user FOREIGN KEY (customer_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_report_event FOREIGN KEY (event_id) REFERENCES event (id)
);

CREATE TABLE notifications
(
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receiver_id     BIGINT NOT NULL,
    sender_id       BIGINT,
    message         VARCHAR(500) NOT NULL,
    is_read         BOOLEAN  DEFAULT FALSE,
    target_url      VARCHAR(255),
    type            ENUM('REPORT','HOST_REQUEST','NOTIFICATION','CUSTOMER_NOTIFICATION') NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES customer (customer_id),
    CONSTRAINT fk_notification_sender FOREIGN KEY (sender_id) REFERENCES customer (customer_id)
);

CREATE TABLE requests
(
    request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    host_id    BIGINT NOT NULL,
    event_id   BIGINT NOT NULL,
    type       ENUM('EVENT_APPROVAL','OTHER') NOT NULL,
    status     ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_host FOREIGN KEY (host_id) REFERENCES host (host_id),
    CONSTRAINT fk_request_event FOREIGN KEY (event_id) REFERENCES event (id)
);

-- 8. Indexes for New Order System (Performance Optimization)
CREATE INDEX idx_tickettype_event_id ON ticket_type (event_id);
CREATE INDEX idx_tickettype_sale_dates ON ticket_type (start_sale_date, end_sale_date);
CREATE INDEX idx_tickettype_quantity ON ticket_type (total_quantity, sold_quantity);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_event_id ON orders (event_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at);
CREATE INDEX idx_orders_customer_status ON orders (customer_id, status);
CREATE INDEX idx_orders_tickettype_id ON orders (ticket_type_id);
CREATE INDEX idx_orders_customer_tickettype ON orders (customer_id, ticket_type_id);
CREATE INDEX idx_orders_event_tickettype ON orders (event_id, ticket_type_id);

-- 9. Indexes for Payment Tables (Performance Optimization)

-- Indexes for Event Guests (Performance Optimization)
CREATE INDEX idx_eventguests_customer_id ON event_guests (customer_id);
CREATE INDEX idx_eventguests_event_id ON event_guests (event_id);
CREATE INDEX idx_eventguests_status ON event_guests (status);
CREATE INDEX idx_eventguests_joined_at ON event_guests (joined_at);
-- Index cho constraint UK_user_event_guest đã được tự động tạo

CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_payos_id ON payments (payos_payment_id);
CREATE INDEX idx_payments_link_id ON payments (payment_link_id);
CREATE INDEX idx_payments_expired_at ON payments (expired_at);
CREATE INDEX idx_payments_status_created ON payments (status, created_at);
CREATE INDEX idx_payments_transaction_id ON payments (transaction_id);

-- 10. Indexes for Event Images Table (Performance Optimization)
CREATE INDEX idx_eventimage_event_id ON event_image (event_id);
CREATE INDEX idx_eventimage_main_poster ON event_image (event_id, main_poster);

-- 11. Indexes for User Sessions Table (Performance Optimization)
CREATE INDEX idx_user_sessions_token ON user_sessions (session_token);
CREATE INDEX idx_user_sessions_account_id ON user_sessions (account_id);
CREATE INDEX idx_user_sessions_active ON user_sessions (is_active, expires_at);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions (expires_at);
CREATE INDEX idx_user_sessions_ip_address ON user_sessions (ip_address);
CREATE INDEX idx_user_sessions_last_accessed ON user_sessions (last_accessed_at);

-- 12. Indexes for Voucher System (Performance Optimization)
CREATE INDEX idx_vouchers_code ON vouchers (code);
CREATE INDEX idx_vouchers_status ON vouchers (status);
CREATE INDEX idx_vouchers_expires_at ON vouchers (expires_at);
CREATE INDEX idx_vouchers_status_expires ON vouchers (status, expires_at);
CREATE INDEX idx_vouchers_created_by ON vouchers (created_by);

CREATE INDEX idx_voucher_usage_voucher_id ON voucher_usage (voucher_id);
CREATE INDEX idx_voucher_usage_order_id ON voucher_usage (order_id);
CREATE INDEX idx_voucher_usage_used_at ON voucher_usage (used_at);

-- 13. Indexes for Updated Orders Table (Performance Optimization)
CREATE INDEX idx_orders_voucher_id ON orders (voucher_id);
CREATE INDEX idx_orders_voucher_code ON orders (voucher_code);
CREATE INDEX idx_orders_original_price ON orders (original_price);
CREATE INDEX idx_orders_total_amount ON orders (total_amount);

-- 11. INSERT SAMPLE DATA
-- Sample accounts
INSERT INTO account (email, password_hash, role) VALUES
('admin@openevent.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN'),
('host1@openevent.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'HOST'),
('host2@openevent.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'HOST'),
('user1@openevent.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'CUSTOMER'),
('user2@openevent.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'CUSTOMER'),
('user3@openevent.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'CUSTOMER');

-- Sample organizations
INSERT INTO organization (org_name, description, email, phone, address, created_at) VALUES
('HCMC University', 'Ho Chi Minh City University of Technology', 'contact@hcmut.edu.vn', '028-38647256', '268 Ly Thuong Kiet, District 10, HCMC', NOW()),
('Tech Hub Vietnam', 'Technology and Innovation Hub', 'info@techhub.vn', '028-12345678', '123 Nguyen Hue, District 1, HCMC', NOW()),
('Creative Arts Center', 'Center for Creative Arts and Culture', 'hello@creativecenter.vn', '028-87654321', '456 Le Loi, District 3, HCMC', NOW());

-- Sample places
INSERT INTO place (building, place_name) VALUES
('ALPHA', 'Alpha Auditorium'),
('ALPHA', 'Alpha Conference Room A'),
('ALPHA', 'Alpha Conference Room B'),
('BETA', 'Beta Main Hall'),
('BETA', 'Beta Meeting Room 1'),
('BETA', 'Beta Meeting Room 2'),
('NONE', 'Online Platform'),
('NONE', 'Outdoor Stage');

-- Sample speakers
INSERT INTO speaker (name, default_role, profile, image_url) VALUES
('Dr. Nguyen Van A', 'SPEAKER', 'Expert in AI and Machine Learning', 'https://via.placeholder.com/150'),
('Ms. Tran Thi B', 'SPEAKER', 'Digital Marketing Specialist', 'https://via.placeholder.com/150'),
('Mr. Le Van C', 'PERFORMER', 'Professional Musician and Composer', 'https://via.placeholder.com/150'),
('DJ Mike', 'ARTIST', 'Electronic Music Producer', 'https://via.placeholder.com/150'),
('Chef Anna', 'OTHER', 'Culinary Expert and Food Blogger', 'https://via.placeholder.com/150');

-- Sample admin
INSERT INTO admin (name, email, phone_number, account_id) VALUES
('System Admin', 'admin@openevent.com', '0901234567', 1);

-- Sample users
INSERT INTO customer (email, phone_number, points, account_id, organization_id) VALUES
('host1@openevent.com', '0901234568', 100, 2, 1),
('host2@openevent.com', '0901234569', 150, 3, 2),
('user1@openevent.com', '0901234570', 50, 4, 1),
('user2@openevent.com', '0901234571', 75, 5, NULL),
('user3@openevent.com', '0901234572', 25, 6, 3);

-- Sample events
INSERT INTO event (event_type, event_title, description, starts_at, ends_at, enroll_deadline, status, image_url, points, benefits, learning_objects, poster, org_id, created_at, public_at) VALUES
('MUSIC', 'Spring Music Festival 2024', 'Annual spring music festival featuring local and international artists', '2024-04-15 18:00:00', '2024-04-15 23:00:00', '2024-04-10 23:59:59', 'PUBLIC', 'https://via.placeholder.com/400x300', 20, 'Free drinks and snacks, Meet & greet with artists', 'Experience diverse music genres, Network with music enthusiasts', TRUE, 3, NOW(), NOW()),
('WORKSHOP', 'AI & Machine Learning Workshop', 'Hands-on workshop covering basics of AI and ML with practical examples', '2024-04-20 09:00:00', '2024-04-20 17:00:00', '2024-04-18 23:59:59', 'PUBLIC', 'https://via.placeholder.com/400x300', 50, 'Certificate of completion, Course materials, Lunch included', 'Learn AI fundamentals, Build your first ML model, Understand data preprocessing', FALSE, 1, NOW(), NOW()),
('OTHERS', 'Digital Marketing Summit 2024', 'Premier conference for digital marketing professionals and enthusiasts', '2024-05-05 08:00:00', '2024-05-05 18:00:00', '2024-05-01 23:59:59', 'PUBLIC', 'https://via.placeholder.com/400x300', 30, 'Networking opportunities, Conference materials, Refreshments', 'Latest marketing trends, Social media strategies, ROI optimization techniques', FALSE, 2, NOW(), NOW()),
('COMPETITION', 'Coding Challenge 2024', 'Annual programming competition for students and professionals', '2024-05-12 10:00:00', '2024-05-12 16:00:00', '2024-05-08 23:59:59', 'PUBLIC', 'https://via.placeholder.com/400x300', 40, 'Cash prizes, Certificates, Job opportunities', 'Problem-solving skills, Algorithm optimization, Team collaboration', FALSE, 1, NOW(), NOW()),
('MUSIC', 'Jazz Night at the Park', 'Relaxing evening of jazz music in outdoor setting', '2024-04-25 19:00:00', '2024-04-25 22:00:00', '2024-04-23 23:59:59', 'PUBLIC', 'https://via.placeholder.com/400x300', 15, 'Outdoor seating, Light refreshments', 'Appreciate jazz music, Relaxing atmosphere', FALSE, 3, NOW(), NOW()),
('FESTIVAL', 'Cultural Festival 2024', 'Multi-day cultural festival celebrating local traditions and arts', '2024-06-01 10:00:00', '2024-06-03 22:00:00', '2024-05-28 23:59:59', 'PUBLIC', 'https://via.placeholder.com/400x300', 35, 'Cultural experiences, Traditional food, Art exhibitions', 'Learn about local culture, Traditional crafts, Community engagement', FALSE, 3, NOW(), NOW());

-- Sample event-specific details
INSERT INTO music_event (event_id, music_type, genre, performer_count) VALUES
(1, 'Live Concert', 'Pop, Rock, Electronic', 8),
(5, 'Jazz Performance', 'Jazz, Blues', 4);

INSERT INTO workshop_event (event_id, materials_link, max_participants, skill_level, prerequisites) VALUES
(2, 'https://workshop-materials.com/ai-ml-2024', 50, 'Beginner to Intermediate', 'Basic programming knowledge preferred');

INSERT INTO conference_event (event_id, conference_type, max_attendees, agenda) VALUES
(3, 'Professional Development', 200, 'Keynote speeches, Panel discussions, Networking sessions');

INSERT INTO competition_event (event_id, prize_pool, competition_type, rules) VALUES
(4, '10,000,000 VND', 'Individual and Team', 'Max 4 hours, Any programming language allowed');

INSERT INTO festival_event (event_id, culture, traditions, activities) VALUES
(6, 'Vietnamese Culture', 'Traditional Vietnamese customs and heritage', 'Cultural performances, Traditional food, Art workshops, Community games');

-- Sample event places
INSERT INTO event_place (event_id, place_id) VALUES
(1, 8), -- Spring Music Festival - Outdoor Stage
(2, 1), -- AI Workshop - Alpha Auditorium  
(3, 4), -- Digital Marketing Summit - Beta Main Hall
(4, 2), -- Coding Challenge - Alpha Conference Room A
(5, 8), -- Jazz Night - Outdoor Stage
(6, 4); -- Cultural Festival - Beta Main Hall

-- Sample event speakers
INSERT INTO event_speaker (event_id, speaker_id, role) VALUES
(1, 3, 'PERFORMER'),
(1, 4, 'ARTIST'),
(2, 1, 'SPEAKER'),
(3, 2, 'SPEAKER'),
(5, 3, 'PERFORMER'),
(6, 5, 'OTHER'); -- Cultural Festival - Chef Anna as cultural expert

-- Sample event images
INSERT INTO event_image (event_id, url, order_index, main_poster) VALUES
(1, 'https://via.placeholder.com/800x600/FF6B6B/FFFFFF?text=Spring+Music+Festival', 1, TRUE),
(1, 'https://via.placeholder.com/800x600/4ECDC4/FFFFFF?text=Stage+Setup', 2, FALSE),
(2, 'https://via.placeholder.com/800x600/45B7D1/FFFFFF?text=AI+Workshop', 1, TRUE),
(3, 'https://via.placeholder.com/800x600/96CEB4/FFFFFF?text=Marketing+Summit', 1, TRUE),
(4, 'https://via.placeholder.com/800x600/FFEAA7/000000?text=Coding+Challenge', 1, TRUE),
(5, 'https://via.placeholder.com/800x600/DDA0DD/000000?text=Jazz+Night', 1, TRUE),
(6, 'https://via.placeholder.com/800x600/F39C12/FFFFFF?text=Cultural+Festival', 1, TRUE);

-- Sample ticket types (Updated with current dates for testing)
INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
-- Spring Music Festival
(1, 'Early Bird', 'Early bird discount ticket', 10000.00, 100, 25, 200.00, '2024-10-01 00:00:00', '2024-12-31 23:59:59'),
(1, 'General Admission', 'Standard admission ticket', 36000.00, 200, 45, 0.00, '2024-10-01 00:00:00', '2024-12-31 23:59:59'),
(1, 'VIP Pass', 'VIP access with premium benefits', 100000.00, 50, 12, 0.00, '2024-10-01 00:00:00', '2024-12-31 23:59:59'),

-- AI Workshop
(2, 'Student Ticket', 'Discounted price for students', 1000.00, 30, 8, 100.00, '2024-10-01 00:00:00', '2024-12-31 23:59:59'),
(2, 'Professional Ticket', 'Regular price for professionals', 3500.00, 20, 5, 0.00, '2024-10-01 00:00:00', '2024-12-31 23:59:59'),

-- Digital Marketing Summit
(3, 'Standard Pass', 'Access to all sessions', 2500.00, 150, 32, 0.00, '2024-10-01 00:00:00', '2024-12-31 23:59:59'),
(3, 'Premium Pass', 'All sessions + networking dinner', 4000.00, 50, 18, 0.00, '2024-10-01 00:00:00', '2024-12-31 23:59:59'),

-- Coding Challenge
(4, 'Individual Entry', 'Single participant entry', 1500.00, 80, 15, 0.00, '2024-10-01 00:00:00', '2024-12-31 23:59:59'),
(4, 'Team Entry', 'Team of up to 4 members', 4500.00, 20, 6, 0.00, '2024-10-01 00:00:00', '2024-12-31 23:59:59'),

-- Jazz Night
(5, 'General Seating', 'Standard outdoor seating', 1800.00, 120, 28, 0.00, '2024-10-01 00:00:00', '2024-12-31 23:59:59'),
(5, 'Premium Seating', 'Front row seating with table service', 3200.00, 40, 12, 0.00, '2024-10-01 00:00:00', '2024-12-31 23:59:59'),

-- Cultural Festival
(6, 'Day Pass', 'Single day access to festival', 1500.00, 200, 45, 0.00, '2024-10-01 00:00:00', '2024-12-31 23:59:59'),
(6, 'Full Festival Pass', 'Access to all 3 days', 4000.00, 100, 22, 0.00, '2024-10-01 00:00:00', '2024-12-31 23:59:59');

-- Sample hosts
INSERT INTO host (created_at, customer_id, organize_id, host_discount_percent) VALUES
(NOW(), 1, 3, 10.00), -- Host1 for Creative Arts Center with 10% discount
(NOW(), 1, 1, 5.00), -- Host1 for HCMC University with 5% discount
(NOW(), 2, 2, 15.00), -- Host2 for Tech Hub Vietnam with 15% discount
(NOW(), 1, 1, 0.00), -- Host1 for HCMC University with no discount
(NOW(), 2, 3, 8.00), -- Host2 for Creative Arts Center with 8% discount
(NOW(), 2, 3, 12.00); -- Host2 for Creative Arts Center with 12% discount

-- Update event table to set host_id after hosts are created
UPDATE event SET host_id = 1 WHERE id = 1; -- Spring Music Festival -> Host1
UPDATE event SET host_id = 2 WHERE id = 2; -- AI Workshop -> Host1  
UPDATE event SET host_id = 3 WHERE id = 3; -- Marketing Summit -> Host2
UPDATE event SET host_id = 4 WHERE id = 4; -- Coding Challenge -> Host1
UPDATE event SET host_id = 5 WHERE id = 5; -- Jazz Night -> Host2
UPDATE event SET host_id = 6 WHERE id = 6; -- Cultural Festival -> Host2

-- Sample event guests (some users already joined events)
INSERT INTO event_guests (customer_id, event_id, joined_at, status) VALUES
(3, 1, '2024-03-15 10:30:00', 'ACTIVE'), -- user1 joined Spring Music Festival
(4, 1, '2024-03-16 14:20:00', 'ACTIVE'), -- user2 joined Spring Music Festival  
(5, 2, '2024-03-20 09:15:00', 'ACTIVE'), -- user3 joined AI Workshop
(3, 3, '2024-04-02 16:45:00', 'ACTIVE'), -- user1 joined Marketing Summit
(4, 5, '2024-04-05 11:30:00', 'ACTIVE'); -- user2 joined Jazz Night

-- Sample orders (some completed purchases) - Updated with new pricing fields
INSERT INTO orders (customer_id, event_id, ticket_type_id, status, original_price, host_discount_percent, host_discount_amount, voucher_discount_amount, total_amount, participant_name, participant_email, participant_phone, created_at) VALUES
(3, 1, 1, 'PAID', 200000.00, 0.00, 0.00, 0.00, 200000.00, 'Nguyen Van User1', 'user1@openevent.com', '0901234570', '2024-03-15 10:30:00'),
(4, 1, 2, 'PAID', 300000.00, 0.00, 0.00, 0.00, 300000.00, 'Tran Thi User2', 'user2@openevent.com', '0901234571', '2024-03-16 14:20:00'),
(5, 2, 1, 'PAID', 100000.00, 0.00, 0.00, 0.00, 100000.00, 'Le Van User3', 'user3@openevent.com', '0901234572', '2024-03-20 09:15:00'),
(3, 3, 1, 'CONFIRMED', 250000.00, 0.00, 0.00, 0.00, 250000.00, 'Nguyen Van User1', 'user1@openevent.com', '0901234570', '2024-04-02 16:45:00'),
(4, 5, 1, 'PENDING', 180000.00, 0.00, 0.00, 0.00, 180000.00, 'Tran Thi User2', 'user2@openevent.com', '0901234571', '2024-04-05 11:30:00');

-- Sample payments
INSERT INTO payments (order_id, amount, status, description, created_at, paid_at) VALUES
(1, 150000.00, 'PAID', 'Payment for Spring Music Festival - Early Bird', '2024-03-15 10:35:00', '2024-03-15 10:36:00'),
(2, 200000.00, 'PAID', 'Payment for Spring Music Festival - General Admission', '2024-03-16 14:25:00', '2024-03-16 14:26:00'),
(3, 100000.00, 'PAID', 'Payment for AI Workshop - Student Ticket', '2024-03-20 09:20:00', '2024-03-20 09:21:00'),
(4, 250000.00, 'PENDING', 'Payment for Digital Marketing Summit - Standard Pass', '2024-04-02 16:50:00', NULL),
(5, 80000.00, 'PENDING', 'Payment for Jazz Night - General Seating', '2024-04-05 11:35:00', NULL);

-- Sample subscription plans
INSERT INTO subscription_plans (name, price, duration_months) VALUES
('Basic Plan', 99000.00, 1),
('Pro Plan', 299000.00, 3),
('Premium Plan', 999000.00, 12);

-- Sample host subscriptions
INSERT INTO host_subscriptions (host_id, plan_id, start_date, end_date) VALUES
(1, 2, '2024-01-01 00:00:00', '2024-04-01 00:00:00'),
(2, 3, '2024-02-01 00:00:00', '2025-02-01 00:00:00');

-- Initialize event sequence
INSERT INTO event_sequence (next_val) VALUES (7);

-- Sample reports
INSERT INTO reports (customer_id, event_id, content, type, status, created_at) VALUES
(4, 1, 'The sound system was too loud during the first hour', 'OTHER', 'RESOLVED', '2024-03-16 20:30:00'),
(5, 2, 'Workshop materials were very helpful', 'OTHER', 'SEEN', '2024-03-20 18:45:00');

-- Sample notifications
INSERT INTO notifications (receiver_id, sender_id, message, type, is_read, created_at) VALUES
(3, NULL, 'Welcome to OpenEvent! Start exploring amazing events.', 'CUSTOMER_NOTIFICATION', TRUE, '2024-03-01 10:00:00'),
(4, NULL, 'Your payment for Spring Music Festival has been confirmed.', 'CUSTOMER_NOTIFICATION', TRUE, '2024-03-16 14:26:00'),
(5, NULL, 'Reminder: AI Workshop starts tomorrow at 9:00 AM.', 'CUSTOMER_NOTIFICATION', FALSE, '2024-04-19 18:00:00'),
(1, NULL, 'Your event "Spring Music Festival" has received 50+ registrations!', 'HOST_REQUEST', FALSE, '2024-03-20 15:30:00');

-- Sample vouchers (Discount Codes)
INSERT INTO vouchers (code, discount_amount, quantity, status, description, created_at, expires_at, created_by) VALUES
('WELCOME50', 50000.00, 100, 'ACTIVE', 'Welcome discount 50k for new users', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1),
('SAVE20K', 20000.00, 50, 'ACTIVE', 'Save 20k on any event', NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 1),
('FESTIVAL100', 100000.00, 20, 'ACTIVE', 'Special festival discount 100k', NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1),
('STUDENT30', 30000.00, 200, 'ACTIVE', 'Student discount 30k', NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), 1),
('EARLYBIRD', 15000.00, 75, 'ACTIVE', 'Early bird discount 15k', NOW(), DATE_ADD(NOW(), INTERVAL 45 DAY), 1),
('VIP200', 200000.00, 10, 'ACTIVE', 'VIP discount 200k for premium events', NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 1),
('EXPIRED10', 10000.00, 5, 'EXPIRED', 'Expired test voucher', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 1),
('UNLIMITED', 25000.00, 999, 'ACTIVE', 'Unlimited use voucher 25k (no expiry)', NOW(), NULL, 1);

-- Sample orders with voucher usage (Updated with new pricing fields)
INSERT INTO orders (customer_id, event_id, ticket_type_id, status, original_price, host_discount_percent, host_discount_amount, voucher_discount_amount, total_amount, voucher_id, voucher_code, participant_name, participant_email, participant_phone, created_at) VALUES
-- Order with voucher discount
(3, 1, 1, 'PENDING', 200000.00, 0.00, 0.00, 50000.00, 150000.00, 1, 'WELCOME50', 'Nguyen Van User1', 'user1@openevent.com', '0901234570', NOW()),
-- Order with host discount
(4, 2, 1, 'PENDING', 100000.00, 10.00, 10000.00, 0.00, 90000.00, NULL, NULL, 'Tran Thi User2', 'user2@openevent.com', '0901234571', NOW()),
-- Order with both host and voucher discount
(5, 3, 1, 'PENDING', 250000.00, 15.00, 37500.00, 20000.00, 192500.00, 2, 'SAVE20K', 'Le Van User3', 'user3@openevent.com', '0901234572', NOW()),
-- Order without any discount
(3, 4, 1, 'PENDING', 150000.00, 0.00, 0.00, 0.00, 150000.00, NULL, NULL, 'Nguyen Van User1', 'user1@openevent.com', '0901234570', NOW());

-- Sample voucher usage records
INSERT INTO voucher_usage (voucher_id, order_id, discount_applied, used_at) VALUES
(1, 6, 50000.00, NOW()),
(2, 8, 20000.00, NOW());

SELECT 'Sample data inserted successfully!' as status;
