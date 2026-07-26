package org.sitmun.proxy.middleware.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

class ProxyProblemResponsesTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @ParameterizedTest
  @CsvSource({
    "401, https://attacker.example/problems/session-expired, 'Sign in at attacker.example', https://sitmun.org/problems/proxy-unauthorized, Unauthorized",
    "403, https://sitmun.org.evil.example/problems/access-denied, 'Forbidden\rtitle', https://sitmun.org/problems/proxy-forbidden, Forbidden"
  })
  void hostileBackendProblemIdentityUsesLocalFallback(
      int status,
      String hostileType,
      String hostileTitle,
      String expectedType,
      String expectedTitle) {
    String body =
        """
        {
          "type": "%s",
          "title": "%s",
          "detail": "upstream secret",
          "instance": "https://internal.example/private"
        }
        """
            .formatted(hostileType, hostileTitle.replace("\r", "\\r"));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"private\"");
    var exception =
        HttpClientErrorException.create(
            HttpStatus.valueOf(status),
            hostileTitle,
            headers,
            body.getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8);

    var response = ProxyProblemResponses.backendAuthorizationFailure(exception, objectMapper);

    assertThat(response.getStatusCode().value()).isEqualTo(status);
    assertThat(response.getHeaders()).doesNotContainKey(HttpHeaders.WWW_AUTHENTICATE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getType()).isEqualTo(expectedType);
    assertThat(response.getBody().getTitle()).isEqualTo(expectedTitle);
    assertThat(response.getBody().getInstance()).isEqualTo("/proxy");
    assertThat(response.getBody().getProperties()).containsEntry("origin", "backend-config");
    assertThat(response.getBody().toString())
        .doesNotContain("attacker.example", "evil.example", "upstream secret", "internal.example");
  }
}
