ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS last_message_content VARCHAR(255);

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS last_message_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS message_read_receipts (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    user_id UUID NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_mrr_message_user UNIQUE (message_id, user_id),
    CONSTRAINT fk_mrr_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mrr_user ON message_read_receipts(user_id);
