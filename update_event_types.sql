-- Update existing event types to match the new discriminator values
UPDATE event SET event_type = 'MUSIC' WHERE event_type = 'MusicEvent';
UPDATE event SET event_type = 'WORKSHOP' WHERE event_type = 'WorkshopEvent';
UPDATE event SET event_type = 'COMPETITION' WHERE event_type = 'CompetitionEvent';
UPDATE event SET event_type = 'FESTIVAL' WHERE event_type = 'FestivalEvent';
UPDATE event SET event_type = 'OTHERS' WHERE event_type = 'OtherEvent';
