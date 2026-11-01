package net.java21.fourcut.auth.token;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** {@code signing_keys} 테이블 (V3). */
@Repository
class JdbcSigningKeyStore implements SigningKeyStore {

  private final JdbcClient jdbc;

  JdbcSigningKeyStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<StoredKey> first() {
    return jdbc.sql("SELECT kid, private_key FROM signing_keys ORDER BY id LIMIT 1")
        .query((rs, row) -> new StoredKey(rs.getString("kid"), rs.getString("private_key")))
        .optional();
  }

  @Override
  public void add(String kid, String privateKeyPem) {
    jdbc.sql("INSERT INTO signing_keys (kid, private_key) VALUES (:kid, :pem)")
        .param("kid", kid)
        .param("pem", privateKeyPem)
        .update();
  }
}
