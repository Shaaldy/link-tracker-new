-- Режим уведомлений чата (бонус Stage 3: батчинг/дайджест).
-- notification_mode: INSTANT (слать сразу, дефолт) | DIGEST (копить и слать раз в сутки).
-- digest_hour: час отправки дайджеста 0–23 в серверной таймзоне; NULL при INSTANT.
ALTER TABLE chats
    ADD COLUMN notification_mode TEXT NOT NULL DEFAULT 'INSTANT',
    ADD COLUMN digest_hour       INT;

-- Инвариант: DIGEST обязан иметь час, INSTANT — не должен.
ALTER TABLE chats
    ADD CONSTRAINT chk_digest_hour
        CHECK (
            (notification_mode = 'INSTANT' AND digest_hour IS NULL)
                OR (notification_mode = 'DIGEST'  AND digest_hour BETWEEN 0 AND 23)
            );