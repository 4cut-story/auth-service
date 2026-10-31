package net.java21.fourcut.auth.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoleRepository extends JpaRepository<Role, UUID> {

  Optional<Role> findByCode(String code);

  @Query(
      "select r.code from Role r, UserRole ur where ur.roleId = r.id and ur.userId = ?1"
          + " order by r.code")
  List<String> findCodesByUserId(UUID userId);
}
