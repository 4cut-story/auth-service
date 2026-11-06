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

/** Access Token(JWT, RS256, 15분). 클레임: sub(사용자 공개 ID), roles(권한 코드 배열). */
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

  public String issue(String userId, List<String> roles) {
    return issue(userId, roles, null, null);
  }

  /** 개인 액세스 토큰에서 바꾼 Access Token. {@code scope}(권한 범위)와 {@code pat}(토큰 공개 ID) 클레임이 더 붙는다. */
  public String issue(String userId, List<String> roles, String scope, String patId) {
    Instant now = clock.instant();
    JWTClaimsSet.Builder builder =
        new JWTClaimsSet.Builder()
            .issuer(properties.publicBaseUrl().toString())
            .subject(userId)
            .claim("roles", roles)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(properties.accessTokenTtl())))
            .jwtID(UUID.randomUUID().toString());
    if (scope != null) {
      builder.claim("scope", scope).claim("pat", patId);
    }
    JWTClaimsSet claims = builder.build();
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
