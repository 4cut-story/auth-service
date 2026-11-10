package net.java21.fourcut.auth.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import net.java21.fourcut.auth.support.PublicId;

/** 회원. 사용자 식별은 (provider, provider_user_id)인 {@link SocialAccount}로 한다. */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

  /** 표시 이름 최대 길이. */
  public static final int MAX_DISPLAY_NAME = 20;

  /** 계정 상태. 탈퇴 중에는 로그인할 수 없다. */
  public enum Status {
    ACTIVE,
    DELETING
  }

  /** 밖에 보이는 사용자 ID. JWT sub, X-User-Id, 다른 서비스의 owner_id가 이 값이다. */
  @Column(name = "public_id", nullable = false, updatable = false, length = PublicId.LENGTH)
  private String publicId = PublicId.generate();

  @Column(nullable = false, length = 30)
  private String nickname;

  /** 사용자가 정한 표시 이름. 없으면 화면은 nickname(로그인 아이디)을 쓴다. */
  @Column(name = "display_name", length = MAX_DISPLAY_NAME)
  private String displayName;

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

  public String getPublicId() {
    return publicId;
  }

  public String getNickname() {
    return nickname;
  }

  public String getDisplayName() {
    return displayName;
  }

  /** 표시 이름을 바꾼다. null이면 지워서 로그인 아이디로 돌아간다. 검증은 부르는 쪽이 한다. */
  public void changeDisplayName(String displayName) {
    this.displayName = displayName;
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
