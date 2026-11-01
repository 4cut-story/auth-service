package net.java21.fourcut.auth.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

  Optional<SocialAccount> findByProviderAndProviderUserId(Provider provider, String providerUserId);

  List<SocialAccount> findByUserId(Long userId);

  /** 다른 공급자에서 같은 이메일을 확인받은 계정 (자동 연결 후보). */
  @Query(
      "select s from SocialAccount s where s.emailVerified = true and lower(s.email) = lower(?1)"
          + " and s.provider <> ?2")
  List<SocialAccount> findVerifiedByEmailOnOtherProvider(String email, Provider provider);
}
