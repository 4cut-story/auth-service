package net.java21.fourcut.auth.user;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.java21.fourcut.auth.error.ApiException;
import net.java21.fourcut.auth.error.ErrorCode;
import net.java21.fourcut.auth.web.HeaderNames;
import net.java21.fourcut.auth.withdraw.WithdrawalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 내 정보, 작성자 표시용 사용자 조회, 탈퇴. 사용자 ID는 게이트웨이가 붙인 {@code X-User-Id}만 믿는다. */
@RestController
public class UserController {

  private static final int MAX_IDS = 50;

  private final UserQueryService queries;
  private final WithdrawalService withdrawals;

  public UserController(UserQueryService queries, WithdrawalService withdrawals) {
    this.queries = queries;
    this.withdrawals = withdrawals;
  }

  @GetMapping("/api/v1/me")
  public MeView me(@RequestHeader(HeaderNames.USER_ID) UUID userId) {
    return queries.me(userId);
  }

  /** 탈퇴 (M-03). 계정을 탈퇴 처리 중으로 바꾸고, 다른 서비스 데이터를 지운 뒤 계정을 지운다. */
  @DeleteMapping("/api/v1/me")
  public ResponseEntity<Void> withdraw(@RequestHeader(HeaderNames.USER_ID) UUID userId) {
    withdrawals.request(userId);
    return ResponseEntity.accepted().build();
  }

  @GetMapping("/api/v1/users/{id}")
  public UserView user(@RequestHeader(HeaderNames.USER_ID) UUID viewer, @PathVariable UUID id) {
    return queries.user(id);
  }

  @GetMapping("/api/v1/users")
  public ItemsResponse<UserView> users(
      @RequestHeader(HeaderNames.USER_ID) UUID viewer, @RequestParam String ids) {
    List<UUID> parsed = parseIds(ids);
    return new ItemsResponse<>(queries.users(parsed));
  }

  static List<UUID> parseIds(String ids) {
    List<String> parts =
        Arrays.stream(ids.split(","))
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .distinct()
            .toList();
    if (parts.isEmpty() || parts.size() > MAX_IDS) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "ids는 1~50개여야 해요.");
    }
    try {
      return parts.stream().map(UUID::fromString).toList();
    } catch (IllegalArgumentException e) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "ids 형식이 올바르지 않아요.", e);
    }
  }
}
