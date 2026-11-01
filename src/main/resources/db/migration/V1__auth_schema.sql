-- auth 스키마 첫 버전. 원본: Crowfoot 4cut-mvp v24 (docs 레포 docs/db/4cut-mvp.sql의 auth 그룹)
-- ID 규칙: 기본 키는 BIGINT IDENTITY, 밖에 보이는 리소스는 NanoID public_id(21자)를 따로 둔다.
-- 이미 v24가 반영된 DB(개발 공용 스키마)에서는 Flyway baseline(1)으로 건너뛴다.
CREATE TABLE users (
    id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    nickname VARCHAR(30) NOT NULL,
    avatar_url VARCHAR(500),
    email VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    public_id VARCHAR(21) NOT NULL,
    CONSTRAINT users_pk PRIMARY KEY (id),
    CONSTRAINT uk_users_nickname UNIQUE (nickname),
    CONSTRAINT uk_users_public_id UNIQUE (public_id),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'DELETING'))
);
CREATE TABLE social_accounts (
    id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT social_accounts_pk PRIMARY KEY (id),
    CONSTRAINT uk_social_accounts_provider_provider_user_id UNIQUE (provider, provider_user_id),
    CONSTRAINT ck_social_accounts_provider CHECK (provider IN ('GITHUB', 'GOOGLE'))
);
CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    family_id UUID NOT NULL,
    expires_at TIMESTAMPTZ(6) NOT NULL,
    used_at TIMESTAMPTZ(6),
    revoked_at TIMESTAMPTZ(6),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT refresh_tokens_pk PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash)
);
CREATE TABLE roles (
    id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT roles_pk PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (code),
    CONSTRAINT ck_roles_code CHECK (code LIKE 'ROLE\_%')
);
CREATE TABLE user_roles (
    id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    role_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT user_roles_pk PRIMARY KEY (id),
    CONSTRAINT uk_user_roles_user_id_role_id UNIQUE (user_id, role_id)
);
ALTER TABLE social_accounts ADD CONSTRAINT fk_social_accounts_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_roles FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT;
CREATE INDEX idx_social_accounts_user_id ON social_accounts (user_id ASC);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id ASC);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id ASC);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id ASC);
CREATE INDEX idx_user_roles_user_id ON user_roles (user_id ASC);
