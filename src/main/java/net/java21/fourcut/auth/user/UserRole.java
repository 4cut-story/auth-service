package net.java21.fourcut.auth.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** 회원과 권한의 N:M 연결. 가입하면 ROLE_MEMBER가 붙는다. */
@Entity
@Table(name = "user_roles")
public class UserRole extends BaseEntity {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "role_id", nullable = false)
  private UUID roleId;

  protected UserRole() {
    // JPA 전용
  }

  public UserRole(UUID userId, UUID roleId) {
    this.userId = userId;
    this.roleId = roleId;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getRoleId() {
    return roleId;
  }
}
