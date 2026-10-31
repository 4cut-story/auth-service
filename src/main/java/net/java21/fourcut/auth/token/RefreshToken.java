package net.java21.fourcut.auth.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import net.java21.fourcut.auth.user.BaseEntity;

/** Refresh Token. 원문은 저장하지 않고 SHA-256 해시만 둔다. 같은 로그인에서 이어진 토큰은 family_id가 같다. */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  @Column(name = "family_id", nullable = false)
  private UUID familyId;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  protected RefreshToken() {
    // JPA 전용
  }

  public RefreshToken(UUID userId, String tokenHash, UUID familyId, Instant expiresAt) {
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.familyId = familyId;
    this.expiresAt = expiresAt;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getFamilyId() {
    return familyId;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public boolean usableAt(Instant now) {
    return usedAt == null && revokedAt == null && expiresAt.isAfter(now);
  }

  public void markUsed(Instant now) {
    this.usedAt = now;
  }
}
