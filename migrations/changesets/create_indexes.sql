CREATE INDEX idx_links_checked_at_id ON links (last_checked_at, id);
CREATE INDEX idx_chat_links_link_id ON chat_links (link_id);