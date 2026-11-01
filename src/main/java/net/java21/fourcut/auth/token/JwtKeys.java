package net.java21.fourcut.auth.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import net.java21.fourcut.auth.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Access Token 서명 키 (RS256). {@code JWT_PRIVATE_KEY}(PKCS#8 PEM)가 있으면 그 키를, 없으면 시작할 때 임시 키를 만든다. 임시
 * 키는 재시작하면 바뀌므로 이전 Access Token은 무효가 되고 web이 Refresh Token으로 다시 받는다.
 */
@Component
public final class JwtKeys {

  private static final Logger log = LoggerFactory.getLogger(JwtKeys.class);

  private final RSAKey key;

  public JwtKeys(AuthProperties properties) {
    this.key = load(properties.jwtPrivateKey(), properties.jwtKeyId());
  }

  RSAKey signingKey() {
    return key;
  }

  /** JWKS에 내보낼 공개키 목록. */
  public Map<String, Object> publicJwks() {
    return new JWKSet(key.toPublicJWK()).toJSONObject();
  }

  static RSAKey load(String pem, String keyId) {
    if (pem == null || pem.isBlank()) {
      log.warn("JWT_PRIVATE_KEY가 없어 임시 서명 키를 만든다. 재시작하면 Access Token이 무효가 된다.");
      return generate();
    }
    try {
      String base64 = pem.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
      KeyFactory factory = KeyFactory.getInstance("RSA");
      RSAPrivateCrtKey privateKey =
          (RSAPrivateCrtKey)
              factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
      RSAPublicKey publicKey =
          (RSAPublicKey)
              factory.generatePublic(
                  new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
      RSAKey.Builder builder =
          new RSAKey.Builder(publicKey).privateKey(privateKey).keyUse(KeyUse.SIGNATURE);
      return keyId == null || keyId.isBlank()
          ? builder.keyIDFromThumbprint().build()
          : builder.keyID(keyId).build();
    } catch (GeneralSecurityException | IllegalArgumentException | JOSEException e) {
      throw new IllegalStateException("JWT_PRIVATE_KEY를 읽을 수 없음 (PKCS#8 PEM이어야 함)", e);
    }
  }

  /**
   * 임시 키의 kid는 키 지문이다. 재시작마다 kid가 바뀌어야 게이트웨이가 모르는 kid를 보고 JWKS를 다시 받는다. 같은 kid로 키만 바뀌면 게이트웨이는 캐시한 옛
   * 키로 검사해 새 토큰을 모두 거절한다.
   */
  private static RSAKey generate() {
    try {
      return new RSAKeyGenerator(2048)
          .keyUse(KeyUse.SIGNATURE)
          .keyIDFromThumbprint(true)
          .generate();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }
}
