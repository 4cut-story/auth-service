package net.java21.fourcut.auth.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** 로그인 공급자 계정. 한 회원이 GitHub·Google 계정을 함께 가질 수 있다. */
@Entity
@Table(name = "social_accounts")
public class SocialAccount extends BaseEntity {

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Provider provider;

  @Column(name = "provider_user_id", nullable = false, length = 100)
  private String providerUserId;

  @Column(length = 255)
  private String email;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  protected SocialAccount() {
    // JPA 전용
  }

  public SocialAccount(
      Long userId, Provider provider, String providerUserId, String email, boolean emailVerified) {
    this.userId = userId;
    this.provider = provider;
    this.providerUserId = providerUserId;
    this.email = email;
    this.emailVerified = emailVerified;
  }

  public Long getUserId() {
    return userId;
  }

  public Provider getProvider() {
    return provider;
  }

  public String getProviderUserId() {
    return providerUserId;
  }

  public String getEmail() {
    return email;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void updateEmail(String email, boolean emailVerified) {
    this.email = email;
    this.emailVerified = emailVerified;
  }
}
