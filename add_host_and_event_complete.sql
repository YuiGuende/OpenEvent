-- =============================================
-- SQL Script: Thêm Host và Event từ Account ID 6,7 (Hoàn chỉnh)
-- =============================================

-- BƯỚC 1: Cập nhật role của account 6,7 thành HOST
UPDATE account SET role = 'HOST' WHERE account_id IN (6, 7);

-- BƯỚC 2: Thêm thông tin customer cho account 6,7 (nếu chưa có)
INSERT IGNORE INTO customer (email, organization_id, phone_number, points, account_id) VALUES
('duy@gmail.com', 1, '0901234006', 0, 6),
('giao@gmail.com', 2, '0901234007', 0, 7);

-- BƯỚC 3: Tạo Host với ID cụ thể
INSERT INTO host (host_id, created_at, customer_id, organize_id, host_discount_percent) VALUES
(1, NOW(), (SELECT customer_id FROM customer WHERE account_id = 6), 1, 10.00), -- Host cho duy@gmail.com
(2, NOW(), (SELECT customer_id FROM customer WHERE account_id = 7), 2, 15.00); -- Host cho giao@gmail.com

-- BƯỚC 4: Thêm Event cho Host ID 1,2
-- Event cho Host ID 1 (duy@gmail.com)
INSERT INTO event (
    id,
    event_type, 
    event_title, 
    description, 
    image_url, 
    capacity, 
    public_date, 
    starts_at, 
    ends_at, 
    enroll_deadline, 
    status, 
    points, 
    benefits, 
    learning_objects, 
    poster, 
    host_id, 
    org_id,
    created_at
) VALUES (
    (SELECT COALESCE(MAX(id), 0) + 1 FROM event e),
    'MUSIC',
    'Summer Music Concert 2025',
    'Join us for an amazing summer music concert featuring local artists and bands. Enjoy live performances under the stars with food and drinks.',
    'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=800&h=600&fit=crop',
    300,
    '2025-04-01 10:00:00',
    '2025-06-15 19:00:00',
    '2025-06-15 23:00:00',
    '2025-06-10 23:59:59',
    'DRAFT',  -- Status ban đầu là DRAFT, chờ phê duyệt
    40,
    'Live music, food trucks, networking, summer vibes',
    'Music appreciation, cultural experience',
    TRUE,
    1,  -- Host ID 1
    1,
    NOW()
);

-- Thêm vào bảng music_event
INSERT INTO music_event (event_id, music_type, genre, performer_count) 
VALUES (LAST_INSERT_ID(), 'Concert', 'Pop, Rock, Indie', 12);

-- Event cho Host ID 2 (giao@gmail.com)
INSERT INTO event (
    id,
    event_type, 
    event_title, 
    description, 
    image_url, 
    capacity, 
    public_date, 
    starts_at, 
    ends_at, 
    enroll_deadline, 
    status, 
    points, 
    benefits, 
    learning_objects, 
    poster, 
    host_id, 
    org_id,
    created_at
) VALUES (
    (SELECT COALESCE(MAX(id), 0) + 1 FROM event e),
    'WORKSHOP',
    'Digital Marketing Masterclass',
    'Learn advanced digital marketing strategies, SEO, social media marketing, and analytics. Perfect for professionals looking to enhance their marketing skills.',
    'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=800&h=600&fit=crop',
    60,
    '2025-04-05 09:00:00',
    '2025-07-20 09:00:00',
    '2025-07-20 17:00:00',
    '2025-07-15 23:59:59',
    'DRAFT',  -- Status ban đầu là DRAFT, chờ phê duyệt
    80,
    'Certificate, lunch, networking, marketing toolkit',
    'Digital marketing, SEO, social media, analytics, content strategy',
    TRUE,
    2,  -- Host ID 2
    2,
    NOW()
);

-- Thêm vào bảng workshop_event
INSERT INTO workshop_event (event_id, materials_link, max_participants, skill_level) 
VALUES (LAST_INSERT_ID(), 'https://github.com/techhub-workshop/digital-marketing', 60, 'Intermediate');

-- BƯỚC 5: Thêm Ticket Types cho các events
-- Ticket types cho Summer Music Concert
SET @summer_concert_id = (SELECT id FROM event WHERE event_title = 'Summer Music Concert 2025' ORDER BY id DESC LIMIT 1);

INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
(@summer_concert_id, 'Early Bird', 'Early bird ticket with discount', 120000, 100, 25, 0, '2025-04-01 00:00:00', '2025-05-01 23:59:59'),
(@summer_concert_id, 'Regular', 'Regular admission ticket', 150000, 150, 45, 0, '2025-05-02 00:00:00', '2025-06-10 23:59:59'),
(@summer_concert_id, 'VIP', 'VIP ticket with premium seating', 250000, 50, 8, 0, '2025-04-01 00:00:00', '2025-06-10 23:59:59');

-- Ticket types cho Digital Marketing Masterclass
SET @marketing_workshop_id = (SELECT id FROM event WHERE event_title = 'Digital Marketing Masterclass' ORDER BY id DESC LIMIT 1);

INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
(@marketing_workshop_id, 'Student', 'Student discount ticket', 0, 20, 12, 0, '2025-04-05 00:00:00', '2025-07-15 23:59:59'),
(@marketing_workshop_id, 'Professional', 'Professional ticket', 600000, 40, 18, 0, '2025-04-05 00:00:00', '2025-07-15 23:59:59');

-- BƯỚC 6: Thêm Places cho các events
-- Thêm places nếu chưa có
INSERT IGNORE INTO place (building, place_name) VALUES
('OUTDOOR_STAGE', 'FPT University HCM - Outdoor Stage'),
('CONFERENCE_ROOM', 'Tech Hub Vietnam - Conference Room');

-- Liên kết events với places
INSERT INTO event_place (event_id, place_id) VALUES
(@summer_concert_id, (SELECT place_id FROM place WHERE place_name = 'FPT University HCM - Outdoor Stage')),
(@marketing_workshop_id, (SELECT place_id FROM place WHERE place_name = 'Tech Hub Vietnam - Conference Room'));

-- BƯỚC 7: Hiển thị kết quả
SELECT 
    a.account_id,
    a.email,
    a.role,
    c.phone_number,
    o.org_name as organization,
    h.host_discount_percent,
    h.host_id,
    COUNT(e.id) as events_count
FROM account a
LEFT JOIN customer c ON a.account_id = c.account_id
LEFT JOIN host h ON c.customer_id = h.customer_id
LEFT JOIN organization o ON h.organize_id = o.org_id
LEFT JOIN event e ON h.host_id = e.host_id
WHERE a.account_id IN (6, 7)
GROUP BY a.account_id, a.email, a.role, c.phone_number, o.org_name, h.host_discount_percent, h.host_id
ORDER BY a.account_id;

-- Hiển thị events mới tạo
SELECT 
    e.id,
    e.event_type,
    e.event_title,
    e.capacity,
    e.starts_at,
    e.status,
    p.place_name as venue,
    c.email as creator_email,
    h.host_discount_percent,
    COUNT(tt.ticket_type_id) as ticket_types_count
FROM event e
LEFT JOIN event_place ep ON e.id = ep.event_id
LEFT JOIN place p ON ep.place_id = p.place_id
LEFT JOIN host h ON e.host_id = h.host_id
LEFT JOIN customer c ON h.customer_id = c.customer_id
LEFT JOIN ticket_type tt ON e.id = tt.event_id
WHERE e.id >= (SELECT MAX(id) - 1 FROM event)
GROUP BY e.id, e.event_type, e.event_title, e.capacity, e.starts_at, e.status, p.place_name, c.email, h.host_discount_percent
ORDER BY e.id DESC;
