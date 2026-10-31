package net.java21.fourcut.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPairGenerator;
import java.util.Base64;
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
  @DisplayName("키가 없으면 임시 키를 만들고, 형식이 틀리면 시작하지 않는다")
  void ephemeralOrFail() {
    assertThat(JwtKeys.load(null, null).getKeyID()).isEqualTo("ephemeral");
    assertThat(JwtKeys.load(" ", "k").getKeyID()).isEqualTo("k");
    assertThatThrownBy(() -> JwtKeys.load("not a key", null))
        .isInstanceOf(IllegalStateException.class);
  }
}
