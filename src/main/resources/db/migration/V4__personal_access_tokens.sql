-- 개인 액세스 토큰. 로그인한 회원이 직접 발급하고, 자동화 도구가 쿠키 없이 게시할 때 쓴다.
-- 원문은 저장하지 않고 SHA-256 해시만 둔다. 권한 범위(scope)는 게시(PUBLISH) 하나뿐이다.
CREATE TABLE personal_access_tokens (
    id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    public_id VARCHAR(21) NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    token_hint VARCHAR(12) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ(6) NOT NULL,
    last_used_at TIMESTAMPTZ(6),
    revoked_at TIMESTAMPTZ(6),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT personal_access_tokens_pk PRIMARY KEY (id),
    CONSTRAINT uk_personal_access_tokens_public_id UNIQUE (public_id),
    CONSTRAINT uk_personal_access_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_personal_access_tokens_scope CHECK (scope IN ('PUBLISH'))
);
-- 감사 기록: 발급(ISSUED), 사용(USED, Access Token으로 바꿀 때마다), 폐기(REVOKED)
CREATE TABLE personal_access_token_events (
    id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    token_id BIGINT NOT NULL,
    event VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT personal_access_token_events_pk PRIMARY KEY (id),
    CONSTRAINT ck_personal_access_token_events_event CHECK (event IN ('ISSUED', 'USED', 'REVOKED'))
);
ALTER TABLE personal_access_tokens ADD CONSTRAINT fk_personal_access_tokens_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE personal_access_token_events ADD CONSTRAINT fk_personal_access_token_events_tokens FOREIGN KEY (token_id) REFERENCES personal_access_tokens (id) ON DELETE CASCADE;
CREATE INDEX idx_personal_access_tokens_user_id ON personal_access_tokens (user_id ASC);
CREATE INDEX idx_personal_access_token_events_token_id ON personal_access_token_events (token_id ASC);
