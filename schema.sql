CREATE DATABASE IF NOT EXISTS openevent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE openevent;

-- 1. Bảng gốc
CREATE TABLE account
(
    account_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN','HOST','USER') NOT NULL
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
    website     VARCHAR(200)
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

CREATE TABLE user
(
    user_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(100),
    organization_id BIGINT,
    phone_number    VARCHAR(20),
    points          INT NOT NULL,
    account_id      BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_user_account FOREIGN KEY (account_id) REFERENCES account (account_id),
    CONSTRAINT fk_user_org FOREIGN KEY (organization_id) REFERENCES organization (org_id)
);

-- 3. Event và các bảng liên quan
CREATE TABLE event
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type       VARCHAR(31)  NOT NULL,
    event_title      VARCHAR(150) NOT NULL,
    description      TEXT,
    starts_at        DATETIME(6) NOT NULL,
    ends_at          DATETIME(6) NOT NULL,
    enroll_deadline  DATETIME(6) NOT NULL,
    status           ENUM('CANCEL','DRAFT','FINISH','ONGOING','PUBLIC') NOT NULL,
    image_url        VARCHAR(255),
    points           INT DEFAULT 0,
    benefits         TEXT,
    learning_objects TEXT,
    
    -- Thời gian thay đổi trạng thái
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    draft_at         DATETIME(6),
    public_date      DATETIME(6),
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
    genre           VARCHAR(100),
    performer_count INT,
    CONSTRAINT fk_music_event FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE
);

CREATE TABLE workshop_event
(
    event_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
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

-- ticket_type table đã được thay thế bằng trường ticket_type_name trong bảng tickets
-- Điều này đơn giản hóa cấu trúc và loại bỏ sự phức tạp không cần thiết

-- 4. Host và Guest (mối quan hệ User-Event)
CREATE TABLE host
(
    host_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at  DATETIME(6) NOT NULL,
    organize_id BIGINT,
    event_id    BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    CONSTRAINT fk_host_event FOREIGN KEY (event_id) REFERENCES event (id),
    CONSTRAINT fk_host_user FOREIGN KEY (user_id) REFERENCES user (user_id),
    CONSTRAINT fk_host_org FOREIGN KEY (organize_id) REFERENCES organization (org_id)
);

-- Bảng event_guests: mối quan hệ User tham gia Event (Guest)
CREATE TABLE event_guests
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    status ENUM('ACTIVE', 'LEFT', 'REMOVED') NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_eventguest_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_eventguest_event FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE,
    UNIQUE KEY UK_user_event_guest (user_id, event_id)
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

-- 5. Ticket và các bảng liên quan (Đã được thay thế bằng bảng tickets mới)
-- Các bảng cũ: ticket, ticket_type đã được thay thế bằng bảng tickets với trường ticket_type_name

-- 6. Payment and Tickets (PayOS Integration)
-- Bảng tickets thay thế cho bảng orders cũ, tích hợp thông tin vé trực tiếp
-- price: Giá vé (chỉ có 1 giá duy nhất)
-- ticket_type_name: Tên loại vé (thay thế cho bảng ticket_type riêng biệt)
-- Constraint: 1 user chỉ được đặt 1 vé cho mỗi event (UNIQUE KEY UK_user_event)
CREATE TABLE tickets
(
    ticket_id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    event_id                BIGINT NOT NULL,
    ticket_code             VARCHAR(50) NOT NULL,
    price                   DECIMAL(10,2) NOT NULL,
    status                  ENUM('PENDING','PAID','CANCELLED','REFUNDED','EXPIRED') NOT NULL DEFAULT 'PENDING',
    description             VARCHAR(500) DEFAULT NULL,
    participant_name        VARCHAR(100) DEFAULT NULL,
    participant_email       VARCHAR(100) DEFAULT NULL,
    participant_phone       VARCHAR(20) DEFAULT NULL,
    participant_organization VARCHAR(150) DEFAULT NULL,
    notes                   VARCHAR(1000) DEFAULT NULL,
    ticket_type_name        VARCHAR(255) DEFAULT NULL,
    purchase_date           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ticket_event FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE KEY UK_ticket_code (ticket_code),
    UNIQUE KEY UK_user_event (user_id, event_id)
);

-- Bảng payments liên kết với tickets thay vì orders
-- Mỗi ticket chỉ có một payment tương ứng (1:1 relationship)
-- amount trong payments = price trong tickets (đồng bộ giá trị)
CREATE TABLE payments
(
    payment_id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id               BIGINT NOT NULL,
    payos_payment_id        BIGINT DEFAULT NULL,
    payment_link_id         VARCHAR(100) DEFAULT NULL,
    checkout_url            VARCHAR(500) DEFAULT NULL,
    qr_code                 VARCHAR(500) DEFAULT NULL,
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
    CONSTRAINT fk_payment_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (ticket_id) ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE KEY UK_payment_ticket (ticket_id)
);

-- Migration completed: Order -> Ticket refactor

-- 7. Reports / Notifications / Requests
CREATE TABLE reports
(
    report_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    event_id   BIGINT NOT NULL,
    content    TEXT   NOT NULL,
    type       ENUM('SPAM','ABUSE','OTHER') NOT NULL,
    status     ENUM('PENDING','SEEN','RESOLVED') DEFAULT 'PENDING',
    seen       BOOLEAN  DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_user FOREIGN KEY (user_id) REFERENCES user (user_id),
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
    type            ENUM('REPORT','HOST_REQUEST','NOTIFICATION','USER_NOTIFICATION') NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES user (user_id),
    CONSTRAINT fk_notification_sender FOREIGN KEY (sender_id) REFERENCES user (user_id)
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

-- 8. Indexes for Ticket and Payment Tables (Performance Optimization)
CREATE INDEX idx_tickets_user_status ON tickets (user_id, status);
CREATE INDEX idx_tickets_event_status ON tickets (event_id, status);
CREATE INDEX idx_tickets_status ON tickets (status);
CREATE INDEX idx_tickets_purchase_date ON tickets (purchase_date);
CREATE INDEX idx_tickets_ticket_code ON tickets (ticket_code);
-- Index cho constraint UK_user_event đã được tự động tạo

-- Indexes for Event Guests (Performance Optimization)
CREATE INDEX idx_eventguests_user_id ON event_guests (user_id);
CREATE INDEX idx_eventguests_event_id ON event_guests (event_id);
CREATE INDEX idx_eventguests_status ON event_guests (status);
CREATE INDEX idx_eventguests_joined_at ON event_guests (joined_at);
-- Index cho constraint UK_user_event_guest đã được tự động tạo

CREATE INDEX idx_payments_ticket_id ON payments (ticket_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_payos_id ON payments (payos_payment_id);
CREATE INDEX idx_payments_link_id ON payments (payment_link_id);
CREATE INDEX idx_payments_expired_at ON payments (expired_at);
CREATE INDEX idx_payments_status_created ON payments (status, created_at);

-- 10. Indexes for Event Images Table (Performance Optimization)
CREATE INDEX idx_eventimage_event_id ON event_image (event_id);
CREATE INDEX idx_eventimage_main_poster ON event_image (event_id, main_poster);
