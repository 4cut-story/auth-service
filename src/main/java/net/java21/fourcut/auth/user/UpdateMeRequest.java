package net.java21.fourcut.auth.user;

/** 내 정보 수정. displayName이 비었거나 null이면 표시 이름을 지운다. */
public record UpdateMeRequest(String displayName) {}
