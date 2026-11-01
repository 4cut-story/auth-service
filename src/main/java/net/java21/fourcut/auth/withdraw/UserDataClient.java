package net.java21.fourcut.auth.withdraw;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.DeleteExchange;

/** 다른 서비스의 탈퇴 정리 내부 API (002 contracts/internal-api.md). 멱등이라 다시 불러도 된다. */
public interface UserDataClient {

  @DeleteExchange("/internal/users/{userId}/data")
  void deleteUserData(@PathVariable String userId);
}
