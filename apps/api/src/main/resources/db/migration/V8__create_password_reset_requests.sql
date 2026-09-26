-- Fila durável dos pedidos públicos de recuperação (revisão do marco 2). O endpoint grava a linha e
-- responde 202; um worker emite o token e envia o e-mail depois. Se o processo parar, a linha continua
-- PENDING e é processada quando a aplicação voltar. Aqui nunca fica token: ele é emitido no envio e só
-- o hash vai para password_reset_tokens.
CREATE TABLE password_reset_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Gravado para qualquer e-mail bem formado, com ou sem conta: a resposta pública não pode diferir.
    email VARCHAR(254) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    last_attempt_at TIMESTAMPTZ,
    -- Motivo da última falha de envio (destinatário e erro SMTP, nunca o corpo com o link).
    last_error VARCHAR(1000),
    completed_at TIMESTAMPTZ,

    CONSTRAINT ck_password_reset_requests_email_not_blank CHECK (btrim(email) <> ''),
    CONSTRAINT ck_password_reset_requests_status CHECK (status IN ('PENDING', 'SENT', 'NO_ACCOUNT', 'FAILED')),
    CONSTRAINT ck_password_reset_requests_attempt_count_non_negative CHECK (attempt_count >= 0),
    CONSTRAINT ck_password_reset_requests_completed_matches_status
        CHECK ((status = 'PENDING') = (completed_at IS NULL))
);

-- Varredura do worker: só pendentes, pela próxima tentativa.
CREATE INDEX ix_password_reset_requests_due ON password_reset_requests (next_attempt_at) WHERE status = 'PENDING';

-- Limpeza dos concluídos após a retenção.
CREATE INDEX ix_password_reset_requests_completed_at ON password_reset_requests (completed_at)
    WHERE completed_at IS NOT NULL;
