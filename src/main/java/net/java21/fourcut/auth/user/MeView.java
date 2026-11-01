package net.java21.fourcut.auth.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** 내 정보: 프로필과 연결된 로그인 공급자. id는 공개 ID다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MeView(String id, String nickname, String avatarUrl, List<Provider> providers) {}
