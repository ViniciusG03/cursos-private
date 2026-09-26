-- Tokens de recuperação de acesso: uso único, 30 minutos, somente o SHA-256 (hex) do token bruto.
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,

    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_password_reset_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_password_reset_tokens_token_hash_sha256_hex CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_password_reset_tokens_expires_after_creation CHECK (expires_at > created_at),
    CONSTRAINT ck_password_reset_tokens_consumed_or_revoked CHECK (consumed_at IS NULL OR revoked_at IS NULL)
);

-- Um novo pedido revoga os anteriores; o índice garante isso mesmo com pedidos concorrentes.
CREATE UNIQUE INDEX uq_password_reset_tokens_open_user ON password_reset_tokens (user_id)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;
