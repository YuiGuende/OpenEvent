-- Script để kiểm tra và tạo dữ liệu test cho places
-- Chạy script này để kiểm tra vấn đề places không tải được

-- 1. Kiểm tra cấu trúc bảng place
DESCRIBE place;

-- 2. Kiểm tra cấu trúc bảng event_place
DESCRIBE event_place;

-- 3. Kiểm tra dữ liệu hiện tại
SELECT 'Places in database:' as info;
SELECT * FROM place;

SELECT 'Event-Place relationships:' as info;
SELECT * FROM event_place;

SELECT 'Events in database:' as info;
SELECT id, event_title FROM event LIMIT 5;

-- 4. Tạo dữ liệu test nếu chưa có
INSERT IGNORE INTO place (place_id, place_name, building) VALUES 
(1, 'Phong 101', 'ALPHA'),
(2, 'Phong 201', 'BETA'),
(3, 'Phong 301', 'GAMMA'),
(4, 'Hội trường A', 'ALPHA'),
(5, 'Sân khấu chính', 'BETA');

-- 5. Tạo quan hệ event-place nếu chưa có
-- Thay event_id = 1 bằng event_id thực tế trong database của bạn
INSERT IGNORE INTO event_place (event_id, place_id) VALUES 
(1, 1),
(1, 2),
(1, 3);

-- 6. Kiểm tra kết quả
SELECT 'After inserting test data:' as info;
SELECT p.place_id, p.place_name, p.building, ep.event_id 
FROM place p 
LEFT JOIN event_place ep ON p.place_id = ep.place_id;

-- 7. Test query để lấy places của event
SELECT 'Places for event 1:' as info;
SELECT p.* FROM place p 
INNER JOIN event_place ep ON p.place_id = ep.place_id 
WHERE ep.event_id = 1;
