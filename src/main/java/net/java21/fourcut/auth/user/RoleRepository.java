package net.java21.fourcut.auth.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByCode(String code);

  @Query(
      "select r.code from Role r, UserRole ur where ur.roleId = r.id and ur.userId = ?1"
          + " order by r.code")
  List<String> findCodesByUserId(Long userId);
}
