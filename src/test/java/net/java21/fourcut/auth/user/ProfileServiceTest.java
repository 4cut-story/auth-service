package net.java21.fourcut.auth.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.java21.fourcut.auth.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileServiceTest {

  @Test
  @DisplayName("닉네임은 앞뒤 공백을 지우고, 비면 null, 20자를 넘거나 제어 문자가 있으면 400")
  void normalize() {
    assertThat(ProfileService.normalize(null)).isNull();
    assertThat(ProfileService.normalize("   ")).isNull();
    assertThat(ProfileService.normalize(" 마르코 ")).isEqualTo("마르코");
    assertThat(ProfileService.normalize("가".repeat(20))).hasSize(20);
    assertThatThrownBy(() -> ProfileService.normalize("가".repeat(21)))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> ProfileService.normalize("a\nb")).isInstanceOf(ApiException.class);
  }
}
