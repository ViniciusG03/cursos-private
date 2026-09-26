-- Contadores de tentativas dos fluxos públicos (login, recuperação, consumo de tokens) em janela fixa.
-- Ficam no banco, e não num mapa em memória, para que o crescimento seja limitado pela limpeza periódica
-- das janelas vencidas. A chave é o SHA-256 de escopo + tipo + valor: IP e e-mail não ficam em texto.
CREATE TABLE auth_attempt_windows (
    bucket_key VARCHAR(64) PRIMARY KEY,
    window_started_at TIMESTAMPTZ NOT NULL,
    attempt_count INT NOT NULL,

    CONSTRAINT ck_auth_attempt_windows_count_positive CHECK (attempt_count > 0)
);

CREATE INDEX ix_auth_attempt_windows_window_started_at ON auth_attempt_windows (window_started_at);
