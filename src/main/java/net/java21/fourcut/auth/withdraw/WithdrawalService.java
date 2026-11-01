package net.java21.fourcut.auth.withdraw;

import net.java21.fourcut.auth.error.ApiException;
import net.java21.fourcut.auth.token.TokenService;
import net.java21.fourcut.auth.user.User;
import net.java21.fourcut.auth.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

/**
 * 탈퇴 (M-03). 순서: 계정을 DELETING으로 바꾸고 토큰 폐기 → conversation 정리 → content 정리 → auth 데이터 물리 삭제. 중간에 실패하면
 * DELETING으로 남겨 두고 5분마다 다시 시도한다. DELETING 계정은 로그인할 수 없다.
 */
@Service
public class WithdrawalService {

  private static final Logger log = LoggerFactory.getLogger(WithdrawalService.class);

  private final UserRepository users;
  private final TokenService tokens;
  private final UserDataClient conversation;
  private final UserDataClient content;
  private final TransactionTemplate tx;

  public WithdrawalService(
      UserRepository users,
      TokenService tokens,
      @Qualifier("conversationUserData") UserDataClient conversation,
      @Qualifier("contentUserData") UserDataClient content,
      TransactionTemplate tx) {
    this.users = users;
    this.tokens = tokens;
    this.conversation = conversation;
    this.content = content;
    this.tx = tx;
  }

  /** 탈퇴 요청. userId는 공개 ID(X-User-Id)다. */
  public void request(String userId) {
    User user =
        tx.execute(
            status -> {
              User found = users.findByPublicId(userId).orElseThrow(ApiException::notFound);
              found.markDeleting();
              tokens.revokeAll(found.getId());
              return found;
            });
    complete(user);
  }

  /** 다른 서비스 데이터를 지우고 계정을 지운다. 실패하면 false (다음 재시도에서 이어서 한다). */
  public boolean complete(User user) {
    String userId = user.getPublicId();
    try {
      conversation.deleteUserData(userId);
      content.deleteUserData(userId);
    } catch (RestClientException e) {
      log.warn("탈퇴 정리 실패, 나중에 다시 시도: userId={} {}", userId, e.toString());
      return false;
    }
    tx.executeWithoutResult(status -> users.deleteById(user.getId()));
    return true;
  }

  @Scheduled(fixedDelayString = "${auth.withdrawal-retry-delay:5m}", initialDelayString = "1m")
  public void retryPending() {
    users.findByStatus(User.Status.DELETING).forEach(this::complete);
  }
}
