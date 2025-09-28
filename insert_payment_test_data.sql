-- Insert data for payment testing only
-- Run this after you have accounts and users in the database

USE openevent;

-- 1. Insert Organizations (needed for events)
INSERT IGNORE INTO organization (org_name, description, email, phone, address, website, created_at) VALUES
('TechCorp Vietnam', 'Leading technology company in Vietnam', 'contact@techcorp.vn', '0123456789', '123 Nguyen Hue, District 1, HCMC', 'https://techcorp.vn', NOW()),
('Music Academy', 'Professional music education center', 'info@musicacademy.vn', '0987654321', '456 Le Loi, District 3, HCMC', 'https://musicacademy.vn', NOW()),
('Business Network', 'Business networking and consulting', 'hello@businessnetwork.vn', '0369258147', '789 Dong Khoi, District 1, HCMC', 'https://businessnetwork.vn', NOW());

-- 2. Insert Places (needed for events)
INSERT IGNORE INTO place (place_name, building) VALUES
('Main Auditorium', 'ALPHA'),
('Conference Room A', 'ALPHA'),
('Conference Room B', 'BETA'),
('Outdoor Stage', 'NONE'),
('Workshop Room 1', 'BETA'),
('Workshop Room 2', 'BETA');

-- 3. Insert Speakers (needed for events)
INSERT IGNORE INTO speaker (name, default_role, profile, image_url) VALUES
('Dr. Nguyen Van A', 'SPEAKER', 'Senior Software Engineer with 10+ years experience', 'https://example.com/speaker1.jpg'),
('Ms. Tran Thi B', 'PERFORMER', 'Professional singer and music producer', 'https://example.com/speaker2.jpg'),
('Mr. Le Van C', 'MC', 'Experienced event host and MC', 'https://example.com/speaker3.jpg'),
('Prof. Pham Van D', 'SPEAKER', 'Business consultant and professor', 'https://example.com/speaker4.jpg'),
('Artist Hoang Thi E', 'ARTIST', 'Contemporary artist and designer', 'https://example.com/speaker5.jpg');

-- 4. Insert Events (main events for testing)
INSERT IGNORE INTO event (event_type, event_title, description, starts_at, ends_at, enroll_deadline, status, image_url, points, benefits, learning_objects, created_at, draft_at, public_at) VALUES
('MusicEvent', 'Music Festival 2025', 'Join us for an amazing music festival featuring top artists from Vietnam and around the world. Experience live performances, workshops, and networking opportunities.', '2025-09-17 18:00:00', '2025-09-17 23:00:00', '2025-09-15 23:59:59', 'PUBLIC', 'https://example.com/music-festival.jpg', 50, 'Certificate of participation, Networking opportunities, Live performances', 'Music appreciation, Event management, Networking skills', NOW(), NOW(), NOW()),
('WorkshopEvent', 'Tech Workshop 2025', 'Hands-on workshop covering modern web development technologies including React, Node.js, and cloud deployment.', '2025-09-20 09:00:00', '2025-09-20 17:00:00', '2025-09-18 23:59:59', 'PUBLIC', 'https://example.com/tech-workshop.jpg', 75, 'Certificate of completion, Project portfolio, Job opportunities', 'Web development, React.js, Node.js, Cloud deployment', NOW(), NOW(), NOW()),
('CompetitionEvent', 'Business Pitch Competition', 'Showcase your innovative business ideas and compete for amazing prizes. Perfect for entrepreneurs and startup enthusiasts.', '2025-09-22 08:00:00', '2025-09-22 18:00:00', '2025-09-20 23:59:59', 'PUBLIC', 'https://example.com/business-pitch.jpg', 100, 'Cash prizes, Mentorship opportunities, Investment connections', 'Pitching skills, Business planning, Presentation techniques', NOW(), NOW(), NOW()),
('ConferenceEvent', 'AI & Tech Conference 2025', 'Join industry leaders and experts for an in-depth conference on Artificial Intelligence, Machine Learning, and emerging technologies.', '2025-09-25 09:00:00', '2025-09-25 18:00:00', '2025-09-23 23:59:59', 'PUBLIC', 'https://example.com/ai-conference.jpg', 80, 'Conference materials, Networking lunch, Expert insights', 'AI fundamentals, Machine Learning, Industry trends', NOW(), NOW(), NOW());

-- 5. Insert Music Events (specific fields)
INSERT IGNORE INTO music_event (event_id, genre, performer_count) 
SELECT id, 'Pop Rock', 5 FROM event WHERE event_type = 'MusicEvent' LIMIT 1;

-- 6. Insert Workshop Events (specific fields)
INSERT IGNORE INTO workshop_event (event_id, max_participants, skill_level, prerequisites) 
SELECT id, 30, 'Intermediate', 'Basic programming knowledge recommended' FROM event WHERE event_type = 'WorkshopEvent' LIMIT 1;

-- 7. Insert Competition Events (specific fields)
INSERT IGNORE INTO competition_event (event_id, prize_pool, competition_type, rules) 
SELECT id, '10,000,000 VND', 'Business Pitch', 'Participants must present a 5-minute business pitch. Judges will evaluate based on innovation, feasibility, and presentation quality.' FROM event WHERE event_type = 'CompetitionEvent' LIMIT 1;

-- 8. Insert Conference Events (specific fields)
INSERT IGNORE INTO conference_event (event_id, conference_type, max_attendees, agenda) 
SELECT id, 'Technology Conference', 200, '9:00 AM - Welcome & Keynote, 10:30 AM - AI Breakthroughs, 2:00 PM - ML Applications, 4:00 PM - Panel Discussion, 5:30 PM - Networking' FROM event WHERE event_type = 'ConferenceEvent' LIMIT 1;

-- 9. Insert Places for Events
INSERT IGNORE INTO event_place (event_id, place_id) 
SELECT e.id, p.place_id FROM event e, place p 
WHERE (e.event_type = 'MusicEvent' AND p.place_name = 'Outdoor Stage')
   OR (e.event_type = 'WorkshopEvent' AND p.place_name = 'Workshop Room 1')
   OR (e.event_type = 'CompetitionEvent' AND p.place_name = 'Main Auditorium')
   OR (e.event_type = 'ConferenceEvent' AND p.place_name = 'Conference Room A');

-- 10. Insert Speakers for Events
INSERT IGNORE INTO event_speaker (event_id, speaker_id, role) 
SELECT e.id, s.speaker_id, 'PERFORMER' FROM event e, speaker s 
WHERE e.event_type = 'MusicEvent' AND s.name = 'Ms. Tran Thi B'
UNION ALL
SELECT e.id, s.speaker_id, 'MC' FROM event e, speaker s 
WHERE e.event_type = 'MusicEvent' AND s.name = 'Mr. Le Van C'
UNION ALL
SELECT e.id, s.speaker_id, 'SPEAKER' FROM event e, speaker s 
WHERE e.event_type = 'WorkshopEvent' AND s.name = 'Dr. Nguyen Van A'
UNION ALL
SELECT e.id, s.speaker_id, 'SPEAKER' FROM event e, speaker s 
WHERE e.event_type = 'CompetitionEvent' AND s.name = 'Prof. Pham Van D'
UNION ALL
SELECT e.id, s.speaker_id, 'MC' FROM event e, speaker s 
WHERE e.event_type = 'CompetitionEvent' AND s.name = 'Mr. Le Van C'
UNION ALL
SELECT e.id, s.speaker_id, 'SPEAKER' FROM event e, speaker s 
WHERE e.event_type = 'ConferenceEvent' AND s.name = 'Dr. Nguyen Van A'
UNION ALL
SELECT e.id, s.speaker_id, 'SPEAKER' FROM event e, speaker s 
WHERE e.event_type = 'ConferenceEvent' AND s.name = 'Artist Hoang Thi E';

-- 11. Insert Event Images
INSERT IGNORE INTO event_image (event_id, url, order_index, main_poster) 
SELECT e.id, 'https://example.com/music-festival-1.jpg', 1, true FROM event e WHERE e.event_type = 'MusicEvent'
UNION ALL
SELECT e.id, 'https://example.com/music-festival-2.jpg', 2, false FROM event e WHERE e.event_type = 'MusicEvent'
UNION ALL
SELECT e.id, 'https://example.com/tech-workshop-1.jpg', 1, true FROM event e WHERE e.event_type = 'WorkshopEvent'
UNION ALL
SELECT e.id, 'https://example.com/business-pitch-1.jpg', 1, true FROM event e WHERE e.event_type = 'CompetitionEvent'
UNION ALL
SELECT e.id, 'https://example.com/ai-conference-1.jpg', 1, true FROM event e WHERE e.event_type = 'ConferenceEvent';

-- 11. Insert Ticket Types for Events
INSERT IGNORE INTO ticket_type (name, price, total_quantity, event_id)
SELECT 'General Admission', 250000, 100, e.id FROM event e WHERE e.event_type = 'MusicEvent'
UNION ALL
SELECT 'VIP', 500000, 50, e.id FROM event e WHERE e.event_type = 'MusicEvent'
UNION ALL
SELECT 'Workshop Pass', 150000, 30, e.id FROM event e WHERE e.event_type = 'WorkshopEvent'
UNION ALL
SELECT 'Competition Entry', 500000, 20, e.id FROM event e WHERE e.event_type = 'CompetitionEvent'
UNION ALL
SELECT 'Conference Pass', 300000, 200, e.id FROM event e WHERE e.event_type = 'ConferenceEvent';

-- 12. Insert Event Schedules
INSERT IGNORE INTO event_schedule (event_id, activity, start_time, end_time) 
SELECT e.id, 'Opening Ceremony', '2025-09-17 18:00:00', '2025-09-17 18:30:00' FROM event e WHERE e.event_type = 'MusicEvent'
UNION ALL
SELECT e.id, 'Live Performance 1', '2025-09-17 18:30:00', '2025-09-17 20:00:00' FROM event e WHERE e.event_type = 'MusicEvent'
UNION ALL
SELECT e.id, 'Break', '2025-09-17 20:00:00', '2025-09-17 20:15:00' FROM event e WHERE e.event_type = 'MusicEvent'
UNION ALL
SELECT e.id, 'Live Performance 2', '2025-09-17 20:15:00', '2025-09-17 21:45:00' FROM event e WHERE e.event_type = 'MusicEvent'
UNION ALL
SELECT e.id, 'Closing Ceremony', '2025-09-17 21:45:00', '2025-09-17 22:00:00' FROM event e WHERE e.event_type = 'MusicEvent'
UNION ALL
SELECT e.id, 'Introduction & Setup', '2025-09-20 09:00:00', '2025-09-20 09:30:00' FROM event e WHERE e.event_type = 'WorkshopEvent'
UNION ALL
SELECT e.id, 'React Basics Workshop', '2025-09-20 09:30:00', '2025-09-20 12:00:00' FROM event e WHERE e.event_type = 'WorkshopEvent'
UNION ALL
SELECT e.id, 'Lunch Break', '2025-09-20 12:00:00', '2025-09-20 13:00:00' FROM event e WHERE e.event_type = 'WorkshopEvent'
UNION ALL
SELECT e.id, 'Node.js Backend Workshop', '2025-09-20 13:00:00', '2025-09-20 15:30:00' FROM event e WHERE e.event_type = 'WorkshopEvent'
UNION ALL
SELECT e.id, 'Cloud Deployment Workshop', '2025-09-20 15:30:00', '2025-09-20 17:00:00' FROM event e WHERE e.event_type = 'WorkshopEvent'
UNION ALL
SELECT e.id, 'Registration & Welcome', '2025-09-22 08:00:00', '2025-09-22 08:30:00' FROM event e WHERE e.event_type = 'CompetitionEvent'
UNION ALL
SELECT e.id, 'Pitch Presentations', '2025-09-22 08:30:00', '2025-09-22 12:00:00' FROM event e WHERE e.event_type = 'CompetitionEvent'
UNION ALL
SELECT e.id, 'Lunch & Networking', '2025-09-22 12:00:00', '2025-09-22 13:00:00' FROM event e WHERE e.event_type = 'CompetitionEvent'
UNION ALL
SELECT e.id, 'Final Presentations', '2025-09-22 13:00:00', '2025-09-22 16:00:00' FROM event e WHERE e.event_type = 'CompetitionEvent'
UNION ALL
SELECT e.id, 'Awards Ceremony', '2025-09-22 16:00:00', '2025-09-22 18:00:00' FROM event e WHERE e.event_type = 'CompetitionEvent'
UNION ALL
SELECT e.id, 'Welcome & Registration', '2025-09-25 09:00:00', '2025-09-25 09:30:00' FROM event e WHERE e.event_type = 'ConferenceEvent'
UNION ALL
SELECT e.id, 'Keynote: Future of AI', '2025-09-25 09:30:00', '2025-09-25 10:30:00' FROM event e WHERE e.event_type = 'ConferenceEvent'
UNION ALL
SELECT e.id, 'Coffee Break', '2025-09-25 10:30:00', '2025-09-25 11:00:00' FROM event e WHERE e.event_type = 'ConferenceEvent'
UNION ALL
SELECT e.id, 'AI Breakthroughs Session', '2025-09-25 11:00:00', '2025-09-25 12:30:00' FROM event e WHERE e.event_type = 'ConferenceEvent'
UNION ALL
SELECT e.id, 'Networking Lunch', '2025-09-25 12:30:00', '2025-09-25 14:00:00' FROM event e WHERE e.event_type = 'ConferenceEvent'
UNION ALL
SELECT e.id, 'ML Applications Workshop', '2025-09-25 14:00:00', '2025-09-25 16:00:00' FROM event e WHERE e.event_type = 'ConferenceEvent'
UNION ALL
SELECT e.id, 'Panel Discussion', '2025-09-25 16:00:00', '2025-09-25 17:30:00' FROM event e WHERE e.event_type = 'ConferenceEvent'
UNION ALL
SELECT e.id, 'Closing & Networking', '2025-09-25 17:30:00', '2025-09-25 18:00:00' FROM event e WHERE e.event_type = 'ConferenceEvent';

-- Display summary
SELECT 'Payment test data inserted successfully!' as Status;

SELECT 
    'Organizations' as Table_Name, COUNT(*) as Records FROM organization
UNION ALL
SELECT 'Places', COUNT(*) FROM place
UNION ALL
SELECT 'Speakers', COUNT(*) FROM speaker
UNION ALL
SELECT 'Events', COUNT(*) FROM event
UNION ALL
SELECT 'Event Schedules', COUNT(*) FROM event_schedule
UNION ALL
SELECT 'Event Speakers', COUNT(*) FROM event_speaker
UNION ALL
SELECT 'Event Images', COUNT(*) FROM event_image;
