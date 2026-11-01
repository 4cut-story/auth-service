package net.java21.fourcut.auth.token;

import java.util.Optional;

/** 서명 키 저장소. 여러 파드와 재시작이 같은 키를 쓰게 한다. */
public interface SigningKeyStore {

  /** 가장 먼저 저장된 키. */
  Optional<StoredKey> first();

  void add(String kid, String privateKeyPem);

  /**
   * @param kid JWKS kid
   * @param privateKeyPem PKCS#8 PEM
   */
  record StoredKey(String kid, String privateKeyPem) {}
}
