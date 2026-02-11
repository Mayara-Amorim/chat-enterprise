-- Habilita extensão para UUID se necessário (boa prática, embora o Java esteja gerando)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Tabela de Conversas
CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL, -- INDIVIDUAL, GROUP
    title VARCHAR(255),        -- Pode ser NULL em conversas 1:1
    creator_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Índice para isolamento de Tenant e busca rápida
CREATE INDEX idx_conversations_tenant ON conversations(tenant_id);


-- 2. Tabela de Participantes (Join Table para Many-to-Many)
-- Mapeia quem está em qual conversa
CREATE TABLE conversation_participants (
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT fk_cp_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

-- Índice Crítico: "Quais conversas eu participo?"
-- Sem isso, a tela inicial do chat seria muito lenta.
CREATE INDEX idx_cp_user ON conversation_participants(user_id);


-- 3. Tabela de Mensagens
CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL, -- SENT, DELIVERED, READ
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_msg_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

-- Índice Composto Crítico: "Paginação de mensagens"
-- Permite buscar mensagens de uma conversa ordenadas por data de forma instantânea.
CREATE INDEX idx_messages_conversation_date ON messages(conversation_id, created_at DESC);

-- Índice para verificar status (ex: "Quantas mensagens não lidas?")
CREATE INDEX idx_messages_status ON messages(status) WHERE status != 'READ';