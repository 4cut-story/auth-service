package net.java21.fourcut.auth.pat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** 개인 액세스 토큰 감사 기록 한 줄 (발급·사용·폐기). */
@Entity
@Table(name = "personal_access_token_events")
public class PersonalAccessTokenEvent {

  /** 기록 종류. */
  public enum Type {
    ISSUED,
    USED,
    REVOKED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "token_id", nullable = false, updatable = false)
  private Long tokenId;

  @Enumerated(EnumType.STRING)
  @Column(name = "event", nullable = false, updatable = false, length = 20)
  private Type event;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected PersonalAccessTokenEvent() {
    // JPA 전용
  }

  public PersonalAccessTokenEvent(Long tokenId, Type event, Instant createdAt) {
    this.tokenId = tokenId;
    this.event = event;
    this.createdAt = createdAt;
  }

  public Long getTokenId() {
    return tokenId;
  }

  public Type getEvent() {
    return event;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
