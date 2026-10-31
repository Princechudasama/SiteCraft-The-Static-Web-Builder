CREATE TABLE chat_messages (
                               id BIGSERIAL PRIMARY KEY,

                               conversation_id UUID NOT NULL,

                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               message TEXT NOT NULL,

                               role VARCHAR(20) NOT NULL,

                               CONSTRAINT chk_chat_message_role
                                   CHECK (role IN ('USER', 'ASSISTANT'))
);

CREATE INDEX idx_chat_messages_conversation_id
    ON chat_messages(conversation_id);

CREATE INDEX idx_chat_messages_created_at
    ON chat_messages(created_at);