CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(255),
    description TEXT,
    creator_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE INDEX idx_conversations_tenant ON conversations(tenant_id);

CREATE TABLE conversation_participants (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP(),
    unread_count INTEGER NOT NULL DEFAULT 0,
    last_read_at TIMESTAMP WITH TIME ZONE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    CONSTRAINT uk_cp_conversation_user UNIQUE (conversation_id, user_id),
    CONSTRAINT fk_cp_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_cp_user ON conversation_participants(user_id);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    CONSTRAINT fk_msg_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_conversation_date ON messages(conversation_id, created_at DESC);
CREATE INDEX idx_messages_status ON messages(status);
