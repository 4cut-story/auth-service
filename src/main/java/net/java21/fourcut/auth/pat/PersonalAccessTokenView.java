package net.java21.fourcut.auth.pat;

import java.time.Instant;
import java.util.Locale;

/**
 * 토큰 목록 한 줄. 원문은 담지 않는다.
 *
 * @param id 토큰 공개 ID
 * @param name 이름 (어디에 쓰는지)
 * @param hint 토큰 앞부분 (어느 토큰인지 알아보는 용도)
 * @param scope 권한 범위 (publish 또는 sounds)
 * @param createdAt 발급 시각
 * @param expiresAt 만료 시각
 * @param lastUsedAt 마지막 사용 시각 (없으면 null)
 * @param revokedAt 폐기 시각 (없으면 null)
 */
public record PersonalAccessTokenView(
    String id,
    String name,
    String hint,
    String scope,
    Instant createdAt,
    Instant expiresAt,
    Instant lastUsedAt,
    Instant revokedAt) {

  static PersonalAccessTokenView of(PersonalAccessToken t) {
    return new PersonalAccessTokenView(
        t.getPublicId(),
        t.getName(),
        t.getTokenHint(),
        t.getScope().name().toLowerCase(Locale.ROOT),
        t.getCreatedAt(),
        t.getExpiresAt(),
        t.getLastUsedAt(),
        t.getRevokedAt());
  }
}
