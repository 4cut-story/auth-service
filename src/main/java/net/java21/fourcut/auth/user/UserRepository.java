package net.java21.fourcut.auth.user;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  boolean existsByNickname(String nickname);

  List<User> findByIdInAndStatus(Collection<UUID> ids, User.Status status);

  List<User> findByStatus(User.Status status);
}
