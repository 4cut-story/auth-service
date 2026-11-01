package net.java21.fourcut.auth.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  boolean existsByNickname(String nickname);

  Optional<User> findByPublicId(String publicId);

  List<User> findByPublicIdInAndStatus(Collection<String> publicIds, User.Status status);

  List<User> findByStatus(User.Status status);
}
