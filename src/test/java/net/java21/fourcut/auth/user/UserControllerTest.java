package net.java21.fourcut.auth.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import net.java21.fourcut.auth.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserControllerTest {

  @Test
  @DisplayName("ids는 쉼표로 나누고 중복·빈 값을 빼며, 1~50개의 UUID여야 한다")
  void parseIds() {
    UUID a = UUID.randomUUID();
    assertThat(UserController.parseIds(a + ", " + a + ",")).containsExactly(a);
    assertThatThrownBy(() -> UserController.parseIds(",")).isInstanceOf(ApiException.class);
    StringBuilder many = new StringBuilder();
    for (int i = 0; i < 51; i++) {
      many.append(UUID.randomUUID()).append(',');
    }
    assertThatThrownBy(() -> UserController.parseIds(many.toString()))
        .isInstanceOf(ApiException.class);
  }
}
