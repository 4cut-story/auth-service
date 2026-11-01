package net.java21.fourcut.auth.token;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * {@code signing_keys} 테이블 (V3). 시작할 때 바로 읽으므로 Flyway가 끝난 뒤에 만든다. JPA와 같은 스키마({@code DB_SCHEMA})를
 * 쓴다.
 */
@Repository
@DependsOnDatabaseInitialization
class JdbcSigningKeyStore implements SigningKeyStore {

  private final JdbcClient jdbc;
  private final String table;

  JdbcSigningKeyStore(JdbcClient jdbc, @Value("${spring.flyway.default-schema}") String schema) {
    this.jdbc = jdbc;
    this.table = "\"" + schema.replace("\"", "") + "\".signing_keys";
  }

  @Override
  public Optional<StoredKey> first() {
    return jdbc.sql("SELECT kid, private_key FROM " + table + " ORDER BY id LIMIT 1")
        .query((rs, row) -> new StoredKey(rs.getString("kid"), rs.getString("private_key")))
        .optional();
  }

  @Override
  public void add(String kid, String privateKeyPem) {
    jdbc.sql("INSERT INTO " + table + " (kid, private_key) VALUES (:kid, :pem)")
        .param("kid", kid)
        .param("pem", privateKeyPem)
        .update();
  }
}
