CREATE TABLE account
(
    account_id    INT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN','HOST','USER') NOT NULL
);

CREATE TABLE admin
(
    admin_id     INT AUTO_INCREMENT PRIMARY KEY,
    email        VARCHAR(100),
    name         VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20),
    account_id   INT         NOT NULL UNIQUE,
    CONSTRAINT fk_admin_account FOREIGN KEY (account_id) REFERENCES account (account_id)
);

CREATE TABLE event
(
    id               INT PRIMARY KEY,
    event_type       VARCHAR(31)  NOT NULL,
    benefits         TEXT,
    created_at       DATETIME(6) NOT NULL,
    description      TEXT,
    ends_at          DATETIME(6) NOT NULL,
    enroll_deadline  DATETIME(6) NOT NULL,
    image_url        VARCHAR(255),
    learning_objects TEXT,
    points           INT,
    public_date      DATETIME(6),
    starts_at        DATETIME(6) NOT NULL,
    status           ENUM('CANCEL','DRAFT','FINISH','ONGOING','PUBLIC') NOT NULL,
    event_title      VARCHAR(150) NOT NULL,
    competition_type VARCHAR(255),
    prize_pool       VARCHAR(255),
    rules            TEXT,
    culture          VARCHAR(255),
    highlight        TEXT,
    materials_link   VARCHAR(255),
    topic            VARCHAR(255),
    parent_event_id  INT,
    CONSTRAINT fk_event_parent FOREIGN KEY (parent_event_id) REFERENCES event (id)
);

CREATE TABLE event_place
(
    event_id INT NOT NULL,
    place_id INT NOT NULL,
    CONSTRAINT fk_eventplace_event FOREIGN KEY (event_id) REFERENCES event (id),
    CONSTRAINT fk_eventplace_place FOREIGN KEY (place_id) REFERENCES place (place_id)
);

CREATE TABLE event_schedule
(
    schedule_id INT AUTO_INCREMENT PRIMARY KEY,
    activity    VARCHAR(255),
    end_time    DATETIME(6),
    start_time  DATETIME(6),
    event_id    INT NOT NULL,
    CONSTRAINT fk_schedule_event FOREIGN KEY (event_id) REFERENCES event (id)
);

CREATE TABLE event_sequence
(
    next_val BIGINT
);

CREATE TABLE event_speaker
(
    event_id   INT NOT NULL,
    speaker_id INT NOT NULL,
    CONSTRAINT fk_eventspeaker_event FOREIGN KEY (event_id) REFERENCES event (id),
    CONSTRAINT fk_eventspeaker_speaker FOREIGN KEY (speaker_id) REFERENCES speaker (speaker_id)
);

CREATE TABLE organization
(
    org_id      INT AUTO_INCREMENT PRIMARY KEY,
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
    place_id   INT AUTO_INCREMENT PRIMARY KEY,
    building   ENUM('ALPHA','BETA','NONE') NOT NULL,
    place_name VARCHAR(150) NOT NULL
);

CREATE TABLE speaker
(
    speaker_id   INT AUTO_INCREMENT PRIMARY KEY,
    default_role ENUM('ARTIST','MC','OTHER','PERFORMER','SINGER','SPEAKER') NOT NULL,
    image_url    VARCHAR(255),
    name         VARCHAR(100) NOT NULL,
    profile      VARCHAR(255)
);

CREATE TABLE ticket_type
(
    ticket_type_id INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255),
    price          DECIMAL(38, 2),
    total_quantity INT,
    event_id       INT NOT NULL,
    CONSTRAINT fk_tickettype_event FOREIGN KEY (event_id) REFERENCES event (id)
);

CREATE TABLE user
(
    user_id         INT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(100),
    organization_id BIGINT,
    phone_number    VARCHAR(20),
    points          INT NOT NULL,
    account_id      INT NOT NULL UNIQUE,
    CONSTRAINT fk_user_account FOREIGN KEY (account_id) REFERENCES account (account_id),
    CONSTRAINT fk_user_org FOREIGN KEY (organization_id) REFERENCES organization (org_id)
);

CREATE TABLE host
(
    host_id     INT AUTO_INCREMENT PRIMARY KEY,
    created_at  DATETIME(6) NOT NULL,
    organize_id INT,
    event_id    INT NOT NULL,
    user_id     INT NOT NULL,
    CONSTRAINT fk_host_event FOREIGN KEY (event_id) REFERENCES event (id),
    CONSTRAINT fk_host_user FOREIGN KEY (user_id) REFERENCES user (user_id),
    CONSTRAINT fk_host_org FOREIGN KEY (organize_id) REFERENCES organization (org_id)
);

CREATE TABLE ticket
(
    ticket_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_type_id BIGINT NOT NULL,
    user_id        BIGINT NOT NULL,
    purchase_date  DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES ticket_type (ticket_type_id),
    CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES user (user_id),
    UNIQUE KEY uq_user_ticket (user_id, ticket_type_id)
);

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
    receiver_id     BIGINT       NOT NULL,
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

CREATE TABLE subscription_plans
(
    plan_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)   NOT NULL,
    price           DECIMAL(10, 2) NOT NULL,
    duration_months INT            NOT NULL
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
