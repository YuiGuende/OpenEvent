-- =============================================
-- SQL Script: Thêm Host và Event mẫu cho OpenEvent
-- =============================================

-- =============================================
-- BƯỚC 1: Thêm các tài khoản Account trước
-- =============================================

-- Thêm các account cho host
INSERT INTO account (email, password, role, created_at) VALUES
('host1@fpt.edu.vn', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', NOW()),
('host2@fpt.edu.vn', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', NOW()),
('host3@fpt.edu.vn', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', NOW()),
('host4@fpt.edu.vn', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', NOW()),
('host5@fpt.edu.vn', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', NOW()),
('host6@fpt.edu.vn', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER', NOW());

-- =============================================
-- BƯỚC 2: Thêm các Customer (thông tin cá nhân)
-- =============================================

-- Thêm customer cho các host
INSERT INTO customer (email, organization_id, phone_number, points, account_id) VALUES
('host1@fpt.edu.vn', 1, '0901234001', 0, (SELECT account_id FROM account WHERE email = 'host1@fpt.edu.vn')),
('host2@fpt.edu.vn', 1, '0901234002', 0, (SELECT account_id FROM account WHERE email = 'host2@fpt.edu.vn')),
('host3@fpt.edu.vn', 2, '0901234003', 0, (SELECT account_id FROM account WHERE email = 'host3@fpt.edu.vn')),
('host4@fpt.edu.vn', 2, '0901234004', 0, (SELECT account_id FROM account WHERE email = 'host4@fpt.edu.vn')),
('host5@fpt.edu.vn', 3, '0901234005', 0, (SELECT account_id FROM account WHERE email = 'host5@fpt.edu.vn')),
('host6@fpt.edu.vn', 3, '0901234006', 0, (SELECT account_id FROM account WHERE email = 'host6@fpt.edu.vn'));

-- =============================================
-- BƯỚC 3: Thêm các Host
-- =============================================

-- Thêm host với các tổ chức khác nhau
INSERT INTO host (created_at, customer_id, organize_id, host_discount_percent) VALUES
(NOW(), (SELECT customer_id FROM customer WHERE email = 'host1@fpt.edu.vn'), 1, 10.00), -- Host1 cho FPT University HCM
(NOW(), (SELECT customer_id FROM customer WHERE email = 'host2@fpt.edu.vn'), 1, 5.00),  -- Host2 cho FPT University HCM
(NOW(), (SELECT customer_id FROM customer WHERE email = 'host3@fpt.edu.vn'), 2, 15.00), -- Host3 cho Tech Hub Vietnam
(NOW(), (SELECT customer_id FROM customer WHERE email = 'host4@fpt.edu.vn'), 2, 8.00),  -- Host4 cho Tech Hub Vietnam
(NOW(), (SELECT customer_id FROM customer WHERE email = 'host5@fpt.edu.vn'), 3, 12.00), -- Host5 cho Creative Arts Center
(NOW(), (SELECT customer_id FROM customer WHERE email = 'host6@fpt.edu.vn'), 3, 0.00);  -- Host6 cho Creative Arts Center

-- =============================================
-- BƯỚC 4: Thêm các Event
-- =============================================

-- 1. Thêm Music Event
INSERT INTO event (
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
    created_at,
    public_at,
    venue_address
) VALUES (
    'MUSIC',
    'FPT Music Festival 2025',
    'Join us for an amazing music festival featuring top artists and bands. Experience live performances, food trucks, and unforgettable memories.',
    'https://cdn.pixabay.com/photo/2016/11/23/15/48/audience-1853662_1280.jpg',
    500,
    '2025-01-15 10:00:00',
    '2025-10-25 18:00:00',
    '2025-10-26 23:00:00',
    '2025-10-22 23:59:59',
    'PUBLIC',
    50,
    'Free gifts, networking, music vibes, food trucks',
    'Music appreciation, cultural experience',
    TRUE,
    (SELECT host_id FROM host WHERE customer_id = (SELECT customer_id FROM customer WHERE email = 'host1@fpt.edu.vn')),
    1,
    NOW(),
    NOW(),
    'FPT University Da Nang - Gamma Building'
);

-- Thêm vào bảng music_event
INSERT INTO music_event (event_id, music_type, genre, performer_count) 
VALUES (LAST_INSERT_ID(), 'Festival', 'Pop, Rock, Electronic', 15);

-- 2. Thêm Workshop Event
INSERT INTO event (
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
    created_at,
    public_at,
    venue_address
) VALUES (
    'WORKSHOP',
    'AI & Machine Learning Workshop',
    'Learn the fundamentals of Artificial Intelligence and Machine Learning. Hands-on experience with Python, TensorFlow, and real-world projects.',
    'https://images.unsplash.com/photo-1555949963-aa79dcee981c?w=800&h=600&fit=crop',
    100,
    '2025-01-20 09:00:00',
    '2025-11-15 09:00:00',
    '2025-11-15 17:00:00',
    '2025-11-10 23:59:59',
    'PUBLIC',
    100,
    'Certificate, lunch, networking, project materials',
    'AI fundamentals, ML algorithms, Python programming, TensorFlow',
    TRUE,
    (SELECT host_id FROM host WHERE customer_id = (SELECT customer_id FROM customer WHERE email = 'host2@fpt.edu.vn')),
    1,
    NOW(),
    NOW(),
    'FPT University HCM - Lab Building'
);

-- Thêm vào bảng workshop_event
INSERT INTO workshop_event (event_id, materials_link, max_participants, skill_level) 
VALUES (LAST_INSERT_ID(), 'https://github.com/fpt-workshop/ai-ml-materials', 100, 'Beginner');

-- 3. Thêm Competition Event
INSERT INTO event (
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
    created_at,
    public_at,
    venue_address
) VALUES (
    'COMPETITION',
    'FPT Coding Competition 2025',
    'Test your programming skills in our annual coding competition. Solve algorithmic problems, compete with peers, and win amazing prizes.',
    'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800&h=600&fit=crop',
    200,
    '2025-02-01 08:00:00',
    '2025-12-10 09:00:00',
    '2025-12-10 18:00:00',
    '2025-12-05 23:59:59',
    'PUBLIC',
    200,
    'Cash prizes, certificates, internship opportunities, networking',
    'Algorithm design, problem solving, competitive programming',
    TRUE,
    (SELECT host_id FROM host WHERE customer_id = (SELECT customer_id FROM customer WHERE email = 'host3@fpt.edu.vn')),
    2,
    NOW(),
    NOW(),
    'FPT University Hanoi - Main Campus'
);

-- Thêm vào bảng competition_event
INSERT INTO competition_event (event_id, prize_pool, competition_type, rules) 
VALUES (LAST_INSERT_ID(), '50,000,000 VND', 'Algorithm Programming', 'Individual competition, 5 hours duration, no internet access during contest');

-- 4. Thêm Festival Event
INSERT INTO event (
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
    created_at,
    public_at,
    venue_address
) VALUES (
    'FESTIVAL',
    'Tech Innovation Festival 2025',
    'Celebrate technology and innovation with exhibitions, demos, talks, and interactive experiences. Meet industry leaders and discover cutting-edge technologies.',
    'https://images.unsplash.com/photo-1515187029135-18ee286d815b?w=800&h=600&fit=crop',
    1000,
    '2025-02-15 10:00:00',
    '2025-12-20 08:00:00',
    '2025-12-22 20:00:00',
    '2025-12-15 23:59:59',
    'PUBLIC',
    75,
    'Free entry, networking, tech demos, food & drinks',
    'Technology trends, innovation, networking, industry insights',
    TRUE,
    (SELECT host_id FROM host WHERE customer_id = (SELECT customer_id FROM customer WHERE email = 'host4@fpt.edu.vn')),
    2,
    NOW(),
    NOW(),
    'Ho Chi Minh City Convention Center'
);

-- Thêm vào bảng festival_event
INSERT INTO festival_event (event_id, culture, traditions, activities) 
VALUES (LAST_INSERT_ID(), 'Tech Culture', 'Innovation showcase, networking traditions', 'Tech exhibitions, startup pitches, VR experiences, robotics demo');

-- 5. Thêm Workshop Event khác
INSERT INTO event (
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
    created_at,
    public_at,
    venue_address
) VALUES (
    'WORKSHOP',
    'Web Development Bootcamp',
    'Master modern web development with React, Node.js, and MongoDB. Build full-stack applications from scratch with industry best practices.',
    'https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=800&h=600&fit=crop',
    80,
    '2025-03-01 09:00:00',
    '2025-11-25 09:00:00',
    '2025-11-25 17:00:00',
    '2025-11-20 23:59:59',
    'PUBLIC',
    120,
    'Certificate, portfolio project, job placement assistance',
    'React, Node.js, MongoDB, REST APIs, Git, deployment',
    TRUE,
    (SELECT host_id FROM host WHERE customer_id = (SELECT customer_id FROM customer WHERE email = 'host5@fpt.edu.vn')),
    3,
    NOW(),
    NOW(),
    'FPT University Da Nang - Computer Lab'
);

-- Thêm vào bảng workshop_event
INSERT INTO workshop_event (event_id, materials_link, max_participants, skill_level) 
VALUES (LAST_INSERT_ID(), 'https://github.com/fpt-workshop/web-dev-bootcamp', 80, 'Intermediate');

-- 6. Thêm Music Event khác
INSERT INTO event (
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
    created_at,
    public_at,
    venue_address
) VALUES (
    'MUSIC',
    'Jazz Night at FPT',
    'Experience the smooth sounds of jazz with our talented musicians. Enjoy live performances, cocktails, and a sophisticated evening atmosphere.',
    'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=800&h=600&fit=crop',
    150,
    '2025-03-10 18:00:00',
    '2025-12-05 19:00:00',
    '2025-12-05 23:00:00',
    '2025-12-01 23:59:59',
    'PUBLIC',
    30,
    'Live music, cocktails, networking, elegant atmosphere',
    'Jazz appreciation, music culture, social networking',
    TRUE,
    (SELECT host_id FROM host WHERE customer_id = (SELECT customer_id FROM customer WHERE email = 'host6@fpt.edu.vn')),
    3,
    NOW(),
    NOW(),
    'FPT University HCM - Auditorium'
);

-- Thêm vào bảng music_event
INSERT INTO music_event (event_id, music_type, genre, performer_count) 
VALUES (LAST_INSERT_ID(), 'Concert', 'Jazz, Blues', 8);

-- =============================================
-- BƯỚC 5: Thêm Ticket Types cho các events
-- =============================================

-- Ticket types cho Music Festival (Event ID sẽ được tự động tạo)
SET @music_festival_id = (SELECT id FROM event WHERE event_title = 'FPT Music Festival 2025' ORDER BY id DESC LIMIT 1);

INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
(@music_festival_id, 'Early Bird', 'Early bird ticket with discount', 150000, 200, 45, 0, '2025-01-15 00:00:00', '2025-03-15 23:59:59'),
(@music_festival_id, 'Regular', 'Regular admission ticket', 200000, 250, 78, 0, '2025-03-16 00:00:00', '2025-10-22 23:59:59'),
(@music_festival_id, 'VIP', 'VIP ticket with premium benefits', 400000, 50, 12, 0, '2025-01-15 00:00:00', '2025-10-22 23:59:59');

-- Ticket types cho AI Workshop
SET @ai_workshop_id = (SELECT id FROM event WHERE event_title = 'AI & Machine Learning Workshop' ORDER BY id DESC LIMIT 1);

INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
(@ai_workshop_id, 'Student', 'Student discount ticket', 0, 50, 23, 0, '2025-01-20 00:00:00', '2025-11-10 23:59:59'),
(@ai_workshop_id, 'Professional', 'Professional ticket', 500000, 50, 15, 0, '2025-01-20 00:00:00', '2025-11-10 23:59:59');

-- Ticket types cho Coding Competition
SET @coding_comp_id = (SELECT id FROM event WHERE event_title = 'FPT Coding Competition 2025' ORDER BY id DESC LIMIT 1);

INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
(@coding_comp_id, 'Competitor', 'Competition entry ticket', 0, 200, 156, 0, '2025-02-01 00:00:00', '2025-12-05 23:59:59');

-- Ticket types cho Tech Festival
SET @tech_festival_id = (SELECT id FROM event WHERE event_title = 'Tech Innovation Festival 2025' ORDER BY id DESC LIMIT 1);

INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
(@tech_festival_id, 'General', 'General admission', 0, 800, 234, 0, '2025-02-15 00:00:00', '2025-12-15 23:59:59'),
(@tech_festival_id, 'Premium', 'Premium access with exclusive sessions', 200000, 200, 67, 0, '2025-02-15 00:00:00', '2025-12-15 23:59:59');

-- Ticket types cho Web Dev Bootcamp
SET @webdev_bootcamp_id = (SELECT id FROM event WHERE event_title = 'Web Development Bootcamp' ORDER BY id DESC LIMIT 1);

INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
(@webdev_bootcamp_id, 'Student', 'Student discount', 0, 40, 18, 0, '2025-03-01 00:00:00', '2025-11-20 23:59:59'),
(@webdev_bootcamp_id, 'Professional', 'Professional rate', 800000, 40, 12, 0, '2025-03-01 00:00:00', '2025-11-20 23:59:59');

-- Ticket types cho Jazz Night
SET @jazz_night_id = (SELECT id FROM event WHERE event_title = 'Jazz Night at FPT' ORDER BY id DESC LIMIT 1);

INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
(@jazz_night_id, 'Standard', 'Standard admission', 300000, 100, 34, 0, '2025-03-10 00:00:00', '2025-12-01 23:59:59'),
(@jazz_night_id, 'Premium', 'Premium seating with drinks', 500000, 50, 19, 0, '2025-03-10 00:00:00', '2025-12-01 23:59:59');

-- =============================================
-- BƯỚC 6: Thêm Places cho các events
-- =============================================

-- Thêm places nếu chưa có
INSERT IGNORE INTO place (building, place_name) VALUES
('GAMMA', 'FPT University Da Nang - Gamma Building'),
('LAB', 'FPT University HCM - Lab Building'),
('MAIN', 'FPT University Hanoi - Main Campus'),
('CONVENTION', 'Ho Chi Minh City Convention Center'),
('COMPUTER_LAB', 'FPT University Da Nang - Computer Lab'),
('AUDITORIUM', 'FPT University HCM - Auditorium');

-- Liên kết events với places
INSERT INTO event_place (event_id, place_id) VALUES
(@music_festival_id, (SELECT place_id FROM place WHERE place_name = 'FPT University Da Nang - Gamma Building')),
(@ai_workshop_id, (SELECT place_id FROM place WHERE place_name = 'FPT University HCM - Lab Building')),
(@coding_comp_id, (SELECT place_id FROM place WHERE place_name = 'FPT University Hanoi - Main Campus')),
(@tech_festival_id, (SELECT place_id FROM place WHERE place_name = 'Ho Chi Minh City Convention Center')),
(@webdev_bootcamp_id, (SELECT place_id FROM place WHERE place_name = 'FPT University Da Nang - Computer Lab')),
(@jazz_night_id, (SELECT place_id FROM place WHERE place_name = 'FPT University HCM - Auditorium'));

-- =============================================
-- BƯỚC 7: Cập nhật sequence cho event_id
-- =============================================
UPDATE event_sequence SET next_val = (SELECT MAX(id) + 1 FROM event);

-- =============================================
-- BƯỚC 8: Hiển thị kết quả
-- =============================================
SELECT 
    e.id,
    e.event_type,
    e.event_title,
    e.capacity,
    e.starts_at,
    e.status,
    p.place_name as venue,
    h.host_discount_percent,
    COUNT(tt.ticket_type_id) as ticket_types_count
FROM event e
LEFT JOIN event_place ep ON e.id = ep.event_id
LEFT JOIN place p ON ep.place_id = p.place_id
LEFT JOIN host h ON e.host_id = h.host_id
LEFT JOIN ticket_type tt ON e.id = tt.event_id
WHERE e.id >= (SELECT MAX(id) - 5 FROM event)
GROUP BY e.id, e.event_type, e.event_title, e.capacity, e.starts_at, e.status, p.place_name, h.host_discount_percent
ORDER BY e.id DESC;

-- =============================================
-- Hiển thị thông tin Host
-- =============================================
SELECT 
    h.host_id,
    c.email as host_email,
    c.phone_number,
    o.org_name as organization,
    h.host_discount_percent,
    COUNT(e.id) as events_count
FROM host h
JOIN customer c ON h.customer_id = c.customer_id
JOIN organization o ON h.organize_id = o.org_id
LEFT JOIN event e ON h.host_id = e.host_id
GROUP BY h.host_id, c.email, c.phone_number, o.org_name, h.host_discount_percent
ORDER BY h.host_id;
