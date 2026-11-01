-- 초기 권한. V1을 baseline으로 건너뛴 개발 공용 스키마에도 들어가도록 V1과 따로 둔다. 다시 실행해도 된다.
INSERT INTO roles (code, name) VALUES
    ('ROLE_MEMBER', '일반 사용자'),
    ('ROLE_ADMIN', '관리자')
ON CONFLICT (code) DO NOTHING;
