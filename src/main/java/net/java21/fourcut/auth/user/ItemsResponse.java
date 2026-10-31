package net.java21.fourcut.auth.user;

import java.util.List;

/** 페이지가 없는 짧은 목록도 객체로 감싼다 (docs api-conventions 4절). */
public record ItemsResponse<T>(List<T> items) {}
