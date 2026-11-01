package net.java21.fourcut.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
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
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import net.java21.fourcut.auth.support.PublicId;
import net.java21.fourcut.auth.user.UserRepository;
import net.java21.fourcut.auth.withdraw.UserDataClient;
import net.java21.fourcut.auth.withdraw.WithdrawalService;
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
import org.springframework.web.client.ResourceAccessException;

/** 로그인 → 토큰 → 내 정보 → 갱신 → 로그아웃 → 탈퇴 흐름. 같은 테스트가 REST Docs 스니펫을 만든다. */
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
class AuthFlowIntegrationTest {

  private static final FieldDescriptor[] ERROR =
      new FieldDescriptor[] {
        fieldWithPath("error.code").description("오류 코드"),
        fieldWithPath("error.message").description("사용자에게 보여도 되는 문장"),
        fieldWithPath("error.traceId").description("요청 추적 ID (X-Trace-Id)")
      };

  private static final FieldDescriptor[] TOKENS =
      new FieldDescriptor[] {
        fieldWithPath("accessToken").description("Access Token (JWT, RS256). 게이트웨이에 Bearer로 보낸다"),
        fieldWithPath("expiresIn").description("Access Token 유효 시간(초)"),
        fieldWithPath("refreshToken").description("Refresh Token. 한 번 쓰면 바뀐다(rotation)")
      };

  @Autowired private MockMvc mvc;
  @Autowired private UserRepository users;
  @Autowired private WithdrawalService withdrawals;

  @MockitoBean(name = "conversationUserData")
  private UserDataClient conversation;

  @MockitoBean(name = "contentUserData")
  private UserDataClient content;

  private String devLogin(String nickname) throws Exception {
    String body =
        mvc.perform(
                post("/auth/dev-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nickname\":\"" + nickname + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.code");
  }

  private String exchange(String code) throws Exception {
    return mvc.perform(
            post("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private static String subject(String tokens) {
    String access = JsonPath.read(tokens, "$.accessToken");
    String payload =
        new String(
            java.util.Base64.getUrlDecoder().decode(access.split("\\.")[1]),
            java.nio.charset.StandardCharsets.UTF_8);
    return JsonPath.read(payload, "$.sub");
  }

  @Test
  @DisplayName("M-01: 로그인 코드를 토큰으로 바꾸고, 내 정보와 사용자 목록을 본다")
  void loginAndProfile() throws Exception {
    String code = devLogin("민지");
    String tokens =
        mvc.perform(
                post("/auth/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"" + code + "\"}"))
            .andExpect(status().isOk())
            .andDo(
                document(
                    "auth-token",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(
                        fieldWithPath("code").description("로그인 뒤 web에 넘긴 일회용 코드 (2분, 한 번만)")),
                    responseFields(TOKENS)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String userId = subject(tokens);

    mvc.perform(get("/api/v1/me").header("X-User-Id", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value("민지"))
        .andExpect(jsonPath("$.providers[0]").value("GITHUB"))
        .andDo(
            document(
                "me",
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("X-User-Id").description("게이트웨이가 붙인 사용자 ID")),
                responseFields(
                    fieldWithPath("id").description("사용자 ID"),
                    fieldWithPath("nickname").description("닉네임"),
                    fieldWithPath("avatarUrl")
                        .description("프로필 이미지 주소")
                        .optional()
                        .type(JsonFieldType.STRING),
                    fieldWithPath("providers").description("연결된 로그인 공급자 (GITHUB, GOOGLE)"))));

    mvc.perform(
            get("/api/v1/users")
                .header("X-User-Id", userId)
                .param("ids", userId + "," + PublicId.generate()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andDo(
            document(
                "users",
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("ids").description("쉼표로 이은 사용자 ID (최대 50개). 없는 ID는 빠진다")),
                responseFields(
                    fieldWithPath("items[].id").description("사용자 ID"),
                    fieldWithPath("items[].nickname").description("닉네임"),
                    fieldWithPath("items[].avatarUrl")
                        .description("프로필 이미지 주소")
                        .optional()
                        .type(JsonFieldType.STRING))));

    mvc.perform(get("/api/v1/users/{id}", userId).header("X-User-Id", userId))
        .andExpect(status().isOk())
        .andDo(
            document(
                "user",
                preprocessResponse(prettyPrint()),
                pathParameters(parameterWithName("id").description("사용자 ID")),
                responseFields(
                    fieldWithPath("id").description("사용자 ID"),
                    fieldWithPath("nickname").description("닉네임"),
                    fieldWithPath("avatarUrl")
                        .description("프로필 이미지 주소")
                        .optional()
                        .type(JsonFieldType.STRING))));
  }

  @Test
  @DisplayName("Refresh Token은 쓸 때마다 바뀌고, 이미 쓴 토큰을 다시 쓰면 그 로그인의 토큰이 모두 폐기된다")
  void refreshRotationAndReuse() throws Exception {
    String first = exchange(devLogin("refresher"));
    String oldRefresh = JsonPath.read(first, "$.refreshToken");

    String second =
        mvc.perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + oldRefresh + "\"}"))
            .andExpect(status().isOk())
            .andDo(
                document(
                    "auth-refresh",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(fieldWithPath("refreshToken").description("지금 Refresh Token")),
                    responseFields(TOKENS)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String newRefresh = JsonPath.read(second, "$.refreshToken");
    assertThat(newRefresh).isNotEqualTo(oldRefresh);

    mvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + oldRefresh + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"))
        .andDo(
            document(
                "auth-refresh-invalid", preprocessResponse(prettyPrint()), responseFields(ERROR)));

    // 재사용이 감지되어 새 토큰도 폐기됐다
    mvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + newRefresh + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("로그아웃하면 Refresh Token을 더 쓸 수 없다 (없는 토큰도 204)")
  void logout() throws Exception {
    String tokens = exchange(devLogin("leaver"));
    String refresh = JsonPath.read(tokens, "$.refreshToken");
    mvc.perform(
            post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refresh + "\"}"))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "auth-logout",
                requestFields(fieldWithPath("refreshToken").description("폐기할 Refresh Token"))));
    mvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refresh + "\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"unknown\"}"))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("일회용 코드는 한 번만 쓸 수 있다 (400 INVALID_AUTH_CODE)")
  void authCodeIsSingleUse() throws Exception {
    String code = devLogin("once");
    exchange(code);
    mvc.perform(
            post("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_AUTH_CODE"))
        .andDo(
            document(
                "auth-token-invalid", preprocessResponse(prettyPrint()), responseFields(ERROR)));
  }

  @Test
  @DisplayName("JWKS로 공개키만 내보내고, 켜진 공급자 목록을 알려 준다")
  void jwksAndProviders() throws Exception {
    mvc.perform(get("/.well-known/jwks.json"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
        .andExpect(jsonPath("$.keys[0].d").doesNotExist())
        .andDo(document("jwks", preprocessResponse(prettyPrint())));
    mvc.perform(get("/auth/providers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0]").value("github"))
        .andDo(
            document(
                "auth-providers",
                preprocessResponse(prettyPrint()),
                responseFields(
                    fieldWithPath("items").description("켜진 로그인 공급자 (github, google)"),
                    fieldWithPath("devLogin").description("개발용 로그인 사용 여부 (운영은 false)"))));
  }

  @Test
  @DisplayName("설정된 공급자 로그인은 공급자로 302, redirect_uri는 공개 주소 기준. 설정 안 된 공급자는 404")
  void loginRedirect() throws Exception {
    mvc.perform(get("/auth/login/github"))
        .andExpect(status().isFound())
        .andExpect(
            header()
                .string(
                    "Location",
                    org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.startsWith(
                            "https://github.com/login/oauth/authorize"),
                        org.hamcrest.Matchers.containsString(
                            "redirect_uri=https://4cut.example.net/auth/callback/github"))))
        .andDo(document("auth-login"));
    mvc.perform(get("/auth/login/google"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
  }

  @Test
  @DisplayName("X-User-Id 없이 부르면 401, 형식이 틀린 ids는 400")
  void requiresUserHeader() throws Exception {
    mvc.perform(get("/api/v1/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    mvc.perform(get("/api/v1/users").header("X-User-Id", PublicId.generate()).param("ids", "nope"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    mvc.perform(
            get("/api/v1/users/{id}", PublicId.generate()).header("X-User-Id", PublicId.generate()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("M-03: 탈퇴하면 다른 서비스 데이터를 지우고 계정을 지운다. 실패하면 DELETING으로 남아 로그인할 수 없다")
  void withdraw() throws Exception {
    String userId = subject(exchange(devLogin("bye")));
    mvc.perform(delete("/api/v1/me").header("X-User-Id", userId))
        .andExpect(status().isAccepted())
        .andDo(
            document(
                "me-delete",
                requestHeaders(headerWithName("X-User-Id").description("게이트웨이가 붙인 사용자 ID"))));
    verify(conversation).deleteUserData(userId);
    verify(content).deleteUserData(userId);
    assertThat(users.findByPublicId(userId)).isEmpty();

    String failingCode = devLogin("stuck");
    String tokens = exchange(failingCode);
    String stuck = subject(tokens);
    doThrow(new ResourceAccessException("down")).when(content).deleteUserData(any());
    mvc.perform(delete("/api/v1/me").header("X-User-Id", stuck)).andExpect(status().isAccepted());
    assertThat(users.findByPublicId(stuck)).get().extracting(u -> u.isDeleting()).isEqualTo(true);

    mvc.perform(
            post("/auth/dev-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"stuck\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("ACCOUNT_DELETING"));

    doThrow(new RuntimeException("unused")).when(conversation).deleteUserData(PublicId.generate());
    org.mockito.Mockito.reset(content);
    withdrawals.retryPending();
    assertThat(users.findByPublicId(stuck)).isEmpty();
  }
}
