package net.java21.fourcut.auth.config;

import java.time.Duration;
import net.java21.fourcut.auth.AuthProperties;
import net.java21.fourcut.auth.web.TraceIdForwarder;
import net.java21.fourcut.auth.withdraw.UserDataClient;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/** 서비스 간 호출 클라이언트 (HTTP Interface + RestClient). 연결 2초, 읽기 5초 (docs api-conventions 9절). */
@Configuration
public class ClientConfig {

  @Bean
  UserDataClient conversationUserData(RestClient.Builder builder, AuthProperties properties) {
    return client(builder, properties.conversationUrl().toString());
  }

  @Bean
  UserDataClient contentUserData(RestClient.Builder builder, AuthProperties properties) {
    return client(builder, properties.contentUrl().toString());
  }

  private static UserDataClient client(RestClient.Builder builder, String baseUrl) {
    RestClient restClient =
        builder
            .clone()
            .baseUrl(baseUrl)
            .requestFactory(
                ClientHttpRequestFactoryBuilder.detect()
                    .build(
                        HttpClientSettings.defaults()
                            .withConnectTimeout(Duration.ofSeconds(2))
                            .withReadTimeout(Duration.ofSeconds(5))))
            .requestInterceptor(new TraceIdForwarder())
            .build();
    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
        .build()
        .createClient(UserDataClient.class);
  }
}
