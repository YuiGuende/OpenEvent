-- Kiểm tra dữ liệu speakers và event_speaker
SELECT 'Speakers Table:' as info;
SELECT * FROM speaker;

SELECT 'Event_Speaker Table:' as info;
SELECT * FROM event_speaker;

SELECT 'Event 1 Speakers:' as info;
SELECT s.*, es.event_id 
FROM speaker s 
JOIN event_speaker es ON s.speaker_id = es.speaker_id 
WHERE es.event_id = 1;

SELECT 'Event 5 Speakers (Music):' as info;
SELECT s.*, es.event_id 
FROM speaker s 
JOIN event_speaker es ON s.speaker_id = es.speaker_id 
WHERE es.event_id = 5;
