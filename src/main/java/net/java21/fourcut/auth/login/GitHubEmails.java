package net.java21.fourcut.auth.login;

import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** GitHub 이메일 목록 API에서 확인된(verified) 대표 이메일을 가져온다. 없거나 실패하면 이메일 없이 가입한다. */
@Component
public class GitHubEmails {

  private final RestClient client;

  public GitHubEmails(RestClient.Builder builder) {
    this.client = builder.clone().baseUrl("https://api.github.com").build();
  }

  public Optional<String> primaryVerified(String accessToken) {
    try {
      List<Email> emails =
          client
              .get()
              .uri("/user/emails")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
              .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
              .retrieve()
              .body(new org.springframework.core.ParameterizedTypeReference<List<Email>>() {});
      if (emails == null) {
        return Optional.empty();
      }
      return emails.stream().filter(e -> e.primary() && e.verified()).map(Email::email).findFirst();
    } catch (RestClientException e) {
      return Optional.empty();
    }
  }

  /** GitHub 이메일 항목. */
  public record Email(String email, boolean primary, boolean verified) {}
}
