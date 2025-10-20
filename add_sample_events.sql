-- =============================================
-- SQL Script: Thêm các Event mẫu cho OpenEvent
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
    'https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=800&h=600&fit=crop',
    500,
    '2025-01-15 10:00:00',
    '2025-10-25 18:00:00',
    '2025-10-26 23:00:00',
    '2025-10-20 23:59:59',
    'PUBLIC',
    50,
    'Free gifts, networking, music vibes, food trucks',
    'Music appreciation, cultural experience',
    TRUE,
    1,
    1,
    NOW(),
    NOW(),
    'FPT University Da Nang - Alpha Building'
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
    1,
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
    1,
    1,
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
    1,
    1,
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
    1,
    1,
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
    1,
    1,
    NOW(),
    NOW(),
    'FPT University HCM - Auditorium'
);

-- Thêm vào bảng music_event
INSERT INTO music_event (event_id, music_type, genre, performer_count) 
VALUES (LAST_INSERT_ID(), 'Concert', 'Jazz, Blues', 8);

-- =============================================
-- Thêm Ticket Types cho các events
-- =============================================

-- Ticket types cho Music Festival
INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
(1, 'Early Bird', 'Early bird ticket with discount', 150000, 200, 45, 0, '2025-01-15 00:00:00', '2025-03-15 23:59:59'),
(1, 'Regular', 'Regular admission ticket', 200000, 250, 78, 0, '2025-03-16 00:00:00', '2025-10-20 23:59:59'),
(1, 'VIP', 'VIP ticket with premium benefits', 400000, 50, 12, 0, '2025-01-15 00:00:00', '2025-10-20 23:59:59');

-- Ticket types cho AI Workshop
INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
(2, 'Student', 'Student discount ticket', 0, 50, 23, 0, '2025-01-20 00:00:00', '2025-11-10 23:59:59'),
(2, 'Professional', 'Professional ticket', 500000, 50, 15, 0, '2025-01-20 00:00:00', '2025-11-10 23:59:59');

-- Ticket types cho Coding Competition
INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
(3, 'Competitor', 'Competition entry ticket', 0, 200, 156, 0, '2025-02-01 00:00:00', '2025-12-05 23:59:59');

-- Ticket types cho Tech Festival
INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
(4, 'General', 'General admission', 0, 800, 234, 0, '2025-02-15 00:00:00', '2025-12-15 23:59:59'),
(4, 'Premium', 'Premium access with exclusive sessions', 200000, 200, 67, 0, '2025-02-15 00:00:00', '2025-12-15 23:59:59');

-- Ticket types cho Web Dev Bootcamp
INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
(5, 'Student', 'Student discount', 0, 40, 18, 0, '2025-03-01 00:00:00', '2025-11-20 23:59:59'),
(5, 'Professional', 'Professional rate', 800000, 40, 12, 0, '2025-03-01 00:00:00', '2025-11-20 23:59:59');

-- Ticket types cho Jazz Night
INSERT INTO ticket_type (event_id, name, description, price, total_quantity, sold_quantity, sale, start_sale_date, end_sale_date) VALUES
(6, 'Standard', 'Standard admission', 300000, 100, 34, 0, '2025-03-10 00:00:00', '2025-12-01 23:59:59'),
(6, 'Premium', 'Premium seating with drinks', 500000, 50, 19, 0, '2025-03-10 00:00:00', '2025-12-01 23:59:59');

-- =============================================
-- Thêm Places cho các events
-- =============================================

-- Thêm places nếu chưa có
INSERT IGNORE INTO place (building, place_name) VALUES
('ALPHA', 'FPT University Da Nang - Alpha Building'),
('LAB', 'FPT University HCM - Lab Building'),
('MAIN', 'FPT University Hanoi - Main Campus'),
('CONVENTION', 'Ho Chi Minh City Convention Center'),
('COMPUTER_LAB', 'FPT University Da Nang - Computer Lab'),
('AUDITORIUM', 'FPT University HCM - Auditorium');

-- Liên kết events với places
INSERT INTO event_place (event_id, place_id) VALUES
(1, (SELECT place_id FROM place WHERE place_name = 'FPT University Da Nang - Alpha Building')),
(2, (SELECT place_id FROM place WHERE place_name = 'FPT University HCM - Lab Building')),
(3, (SELECT place_id FROM place WHERE place_name = 'FPT University Hanoi - Main Campus')),
(4, (SELECT place_id FROM place WHERE place_name = 'Ho Chi Minh City Convention Center')),
(5, (SELECT place_id FROM place WHERE place_name = 'FPT University Da Nang - Computer Lab')),
(6, (SELECT place_id FROM place WHERE place_name = 'FPT University HCM - Auditorium'));

-- =============================================
-- Cập nhật sequence cho event_id
-- =============================================
UPDATE event_sequence SET next_val = (SELECT MAX(id) + 1 FROM event);

-- =============================================
-- Hiển thị kết quả
-- =============================================
SELECT 
    e.id,
    e.event_type,
    e.event_title,
    e.capacity,
    e.starts_at,
    e.status,
    p.place_name as venue,
    COUNT(tt.ticket_type_id) as ticket_types_count
FROM event e
LEFT JOIN event_place ep ON e.id = ep.event_id
LEFT JOIN place p ON ep.place_id = p.place_id
LEFT JOIN ticket_type tt ON e.id = tt.event_id
WHERE e.id >= (SELECT MAX(id) - 5 FROM event)
GROUP BY e.id, e.event_type, e.event_title, e.capacity, e.starts_at, e.status, p.place_name
ORDER BY e.id DESC;
