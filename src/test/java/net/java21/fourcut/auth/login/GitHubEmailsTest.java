package net.java21.fourcut.auth.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GitHubEmailsTest {

  @Test
  @DisplayName("확인된 대표 이메일을 고르고, API가 실패하면 이메일 없이 간다")
  void primaryVerified() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    GitHubEmails emails = new GitHubEmails(builder);

    server
        .expect(requestTo("https://api.github.com/user/emails"))
        .andExpect(header("Authorization", "Bearer tok"))
        .andRespond(
            withSuccess(
                "[{\"email\":\"x@x.com\",\"primary\":false,\"verified\":true},"
                    + "{\"email\":\"p@x.com\",\"primary\":true,\"verified\":true}]",
                MediaType.APPLICATION_JSON));
    assertThat(emails.primaryVerified("tok")).contains("p@x.com");

    server.reset();
    server.expect(requestTo("https://api.github.com/user/emails")).andRespond(withServerError());
    assertThat(emails.primaryVerified("tok")).isEmpty();
  }
}
