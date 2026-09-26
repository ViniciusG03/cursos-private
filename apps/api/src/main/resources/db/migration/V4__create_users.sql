-- Contas da biblioteca. Não há conta padrão aqui: o primeiro ADMIN nasce pelo bootstrap explícito
-- (library.admin-bootstrap.*) e cada MEMBER nasce apenas ao aceitar um convite.
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Normalizado pela aplicação (trim + minúsculas) antes de gravar, buscar ou comparar.
    email VARCHAR(254) NOT NULL,
    -- Somente o hash com salt do PasswordEncoder (formato {id}hash); nunca a senha em texto.
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    password_changed_at TIMESTAMPTZ NOT NULL,
    -- Incrementado a cada troca de senha; sessões guardam a versão do login e são rejeitadas se ficarem para trás.
    credential_version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_not_blank CHECK (btrim(email) <> ''),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'MEMBER')),
    CONSTRAINT ck_users_credential_version_non_negative CHECK (credential_version >= 0)
);

-- A V1 tem um único administrador. O índice também torna o bootstrap seguro contra execuções concorrentes.
CREATE UNIQUE INDEX uq_users_single_admin ON users (role) WHERE role = 'ADMIN';
