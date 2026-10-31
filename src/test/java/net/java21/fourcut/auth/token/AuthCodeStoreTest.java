package net.java21.fourcut.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.java21.fourcut.auth.AuthProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthCodeStoreTest {

  private final AtomicReference<Instant> now =
      new AtomicReference<>(Instant.parse("2026-10-31T00:00:00Z"));

  private final Clock clock =
      new Clock() {
        @Override
        public java.time.ZoneId getZone() {
          return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
          return this;
        }

        @Override
        public Instant instant() {
          return now.get();
        }
      };

  private final AuthCodeStore store =
      new AuthCodeStore(
          new AuthProperties(
              URI.create("https://x"),
              Duration.ofMinutes(15),
              Duration.ofDays(14),
              Duration.ofMinutes(2),
              null,
              null,
              false,
              URI.create("http://c"),
              URI.create("http://d")),
          clock);

  @Test
  @DisplayName("코드는 한 번만, 2분 안에만 쓸 수 있고, 지난 코드는 정리된다")
  void singleUseAndExpiry() {
    UUID user = UUID.randomUUID();
    String code = store.issue(user);
    assertThat(store.consume(code)).contains(user);
    assertThat(store.consume(code)).isEmpty();
    assertThat(store.consume(null)).isEmpty();

    String late = store.issue(user);
    now.set(now.get().plus(Duration.ofMinutes(3)));
    assertThat(store.consume(late)).isEmpty();

    store.issue(user);
    now.set(now.get().plus(Duration.ofMinutes(3)));
    store.purgeExpired();
    assertThat(store.size()).isZero();
  }
}
