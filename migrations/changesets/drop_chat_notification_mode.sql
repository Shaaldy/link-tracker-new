ALTER TABLE chats DROP CONSTRAINT chk_digest_hour;
ALTER TABLE chats DROP COLUMN digest_hour;
ALTER TABLE chats DROP COLUMN notification_mode;