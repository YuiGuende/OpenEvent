-- Script to fix existing event_type values in database
-- This will update any existing records that use old class names to new enum values

UPDATE event SET event_type = 'MUSIC' WHERE event_type = 'MusicEvent';
UPDATE event SET event_type = 'WORKSHOP' WHERE event_type = 'WorkshopEvent';
UPDATE event SET event_type = 'COMPETITION' WHERE event_type = 'CompetitionEvent';
UPDATE event SET event_type = 'FESTIVAL' WHERE event_type = 'FestivalEvent';
UPDATE event SET event_type = 'OTHERS' WHERE event_type = 'OtherEvent';

-- Verify the changes
SELECT event_type, COUNT(*) as count FROM event GROUP BY event_type;
