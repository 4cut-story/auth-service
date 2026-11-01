-- JWT 서명 키. JWT_PRIVATE_KEY가 없으면 auth-service가 처음 뜰 때 하나 만들어 두고, 재시작하거나 파드가 늘어도 이 키를 같이 쓴다.
-- 여러 파드가 동시에 처음 떠서 키가 둘 이상 생기면 id가 가장 작은 키를 쓴다.
CREATE TABLE signing_keys (
    id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    kid VARCHAR(64) NOT NULL,
    private_key TEXT NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT signing_keys_pk PRIMARY KEY (id),
    CONSTRAINT uk_signing_keys_kid UNIQUE (kid)
);
