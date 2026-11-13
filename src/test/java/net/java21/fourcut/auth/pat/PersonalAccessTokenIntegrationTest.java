package net.java21.fourcut.auth.pat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import net.java21.fourcut.auth.TestcontainersConfiguration;
import net.java21.fourcut.auth.support.PublicId;
import net.java21.fourcut.auth.withdraw.UserDataClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 개인 액세스 토큰: 발급 → 교환 → 목록 → 폐기. 같은 테스트가 REST Docs 스니펫을 만든다. */
@SpringBootTest(
    properties = {
      "AUTH_DEV_LOGIN_ENABLED=true",
      "GITHUB_CLIENT_ID=test-client",
      "GITHUB_CLIENT_SECRET=test-secret",
      "PUBLIC_BASE_URL=https://4cut.example.net"
    })
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Import(TestcontainersConfiguration.class)
class PersonalAccessTokenIntegrationTest {

  private static final FieldDescriptor[] INFO =
      new FieldDescriptor[] {
        fieldWithPath("id").description("토큰 ID"),
        fieldWithPath("name").description("이름"),
        fieldWithPath("hint").description("토큰 앞부분 (알아보기용)"),
        fieldWithPath("scope").description("권한 범위 (publish: 게시, sounds: 소리 검색 API)"),
        fieldWithPath("createdAt").description("발급 시각"),
        fieldWithPath("expiresAt").description("만료 시각"),
        fieldWithPath("lastUsedAt").description("마지막 사용 시각").optional().type(JsonFieldType.STRING),
        fieldWithPath("revokedAt").description("폐기 시각").optional().type(JsonFieldType.STRING)
      };

  @Autowired private MockMvc mvc;
  @Autowired private PersonalAccessTokenRepository tokens;
  @Autowired private PersonalAccessTokenEventRepository events;

  @MockitoBean(name = "conversationUserData")
  private UserDataClient conversation;

  @MockitoBean(name = "contentUserData")
  private UserDataClient content;

  private String login(String nickname) throws Exception {
    String code =
        JsonPath.read(
            mvc.perform(
                    post("/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + nickname + "\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.code");
    String pair =
        mvc.perform(
                post("/auth/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"" + code + "\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return claim(JsonPath.read(pair, "$.accessToken"), "$.sub");
  }

  private static <T> T claim(String jwt, String path) {
    String payload =
        new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
    return JsonPath.read(payload, path);
  }

  private String issue(String userId, String body) throws Exception {
    return mvc.perform(
            post("/api/v1/me/tokens")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andReturn()
        .getResponse()
        .getContentAsString(StandardCharsets.UTF_8);
  }

  private void exchangeFails(String raw) throws Exception {
    mvc.perform(
            post("/auth/pat/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + raw + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("INVALID_ACCESS_TOKEN"));
  }

  @Test
  @DisplayName("PAT-01: 토큰을 발급해 Access Token으로 바꾸고, 폐기하면 더는 못 쓴다")
  void issueExchangeRevoke() throws Exception {
    String userId = login("토큰작가");

    String issued =
        mvc.perform(
                post("/api/v1/me/tokens")
                    .header("X-User-Id", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"스토리 자동화\",\"expiresInDays\":30,\"scope\":\"publish\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.info.scope").value("publish"))
            .andDo(
                document(
                    "pat-issue",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(
                        fieldWithPath("name").description("이름 (어디에 쓰는지, 50자까지)"),
                        fieldWithPath("expiresInDays")
                            .description("유효 기간(일, 1~90). 비우면 30")
                            .optional(),
                        fieldWithPath("scope")
                            .description("권한 범위 (publish 또는 sounds). 비우면 publish")
                            .optional()
                            .type(JsonFieldType.STRING)),
                    responseFields(fieldWithPath("token").description("토큰 원문. 이 응답에서만 보인다"))
                        .andWithPrefix("info.", INFO)))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    String raw = JsonPath.read(issued, "$.token");
    String tokenId = JsonPath.read(issued, "$.info.id");
    assertThat(raw).startsWith("fcp_");
    assertThat((String) JsonPath.read(issued, "$.info.hint")).isEqualTo(raw.substring(0, 8));

    String exchanged =
        mvc.perform(
                post("/auth/pat/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"" + raw + "\"}"))
            .andExpect(status().isOk())
            .andDo(
                document(
                    "pat-exchange",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(fieldWithPath("token").description("개인 액세스 토큰 원문")),
                    responseFields(
                        fieldWithPath("accessToken")
                            .description("Access Token (JWT). scope=publish"),
                        fieldWithPath("expiresIn").description("유효 시간(초)"),
                        fieldWithPath("scope").description("권한 범위"))))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String access = JsonPath.read(exchanged, "$.accessToken");
    assertThat((String) claim(access, "$.sub")).isEqualTo(userId);
    assertThat((String) claim(access, "$.scope")).isEqualTo("publish");
    assertThat((String) claim(access, "$.pat")).isEqualTo(tokenId);
    List<String> roles = claim(access, "$.roles");
    assertThat(roles).containsExactly("ROLE_MEMBER");

    mvc.perform(get("/api/v1/me/tokens").header("X-User-Id", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(tokenId))
        .andExpect(jsonPath("$.items[0].lastUsedAt").isNotEmpty())
        .andDo(
            document(
                "pat-list",
                preprocessResponse(prettyPrint()),
                responseFields(fieldWithPath("items").description("내 토큰 (최근 발급 순)"))
                    .andWithPrefix("items[].", INFO)));

    // 남의 토큰은 폐기할 수 없다
    String other = login("남의작가");
    mvc.perform(delete("/api/v1/me/tokens/{id}", tokenId).header("X-User-Id", other))
        .andExpect(status().isNotFound());

    mvc.perform(delete("/api/v1/me/tokens/{id}", tokenId).header("X-User-Id", userId))
        .andExpect(status().isNoContent())
        .andDo(document("pat-revoke"));
    mvc.perform(delete("/api/v1/me/tokens/{id}", tokenId).header("X-User-Id", userId))
        .andExpect(status().isNoContent());

    exchangeFails(raw);

    Long id =
        tokens.findAll().stream()
            .filter(t -> t.getPublicId().equals(tokenId))
            .findFirst()
            .orElseThrow()
            .getId();
    assertThat(events.findByTokenIdOrderByIdAsc(id))
        .extracting(PersonalAccessTokenEvent::getEvent)
        .containsExactly(
            PersonalAccessTokenEvent.Type.ISSUED,
            PersonalAccessTokenEvent.Type.USED,
            PersonalAccessTokenEvent.Type.REVOKED);
  }

  @Test
  @DisplayName("PAT-03: 소리 검색 토큰을 발급하면 scope=sounds Access Token으로 바뀌고, 모르는 권한 범위는 거절한다")
  void soundsScope() throws Exception {
    String userId = login("소리작가");
    String issued = issue(userId, "{\"name\":\"소리 검색\",\"scope\":\"sounds\"}");
    assertThat((String) JsonPath.read(issued, "$.info.scope")).isEqualTo("sounds");
    String raw = JsonPath.read(issued, "$.token");
    String exchanged =
        mvc.perform(
                post("/auth/pat/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"" + raw + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scope").value("sounds"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat((String) claim(JsonPath.read(exchanged, "$.accessToken"), "$.scope"))
        .isEqualTo("sounds");
    assertThat(issue(userId, "{\"name\":\"a\",\"scope\":\"admin\"}")).contains("VALIDATION_FAILED");
    assertThat(issue(userId, "{\"name\":\"b\",\"scope\":\" \"}")).contains("publish");
  }

  @Test
  @DisplayName("PAT-02: 형식이 틀리거나 없는 토큰, 잘못된 만료일·이름, 6번째 토큰은 거절한다")
  void rejects() throws Exception {
    String userId = login("거절작가");
    exchangeFails("nope");
    exchangeFails("fcp_doesnotexist");
    assertThat(issue(userId, "{\"name\":\"a\",\"expiresInDays\":91}"))
        .contains("VALIDATION_FAILED");
    assertThat(issue(userId, "{\"name\":\"a\",\"expiresInDays\":0}")).contains("VALIDATION_FAILED");
    assertThat(issue(userId, "{\"name\":\"" + "가".repeat(51) + "\"}"))
        .contains("VALIDATION_FAILED");
    for (int i = 0; i < PersonalAccessTokenService.MAX_ACTIVE; i++) {
      assertThat(issue(userId, "{\"name\":\"t" + i + "\"}")).contains("fcp_");
    }
    assertThat(issue(userId, "{\"name\":\"over\"}")).contains("CONFLICT");

    mvc.perform(get("/api/v1/me/tokens").header("X-User-Id", PublicId.generate()))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/me/tokens").header("X-User-Id", "bad"))
        .andExpect(status().isUnauthorized());
    mvc.perform(delete("/api/v1/me/tokens/{id}", "bad!").header("X-User-Id", userId))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/v1/me/tokens/{id}", PublicId.generate()).header("X-User-Id", userId))
        .andExpect(status().isNotFound());
  }
}
