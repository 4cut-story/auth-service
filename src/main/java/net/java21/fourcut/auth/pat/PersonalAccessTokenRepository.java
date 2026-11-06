package net.java21.fourcut.auth.pat;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalAccessTokenRepository extends JpaRepository<PersonalAccessToken, Long> {

  Optional<PersonalAccessToken> findByTokenHash(String tokenHash);

  Optional<PersonalAccessToken> findByPublicIdAndUserId(String publicId, Long userId);

  List<PersonalAccessToken> findByUserIdOrderByIdDesc(Long userId);
}
