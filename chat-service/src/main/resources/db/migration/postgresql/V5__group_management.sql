ALTER TABLE conversations
  ADD COLUMN messaging_permission VARCHAR(20) NOT NULL DEFAULT 'ALL',
  ADD COLUMN dissolved_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE conversation_participants
  ADD COLUMN left_at TIMESTAMP WITH TIME ZONE,
  ADD COLUMN deleted_conversation BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE conversation_participants cp
SET role = 'OWNER'
FROM conversations c
WHERE cp.conversation_id = c.id
  AND cp.user_id = c.creator_id
  AND c.type = 'GROUP';

CREATE INDEX idx_cp_left_at ON conversation_participants(left_at);
