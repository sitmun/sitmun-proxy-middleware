package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.proxy.middleware.test.fixtures.AuthorizationProxyFixtures.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.sitmun.proxy.middleware.service.RequestExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.ResponseEntity;

@SpringBootTest
@AutoConfigureTestDatabase
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("Generic http requests tests")
class ExecutionRequestExecutorServiceTest {

  @Autowired private RequestExecutorService requestExecutorService;

  @Autowired private HttpClientFactoryService httpClientFactoryService;

  private JacksonTester<Object> jsonTester;

  @BeforeAll
  void setup() {
    ObjectMapper objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
  }

  @AfterEach
  void clearInterceptors() {
    httpClientFactoryService.removeAllInterceptors();
  }

  /**
   * Public user access to the public WFS service.
   *
   * @throws Exception for unexpected failures
   */
  @Test
  @DisplayName("Request to a public WFS service")
  void publicWfs() throws Exception {
    ResponseEntity<Object> response = requestExecutorService.executeRequest("", wfsService(false));
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(Objects.requireNonNull(response.getHeaders().get("Content-Type")).get(0))
        .isEqualTo("application/json;charset=UTF-8");
    Object body = response.getBody();
    assertThat(body).isNotNull().isInstanceOf(byte[].class);
    String text = new String((byte[]) body, StandardCharsets.UTF_8);
    assertThat(jsonTester.parse(text)).extracting("totalFeatures").isEqualTo(273);
  }
}
