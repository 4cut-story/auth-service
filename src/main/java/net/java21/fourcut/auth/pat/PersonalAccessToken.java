package net.java21.fourcut.auth.pat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import net.java21.fourcut.auth.support.PublicId;
import net.java21.fourcut.auth.user.BaseEntity;

/** 개인 액세스 토큰. 원문은 저장하지 않고 SHA-256 해시와 앞 몇 글자(목록 표시용)만 둔다. */
@Entity
@Table(name = "personal_access_tokens")
public class PersonalAccessToken extends BaseEntity {

  /** 권한 범위. 게시(대화 올리기·만화 만들기·게시물 올리기)와 소리 검색(배경음·효과음 검색 API 읽기). */
  public enum Scope {
    PUBLISH,
    SOUNDS
  }

  @Column(name = "public_id", nullable = false, updatable = false, length = PublicId.LENGTH)
  private String publicId = PublicId.generate();

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
  private String tokenHash;

  @Column(name = "token_hint", nullable = false, updatable = false, length = 12)
  private String tokenHint;

  @Enumerated(EnumType.STRING)
  @Column(name = "scope", nullable = false, updatable = false, length = 20)
  private Scope scope;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  protected PersonalAccessToken() {
    // JPA 전용
  }

  public PersonalAccessToken(
      Long userId,
      String name,
      String tokenHash,
      String tokenHint,
      Scope scope,
      Instant expiresAt) {
    this.userId = userId;
    this.name = name;
    this.tokenHash = tokenHash;
    this.tokenHint = tokenHint;
    this.scope = scope;
    this.expiresAt = expiresAt;
  }

  public String getPublicId() {
    return publicId;
  }

  public Long getUserId() {
    return userId;
  }

  public String getName() {
    return name;
  }

  public String getTokenHint() {
    return tokenHint;
  }

  public Scope getScope() {
    return scope;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public boolean activeAt(Instant now) {
    return revokedAt == null && expiresAt.isAfter(now);
  }

  public void markUsed(Instant now) {
    this.lastUsedAt = now;
  }

  public void revoke(Instant now) {
    if (revokedAt == null) {
      this.revokedAt = now;
    }
  }
}
