package net.java21.fourcut.auth.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** 회원. 사용자 식별은 (provider, provider_user_id)인 {@link SocialAccount}로 한다. */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

  /** 계정 상태. 탈퇴 중에는 로그인할 수 없다. */
  public enum Status {
    ACTIVE,
    DELETING
  }

  @Column(nullable = false, length = 30)
  private String nickname;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Column(length = 255)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.ACTIVE;

  protected User() {
    // JPA 전용
  }

  public User(String nickname, String avatarUrl, String email) {
    this.nickname = nickname;
    this.avatarUrl = avatarUrl;
    this.email = email;
  }

  public String getNickname() {
    return nickname;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public String getEmail() {
    return email;
  }

  public Status getStatus() {
    return status;
  }

  public boolean isDeleting() {
    return status == Status.DELETING;
  }

  public void markDeleting() {
    this.status = Status.DELETING;
  }

  /** 로그인 때 공급자의 최신 프로필 이미지·이메일로 비어 있는 값만 채운다. */
  public void fillProfile(String avatarUrl, String email) {
    if (this.avatarUrl == null) {
      this.avatarUrl = avatarUrl;
    }
    if (this.email == null) {
      this.email = email;
    }
  }
}
