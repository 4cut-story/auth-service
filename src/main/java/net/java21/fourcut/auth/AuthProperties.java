package net.java21.fourcut.auth;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * auth-service 설정.
 *
 * @param publicBaseUrl 공개 주소. OAuth redirect_uri와 로그인 뒤 돌아갈 web 주소를 여기서 만든다 (요청 헤더의 scheme을 믿지 않는다)
 * @param accessTokenTtl Access Token 유효 시간 (15분)
 * @param refreshTokenTtl Refresh Token 유효 시간 (14일)
 * @param authCodeTtl 로그인 뒤 web에 넘기는 일회용 코드 유효 시간
 * @param jwtPrivateKey RS256 서명 개인키 (PKCS#8 PEM). 비어 있으면 DB에 한 번 만들어 둔 키를 쓴다
 * @param jwtKeyId 서명 키 ID (JWKS kid)
 * @param devLoginEnabled 개발용 로그인(공급자 없이 닉네임으로 로그인). local에서만 켠다
 * @param conversationUrl conversation-service 주소 (탈퇴 정리)
 * @param contentUrl content-service 주소 (탈퇴 정리)
 */
@ConfigurationProperties("auth")
public record AuthProperties(
    URI publicBaseUrl,
    @DefaultValue("15m") Duration accessTokenTtl,
    @DefaultValue("14d") Duration refreshTokenTtl,
    @DefaultValue("2m") Duration authCodeTtl,
    String jwtPrivateKey,
    String jwtKeyId,
    @DefaultValue("false") boolean devLoginEnabled,
    URI conversationUrl,
    URI contentUrl) {

  /** 공개 주소 아래 경로. 끝의 '/'는 하나로 맞춘다. */
  public String publicUrl(String path) {
    String base = publicBaseUrl.toString();
    return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + path;
  }
}
