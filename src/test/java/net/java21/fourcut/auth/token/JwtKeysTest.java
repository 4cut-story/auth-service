package net.java21.fourcut.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtKeysTest {

  @Test
  @DisplayName("PKCS#8 PEM 개인키를 읽고 공개키를 만든다. kid가 없으면 thumbprint를 쓴다")
  void loadsPem() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    String pem =
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder()
                .encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----";

    RSAKey withId = JwtKeys.load(pem, "prod-1");
    RSAKey thumb = JwtKeys.load(pem, "");

    assertThat(withId.getKeyID()).isEqualTo("prod-1");
    assertThat(withId.isPrivate()).isTrue();
    assertThat(thumb.getKeyID()).isNotBlank();
  }

  @Test
  @DisplayName("형식이 틀린 JWT_PRIVATE_KEY면 시작하지 않는다")
  void badPem() {
    assertThatThrownBy(() -> JwtKeys.load("not a key", null))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("키 설정이 없으면 처음 한 번 만들어 DB에 두고, 재시작해도 같은 키를 쓴다")
  void storedKeySurvivesRestart() throws Exception {
    MemoryStore store = new MemoryStore();

    RSAKey first = JwtKeys.resolve(null, null, store);
    RSAKey afterRestart = JwtKeys.resolve(" ", null, store);

    assertThat(store.keys).hasSize(1);
    assertThat(first.getKeyID()).isNotBlank().isEqualTo(afterRestart.getKeyID());
    assertThat(afterRestart.toRSAPublicKey()).isEqualTo(first.toRSAPublicKey());
    assertThat(afterRestart.isPrivate()).isTrue();
  }

  @Test
  @DisplayName("파드 둘이 동시에 키를 만들면 먼저 저장된 키를 같이 쓴다")
  void concurrentStartUsesFirstKey() {
    MemoryStore store =
        new MemoryStore() {
          @Override
          public void add(String kid, String privateKeyPem) {
            if (keys.isEmpty()) {
              RSAKey other = JwtKeys.generate();
              super.add(other.getKeyID(), JwtKeys.toPem(other));
            }
            super.add(kid, privateKeyPem);
          }
        };

    RSAKey chosen = JwtKeys.resolve(null, null, store);

    assertThat(store.keys).hasSize(2);
    assertThat(chosen.getKeyID()).isEqualTo(store.keys.get(0).kid());
  }

  @Test
  @DisplayName("JWT_PRIVATE_KEY가 있으면 DB를 쓰지 않는다")
  void configuredKeyWins() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    String pem =
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder()
                .encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----";
    MemoryStore store = new MemoryStore();

    assertThat(JwtKeys.resolve(pem, "prod-1", store).getKeyID()).isEqualTo("prod-1");
    assertThat(store.keys).isEmpty();
  }

  private static class MemoryStore implements SigningKeyStore {
    final List<StoredKey> keys = new ArrayList<>();

    @Override
    public Optional<StoredKey> first() {
      return keys.stream().findFirst();
    }

    @Override
    public void add(String kid, String privateKeyPem) {
      keys.add(new StoredKey(kid, privateKeyPem));
    }
  }
}
