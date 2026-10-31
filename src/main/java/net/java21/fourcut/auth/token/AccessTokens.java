package net.java21.fourcut.auth.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import net.java21.fourcut.auth.AuthProperties;
import org.springframework.stereotype.Component;

/** Access Token(JWT, RS256, 15분). 클레임: sub(사용자 ID), roles(권한 코드 배열). */
@Component
public class AccessTokens {

  private final JwtKeys keys;
  private final AuthProperties properties;
  private final Clock clock;

  public AccessTokens(JwtKeys keys, AuthProperties properties, Clock clock) {
    this.keys = keys;
    this.properties = properties;
    this.clock = clock;
  }

  public String issue(UUID userId, List<String> roles) {
    Instant now = clock.instant();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(properties.publicBaseUrl().toString())
            .subject(userId.toString())
            .claim("roles", roles)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(properties.accessTokenTtl())))
            .jwtID(UUID.randomUUID().toString())
            .build();
    SignedJWT jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(keys.signingKey().getKeyID())
                .type(JOSEObjectType.JWT)
                .build(),
            claims);
    try {
      jwt.sign(new RSASSASigner(keys.signingKey()));
    } catch (JOSEException e) {
      throw new IllegalStateException("Access Token 서명 실패", e);
    }
    return jwt.serialize();
  }

  public long expiresInSeconds() {
    return properties.accessTokenTtl().toSeconds();
  }
}
