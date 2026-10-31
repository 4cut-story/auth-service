package net.java21.fourcut.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 회원, GitHub·Google 로그인, JWT 발급·갱신, JWKS 공개. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class AuthApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuthApplication.class, args);
  }
}
