ALTER TABLE conversations
  ADD COLUMN messaging_permission VARCHAR(20) NOT NULL DEFAULT 'ALL';
ALTER TABLE conversations
  ADD COLUMN dissolved_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE conversation_participants
  ADD COLUMN left_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE conversation_participants
  ADD COLUMN deleted_conversation BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE conversation_participants
SET role = 'OWNER'
WHERE user_id IN (
  SELECT c.creator_id FROM conversations c
  WHERE c.id = conversation_participants.conversation_id
    AND c.type = 'GROUP'
);

CREATE INDEX idx_cp_left_at ON conversation_participants(left_at);
