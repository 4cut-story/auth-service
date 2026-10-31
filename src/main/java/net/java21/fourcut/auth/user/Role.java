package net.java21.fourcut.auth.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** 권한 코드 (ROLE_MEMBER, ROLE_ADMIN). 기본 데이터는 Flyway V1에 있다. */
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

  public static final String MEMBER = "ROLE_MEMBER";

  @Column(nullable = false, length = 30)
  private String code;

  @Column(nullable = false, length = 50)
  private String name;

  protected Role() {
    // JPA 전용
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }
}
