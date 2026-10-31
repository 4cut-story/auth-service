package net.java21.fourcut.auth.config;

import net.java21.fourcut.auth.login.ConfiguredProviderResolver;
import net.java21.fourcut.auth.login.LoginHandlers;
import net.java21.fourcut.auth.login.OAuthClientRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security는 공급자 로그인(OAuth2 Client) 흐름에만 쓴다. API 인증은 게이트웨이가 하고, 서비스는 {@code X-User-Id}만 본다.
 * 세션은 로그인 중 state 보관에만 생긴다.
 */
@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(
      HttpSecurity http, OAuthClientRegistrations registrations, LoginHandlers handlers) {
    // 세션은 공급자 로그인(GET 리다이렉트) 중에만 생긴다. 상태를 바꾸는 요청은 쿠키가 아닌 본문 토큰·게이트웨이 헤더로 인증하므로
    // CSRF 토큰 검사에서 뺀다.
    http.csrf(csrf -> csrf.ignoringRequestMatchers("/auth/**", "/api/**", "/internal/**"))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(RequestCacheConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    if (!registrations.enabled().isEmpty()) {
      http.oauth2Login(
          oauth ->
              oauth
                  .clientRegistrationRepository(registrations)
                  .authorizationEndpoint(
                      a ->
                          a.baseUri("/auth/login")
                              .authorizationRequestResolver(
                                  new ConfiguredProviderResolver(registrations, "/auth/login")))
                  .redirectionEndpoint(r -> r.baseUri("/auth/callback/*"))
                  .successHandler(handlers)
                  .failureHandler(handlers));
    }
    return http.build();
  }
}
