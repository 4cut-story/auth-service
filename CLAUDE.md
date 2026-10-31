# auth-service

회원(프로필, 탈퇴), GitHub·Google 로그인, JWT 발급·갱신, JWKS 공개. Spring Boot + Spring Security OAuth2 Client.

## 기준
- 상위 원칙: `docs`의 `.specify/memory/constitution.md`
- 아키텍처: `docs`의 `docs/architecture.md` (v3.0)
- **계약 기준 버전: common `v0.1.0` (태그 예정)** — 계약은 이 버전에 고정해 쓰고, 올릴 때 이 줄을 함께 고친다.
- 포트: 8080 (local 8081) (`common/service-catalog.yaml`)

## 기술
Java 21, Spring Boot 4, Spring Security OAuth2 Client, Flyway, Spring Data JPA, Maven Wrapper, `common/parent-pom`.

## 규칙
- 서비스 간 통신은 `common/contracts`의 OpenAPI대로만 한다. API를 바꾸려면 `common` PR → 태그가 먼저다.
- 환경별 차이는 프로필과 환경 변수로만 둔다. 비밀 값은 커밋하지 않는다.
- 녹음과 전사 텍스트는 커밋하지 않는다.
- 테스트 이름은 한국어 `@DisplayName`(또는 테스트 설명)으로 쓰고 FR 번호와 연결한다.
