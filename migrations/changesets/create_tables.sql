-- chats: Telegram-чаты. PK = реальный Telegram chat id (приходит извне, уже уникален).
CREATE TABLE chats (
                       chat_id    BIGINT      PRIMARY KEY,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- links: уникальные отслеживаемые ссылки.
-- last_checked_at — водяная метка планировщика (до какого момента активность уже обработана).
CREATE TABLE links (
                       id              BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       url             TEXT        NOT NULL UNIQUE,
                       last_checked_at TIMESTAMPTZ NOT NULL DEFAULT '-infinity',
                       created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- chat_links: подписка (центральная сущность домена). Составной PK гарантирует уникальность подписки.
CREATE TABLE chat_links (
                            chat_id    BIGINT      NOT NULL REFERENCES chats (chat_id) ON DELETE CASCADE,
                            link_id    BIGINT      NOT NULL REFERENCES links (id)     ON DELETE CASCADE,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                            PRIMARY KEY (chat_id, link_id)
);

-- link_tags: теги подписки (1:N от chat_links). Тег принадлежит подписке, не ссылке.
CREATE TABLE link_tags (
                           chat_id BIGINT NOT NULL,
                           link_id BIGINT NOT NULL,
                           tag     TEXT   NOT NULL,
                           PRIMARY KEY (chat_id, link_id, tag),
                           FOREIGN KEY (chat_id, link_id) REFERENCES chat_links (chat_id, link_id) ON DELETE CASCADE
);

-- link_filters: фильтры подписки (1:N от chat_links).
CREATE TABLE link_filters (
                              chat_id BIGINT NOT NULL,
                              link_id BIGINT NOT NULL,
                              filter  TEXT   NOT NULL,
                              PRIMARY KEY (chat_id, link_id, filter),
                              FOREIGN KEY (chat_id, link_id) REFERENCES chat_links (chat_id, link_id) ON DELETE CASCADE
);