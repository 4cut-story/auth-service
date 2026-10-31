package net.java21.fourcut.auth.user;

import java.util.Locale;
import java.util.Optional;

/** 로그인 공급자. 추가만 하면 되게 둔다. */
public enum Provider {
  GITHUB,
  GOOGLE;

  /** OAuth registrationId(github, google)로 찾는다. */
  public static Optional<Provider> fromRegistrationId(String registrationId) {
    for (Provider p : values()) {
      if (p.registrationId().equals(registrationId)) {
        return Optional.of(p);
      }
    }
    return Optional.empty();
  }

  public String registrationId() {
    return name().toLowerCase(Locale.ROOT);
  }
}
