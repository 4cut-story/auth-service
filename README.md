# auth-service

**회원과 로그인을 맡아요. GitHub·Google 로그인, JWT 발급, 개인 액세스 토큰.**

[← 4cut story 소개](https://github.com/4cut-story) · [데모](https://4cut.java21.net) · [아키텍처](https://github.com/4cut-story/.github/blob/main/docs/architecture.md)

## 하는 일

- GitHub·Google OAuth 로그인 (`/auth/login/{provider}`), 로그인 뒤 일회용 코드를 토큰으로 바꾸기 (`/auth/token`)
- Access Token 발급과 갱신 (`/auth/refresh`), 로그아웃
- JWT 서명 공개키 공개 (`/.well-known/jwks.json`). api-gateway가 이 키로 토큰을 직접 확인해요.
- 내 프로필 조회·수정, 사용자 검색 (`/api/v1/me`, `/api/v1/users`)
- 개인 액세스 토큰(PAT) 발급·목록·삭제, PAT를 JWT로 바꾸기 (`/auth/pat/exchange`)
- 탈퇴. conversation-service·content-service에 그 사용자의 데이터를 지우라고 알려요.

## 시스템 안에서의 위치

```
web ──> api-gateway ──> auth-service ──> PostgreSQL
                             └──> conversation-service, content-service (탈퇴 시 데이터 정리)
```

## 기술

Java 21 · Spring Boot 4 · Spring Security OAuth2 Client · Spring Data JPA · Flyway · PostgreSQL

## 실행

```bash
DB_URL=jdbc:postgresql://localhost:5432/fourcut DB_USERNAME=... DB_PASSWORD=... \
AUTH_DEV_LOGIN_ENABLED=true \
./mvnw spring-boot:run
```

로컬 포트는 8081이에요. `AUTH_DEV_LOGIN_ENABLED=true`면 OAuth 앱 없이 개발용 로그인을 쓸 수 있어요. 실제 로그인에는 `GITHUB_CLIENT_ID`·`GOOGLE_CLIENT_ID` 등과 JWT 서명 키(`JWT_PRIVATE_KEY`, `JWT_KEY_ID`)가 필요해요.

## 검사

```bash
./mvnw verify          # 테스트, 정적 분석, 커버리지 80%
./mvnw spotless:apply  # 코드 스타일 고치기
```

## 자세한 문서

| 문서 | 내용 |
| --- | --- |
| [아키텍처](https://github.com/4cut-story/.github/blob/main/docs/architecture.md) | 로그인 흐름, 토큰과 세션, 서비스 간 통신 |
| [설계 결정](https://github.com/4cut-story/.github/blob/main/docs/design-decisions.md) | 고른 것과 버린 대안 |
| [common](https://github.com/4cut-story/common) | OpenAPI 계약, 빌드와 CI 기준 |
