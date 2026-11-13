-- 개인 액세스 토큰 권한 범위에 소리 검색(SOUNDS)을 더한다. SOUNDS 토큰으로는 배경음·효과음 검색 API만 부를 수 있다(web이 막는다).
ALTER TABLE personal_access_tokens DROP CONSTRAINT ck_personal_access_tokens_scope;
ALTER TABLE personal_access_tokens ADD CONSTRAINT ck_personal_access_tokens_scope CHECK (scope IN ('PUBLISH', 'SOUNDS'));
