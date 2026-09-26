-- Convites de uso único emitidos pelo ADMIN. O token bruto só existe no e-mail; aqui fica o SHA-256 (hex).
CREATE TABLE invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(254) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_by_user_id UUID NOT NULL,
    -- Falha de envio fica registrada aqui para o ADMIN reenviar (o reenvio revoga este convite).
    delivery_status VARCHAR(20) NOT NULL,
    delivery_attempted_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,

    CONSTRAINT fk_invitations_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT uq_invitations_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_invitations_email_not_blank CHECK (btrim(email) <> ''),
    CONSTRAINT ck_invitations_token_hash_sha256_hex CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_invitations_expires_after_creation CHECK (expires_at > created_at),
    CONSTRAINT ck_invitations_consumed_or_revoked CHECK (consumed_at IS NULL OR revoked_at IS NULL),
    CONSTRAINT ck_invitations_delivery_status CHECK (delivery_status IN ('PENDING', 'SENT', 'FAILED'))
);

-- No máximo um convite em aberto (nem consumido nem revogado) por e-mail. Expirados continuam "em aberto"
-- até o reenvio revogá-los, o que mantém a regra simples e verificável pelo banco.
CREATE UNIQUE INDEX uq_invitations_open_email ON invitations (email)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;

CREATE INDEX ix_invitations_created_by_user_id ON invitations (created_by_user_id);
