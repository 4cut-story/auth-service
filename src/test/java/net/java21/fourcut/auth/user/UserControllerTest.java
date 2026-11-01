package net.java21.fourcut.auth.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.java21.fourcut.auth.error.ApiException;
import net.java21.fourcut.auth.support.PublicId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserControllerTest {

  @Test
  @DisplayName("ids는 쉼표로 나누고 중복·빈 값을 빼며, 1~50개의 공개 ID여야 한다")
  void parseIds() {
    String a = PublicId.generate();
    assertThat(UserController.parseIds(a + ", " + a + ",")).containsExactly(a);
    assertThatThrownBy(() -> UserController.parseIds(",")).isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> UserController.parseIds("0193a1c2-7e5b-7c3d-9a2f-5b8e4d1c6a70"))
        .isInstanceOf(ApiException.class);
    StringBuilder many = new StringBuilder();
    for (int i = 0; i < 51; i++) {
      many.append(PublicId.generate()).append(',');
    }
    assertThatThrownBy(() -> UserController.parseIds(many.toString()))
        .isInstanceOf(ApiException.class);
  }
}
