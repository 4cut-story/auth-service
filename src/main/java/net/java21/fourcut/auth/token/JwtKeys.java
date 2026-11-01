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
import java.util.Optional;
import net.java21.fourcut.auth.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Access Token 서명 키 (RS256). {@code JWT_PRIVATE_KEY}(PKCS#8 PEM)가 있으면 그 키를 쓴다. 없으면 DB({@code
 * signing_keys})에 저장된 키를 쓰고, 그것도 없으면 처음 한 번 만들어 저장한다. 그래서 재시작하거나 파드가 늘어도 같은 키를 쓴다.
 */
@Component
public final class JwtKeys {

  private static final Logger log = LoggerFactory.getLogger(JwtKeys.class);

  private final RSAKey key;

  public JwtKeys(AuthProperties properties, SigningKeyStore store) {
    this.key = resolve(properties.jwtPrivateKey(), properties.jwtKeyId(), store);
  }

  RSAKey signingKey() {
    return key;
  }

  /** JWKS에 내보낼 공개키 목록. */
  public Map<String, Object> publicJwks() {
    return new JWKSet(key.toPublicJWK()).toJSONObject();
  }

  static RSAKey resolve(String pem, String keyId, SigningKeyStore store) {
    if (pem != null && !pem.isBlank()) {
      return load(pem, keyId);
    }
    Optional<SigningKeyStore.StoredKey> stored = store.first();
    if (stored.isEmpty()) {
      RSAKey created = generate();
      store.add(created.getKeyID(), toPem(created));
      log.info("서명 키를 새로 만들어 DB에 저장했다: kid={}", created.getKeyID());
      // 다른 파드가 동시에 만들었으면 먼저 저장된 키를 모두 같이 쓴다
      stored = store.first();
    }
    SigningKeyStore.StoredKey chosen = stored.orElseThrow();
    return load(chosen.privateKeyPem(), chosen.kid());
  }

  static RSAKey load(String pem, String keyId) {
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
   * 새 키의 kid는 키 지문이다. 키가 바뀌면 kid도 바뀌어야 게이트웨이가 모르는 kid를 보고 JWKS를 다시 받는다. 같은 kid로 키만 바뀌면 게이트웨이는 캐시한 옛
   * 키로 검사해 새 토큰을 모두 거절한다.
   */
  static RSAKey generate() {
    try {
      return new RSAKeyGenerator(2048)
          .keyUse(KeyUse.SIGNATURE)
          .keyIDFromThumbprint(true)
          .generate();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }

  static String toPem(RSAKey key) {
    try {
      return "-----BEGIN PRIVATE KEY-----\n"
          + Base64.getMimeEncoder().encodeToString(key.toRSAPrivateKey().getEncoded())
          + "\n-----END PRIVATE KEY-----\n";
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }
}
