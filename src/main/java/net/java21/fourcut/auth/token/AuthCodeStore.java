package net.java21.fourcut.auth.token;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.java21.fourcut.auth.AuthProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 로그인 성공 뒤 web에 넘기는 일회용 코드. 브라우저 주소창에는 토큰 대신 이 코드만 지나가고, web 서버가 {@code POST /auth/token}으로 바꾼다. 유효
 * 시간이 짧고 한 번만 쓸 수 있다. MVP는 레플리카 하나라 메모리에 둔다.
 */
@Component
public class AuthCodeStore {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final Map<String, Entry> codes = new ConcurrentHashMap<>();
  private final AuthProperties properties;
  private final Clock clock;

  public AuthCodeStore(AuthProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public String issue(Long userId) {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    codes.put(code, new Entry(userId, clock.instant().plus(properties.authCodeTtl())));
    return code;
  }

  /** 코드를 꺼낸다. 한 번 꺼내면 지워진다. */
  public Optional<Long> consume(String code) {
    Entry entry = code == null ? null : codes.remove(code);
    if (entry == null || !entry.expiresAt().isAfter(clock.instant())) {
      return Optional.empty();
    }
    return Optional.of(entry.userId());
  }

  @Scheduled(fixedDelay = 60_000)
  void purgeExpired() {
    Instant now = clock.instant();
    codes.values().removeIf(e -> !e.expiresAt().isAfter(now));
  }

  int size() {
    return codes.size();
  }

  private record Entry(Long userId, Instant expiresAt) {}
}
