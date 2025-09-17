-- =========================================
-- openEvent - Full SQL Schema (SQL Server)
-- =========================================

IF DB_ID('open_event') IS NULL
    CREATE DATABASE open_event;
GO
USE open_event;
GO

-- =============== RESET (optional) ===============
IF OBJECT_ID('speaker_image', 'U') IS NOT NULL DROP TABLE speaker_image;
IF OBJECT_ID('event_image', 'U') IS NOT NULL DROP TABLE event_image;
IF OBJECT_ID('image', 'U') IS NOT NULL DROP TABLE image;
IF OBJECT_ID('subscriber', 'U') IS NOT NULL DROP TABLE subscriber;
IF OBJECT_ID('subscription_plan', 'U') IS NOT NULL DROP TABLE subscription_plan;
IF OBJECT_ID('checkout', 'U') IS NOT NULL DROP TABLE checkout;
IF OBJECT_ID('announcement', 'U') IS NOT NULL DROP TABLE announcement;
IF OBJECT_ID('speaker', 'U') IS NOT NULL DROP TABLE speaker;
IF OBJECT_ID('organization_member', 'U') IS NOT NULL DROP TABLE organization_member;
IF OBJECT_ID('organization', 'U') IS NOT NULL DROP TABLE organization;
IF OBJECT_ID('ticket', 'U') IS NOT NULL DROP TABLE ticket;
IF OBJECT_ID('guest', 'U') IS NOT NULL DROP TABLE guest;
IF OBJECT_ID('host', 'U') IS NOT NULL DROP TABLE host;
IF OBJECT_ID('place', 'U') IS NOT NULL DROP TABLE place;
IF OBJECT_ID('festival_event', 'U') IS NOT NULL DROP TABLE festival_event;
IF OBJECT_ID('competition_event', 'U') IS NOT NULL DROP TABLE competition_event;
IF OBJECT_ID('workshop_event', 'U') IS NOT NULL DROP TABLE workshop_event;
IF OBJECT_ID('music_event', 'U') IS NOT NULL DROP TABLE music_event;
IF OBJECT_ID('event', 'U') IS NOT NULL DROP TABLE event;
IF OBJECT_ID('[user]', 'U') IS NOT NULL DROP TABLE [user];
IF OBJECT_ID('admin', 'U') IS NOT NULL DROP TABLE admin;
IF OBJECT_ID('account', 'U') IS NOT NULL DROP TABLE account;

-- =============== 1) Account & Profiles ===============
CREATE TABLE account (
  account_id    INT IDENTITY(1,1) PRIMARY KEY,
  email         VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role          VARCHAR(20) NOT NULL
      CHECK (role IN ('ADMIN','USER','SPONSOR','TEACHER'))
);

CREATE TABLE admin (
  admin_id     INT IDENTITY(1,1) PRIMARY KEY,
  account_id   INT NOT NULL UNIQUE,
  name         VARCHAR(50) NOT NULL,
  phone_number VARCHAR(20),
  email        VARCHAR(100),
  CONSTRAINT fk_admin_account FOREIGN KEY (account_id)
      REFERENCES account(account_id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE [user] (
  user_id      INT IDENTITY(1,1) PRIMARY KEY,
  account_id   INT NOT NULL UNIQUE,
  phone_number VARCHAR(20),
  organization VARCHAR(150),
  email        VARCHAR(100),
  points       INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_user_account FOREIGN KEY (account_id)
      REFERENCES account(account_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- =============== 2) Event & Place ===============
CREATE TABLE event (
  event_id        INT IDENTITY(1,1) PRIMARY KEY,
  parent_event_id INT NULL,
  event_title     VARCHAR(150) NOT NULL,
  image_url       VARCHAR(255),
  description     VARCHAR(MAX),
  public_date     DATETIME,
  event_type      VARCHAR(50) NOT NULL DEFAULT 'OTHERS'
      CHECK (event_type IN ('MUSIC','WORKSHOP','CONFERENCE','COMPETITION','FESTIVAL','OTHERS')),
  enroll_deadline DATETIME NOT NULL,
  starts_at       DATETIME NOT NULL,
  ends_at         DATETIME NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT GETDATE(),
  status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
      CHECK (status IN ('PUBLIC','DRAFT','ONGOING','CANCEL','FINISH')),
  benefits        VARCHAR(MAX),
  learning_objects VARCHAR(MAX),
  points          INT,
  CONSTRAINT fk_event_parent FOREIGN KEY (parent_event_id)
    REFERENCES event(event_id)
    ON DELETE NO ACTION 
    ON UPDATE NO ACTION,
  CONSTRAINT chk_event_time_order_1 CHECK (enroll_deadline <= starts_at),
  CONSTRAINT chk_event_time_order_2 CHECK (starts_at < ends_at)
);
CREATE TABLE event_schedule (
  schedule_id INT IDENTITY(1,1) PRIMARY KEY,
  event_id    INT NOT NULL,
  activity    VARCHAR(255) NOT NULL,
  start_time  DATETIME NOT NULL,
  end_time    DATETIME NOT NULL,
  CONSTRAINT fk_schedule_event FOREIGN KEY (event_id)
      REFERENCES event(event_id) ON DELETE CASCADE
);

CREATE TABLE place (
  place_id   INT IDENTITY(1,1) PRIMARY KEY,
  event_id   INT NOT NULL,
  building   VARCHAR(20) NOT NULL DEFAULT 'NONE'
      CHECK (building IN ('ALPHA','BETA','NONE')),
  place_name VARCHAR(150) NOT NULL,
  CONSTRAINT uq_place_event_name UNIQUE (event_id, place_name),
  CONSTRAINT fk_place_event FOREIGN KEY (event_id)
      REFERENCES event(event_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Specialized Event Tables
CREATE TABLE music_event (
  event_id   INT PRIMARY KEY,
  artist     VARCHAR(255) NOT NULL,
  genre      VARCHAR(100),
  stage_name VARCHAR(150),
  FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE
);

CREATE TABLE workshop_event (
  event_id       INT PRIMARY KEY,
  speaker        VARCHAR(255) NOT NULL,
  topic          VARCHAR(255),
  materials_link VARCHAR(255),
  FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE
);

CREATE TABLE competition_event (
  event_id         INT PRIMARY KEY,
  competition_type VARCHAR(100),
  rules            VARCHAR(MAX),
  prize_pool       VARCHAR(255),
  FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE
);

CREATE TABLE festival_event (
  event_id   INT PRIMARY KEY,
  culture    VARCHAR(100),
  highlight  VARCHAR(MAX),
  FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE
);

-- =============== 3) Organization & Membership ===============
CREATE TABLE organization (
  org_id         INT IDENTITY(1,1) PRIMARY KEY,
  org_name       VARCHAR(150) NOT NULL UNIQUE,
  leader_user_id INT NOT NULL,
  member_amount  INT NOT NULL DEFAULT 1,
  CONSTRAINT fk_org_leader_user FOREIGN KEY (leader_user_id)
      REFERENCES [user](user_id) ON DELETE NO ACTION ON UPDATE CASCADE
);

CREATE TABLE organization_member (
  org_id    INT NOT NULL,
  user_id   INT NOT NULL,
  joined_at DATETIME NOT NULL DEFAULT GETDATE(),
  PRIMARY KEY (org_id, user_id),
  CONSTRAINT fk_org_member_org FOREIGN KEY (org_id)
      REFERENCES organization(org_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_org_member_user FOREIGN KEY (user_id)
      REFERENCES [user](user_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- =============== 4) Host & Guest ===============
CREATE TABLE host (
  host_id     INT IDENTITY(1,1) PRIMARY KEY,
  user_id     INT NOT NULL,
  event_id    INT NOT NULL,
  organize_id INT NULL,
  created_at  DATETIME NOT NULL DEFAULT GETDATE(),
  CONSTRAINT uq_host_user_event UNIQUE (user_id, event_id),
  CONSTRAINT fk_host_user FOREIGN KEY (user_id)
      REFERENCES [user](user_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_host_event FOREIGN KEY (event_id)
      REFERENCES event(event_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_host_organization FOREIGN KEY (organize_id)
      REFERENCES organization(org_id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE guest (
  guest_id   INT IDENTITY(1,1) PRIMARY KEY,
  user_id    INT NOT NULL,
  event_id   INT NOT NULL,
  join_at    DATETIME NOT NULL DEFAULT GETDATE(),
  CONSTRAINT uq_guest_user_event UNIQUE (user_id, event_id),
  CONSTRAINT fk_guest_user FOREIGN KEY (user_id)
      REFERENCES [user](user_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_guest_event FOREIGN KEY (event_id)
      REFERENCES event(event_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- =============== 5) Ticket ===============
CREATE TABLE ticket (
  ticket_id  INT IDENTITY(1,1) PRIMARY KEY,
  guest_id   INT NOT NULL UNIQUE,
  event_id   INT NOT NULL,
  place_id   INT NOT NULL UNIQUE,
  type       VARCHAR(20) NOT NULL DEFAULT 'NORMAL'
      CHECK (type IN ('NORMAL','VJP')),
  price      DECIMAL(10,2) NOT NULL DEFAULT 0,
  status     VARCHAR(20) NOT NULL DEFAULT 'ISSUED'
      CHECK (status IN ('ISSUED','CANCELLED','USED')),
  created_at DATETIME NOT NULL DEFAULT GETDATE(),
  CONSTRAINT fk_ticket_guest FOREIGN KEY (guest_id)
      REFERENCES guest(guest_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_ticket_event FOREIGN KEY (event_id)
      REFERENCES event(event_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_ticket_place FOREIGN KEY (place_id)
      REFERENCES place(place_id) ON DELETE NO ACTION ON UPDATE CASCADE
);

-- =============== 6) Speaker ===============

CREATE TABLE event_speaker (
    event_speaker_id INT IDENTITY(1,1) PRIMARY KEY,
    event_id   INT NOT NULL,
    speaker_id INT NOT NULL,
    role       VARCHAR(50) NOT NULL,
    note       VARCHAR(255),
    FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE,
    FOREIGN KEY (speaker_id) REFERENCES speaker(speaker_id) ON DELETE CASCADE
);
CREATE TABLE speaker (
    speaker_id INT IDENTITY(1,1) PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    image_url  VARCHAR(255),
	profile  VARCHAR(255),
    default_role VARCHAR(50) NOT NULL DEFAULT 'SPEAKER'
);

-- =============== 7) Announcement ===============
CREATE TABLE announcement (
  announcement_id INT IDENTITY(1,1) PRIMARY KEY,
  event_id        INT NOT NULL,
  host_id         INT NULL,
  message         VARCHAR(MAX) NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT GETDATE(),
  CONSTRAINT fk_announcement_event FOREIGN KEY (event_id)
      REFERENCES event(event_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_announcement_host FOREIGN KEY (host_id)
      REFERENCES host(host_id) ON DELETE SET NULL ON UPDATE CASCADE
);

-- =============== 8) Checkout / Points ===============
CREATE TABLE checkout (
  checkout_id INT IDENTITY(1,1) PRIMARY KEY,
  user_id     INT NOT NULL,
  points_used INT NOT NULL DEFAULT 0,
  created_at  DATETIME NOT NULL DEFAULT GETDATE(),
  CONSTRAINT fk_checkout_user FOREIGN KEY (user_id)
      REFERENCES [user](user_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- =============== 9) Subscription Plans & Subscribers ===============
CREATE TABLE subscription_plan (
  plan_id       INT IDENTITY(1,1) PRIMARY KEY,
  name          VARCHAR(20) NOT NULL
      CHECK (name IN ('BASIC','PRO')),
  price         DECIMAL(10,2) NOT NULL DEFAULT 0,
  duration_month INT NOT NULL,
  status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
      CHECK (status IN ('ACTIVE','EXPIRED')),
  is_active     BIT NOT NULL DEFAULT 1,
  created_at    DATETIME NOT NULL DEFAULT GETDATE()
);

CREATE TABLE subscriber (
  subscriber_id INT IDENTITY(1,1) PRIMARY KEY,
  user_id       INT NOT NULL,
  plan_id       INT NOT NULL,
  created_at    DATETIME NOT NULL DEFAULT GETDATE(),
  CONSTRAINT fk_subscriber_user FOREIGN KEY (user_id)
      REFERENCES [user](user_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_subscriber_plan FOREIGN KEY (plan_id)
      REFERENCES subscription_plan(plan_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- =============== 10) Image system ===============
CREATE TABLE image (
  image_id   INT IDENTITY(1,1) PRIMARY KEY,
  url        VARCHAR(255) NOT NULL,
  caption    VARCHAR(200),
  created_at DATETIME NOT NULL DEFAULT GETDATE()
);

CREATE TABLE event_image (
  event_id   INT NOT NULL,
  image_id   INT NOT NULL,
  is_cover   BIT NOT NULL DEFAULT 0,
  sort_order INT NOT NULL DEFAULT 0,
  PRIMARY KEY (event_id, image_id),
  CONSTRAINT fk_event_image_event FOREIGN KEY (event_id)
      REFERENCES event(event_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_event_image_image FOREIGN KEY (image_id)
      REFERENCES image(image_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT uq_event_cover UNIQUE (event_id, is_cover)
);

CREATE TABLE speaker_image (
  speaker_id INT NOT NULL,
  image_id   INT NOT NULL,
  is_primary BIT NOT NULL DEFAULT 0,
  sort_order INT NOT NULL DEFAULT 0,
  PRIMARY KEY (speaker_id, image_id),
  CONSTRAINT fk_speaker_image_speaker FOREIGN KEY (speaker_id)
      REFERENCES speaker(speaker_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_speaker_image_image FOREIGN KEY (image_id)
      REFERENCES image(image_id) ON DELETE CASCADE ON UPDATE CASCADE
);
