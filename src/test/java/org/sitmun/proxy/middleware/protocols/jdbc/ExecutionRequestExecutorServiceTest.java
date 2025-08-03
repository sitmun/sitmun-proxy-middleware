package org.sitmun.proxy.middleware.protocols.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.proxy.middleware.test.fixtures.AuthorizationProxyFixtures.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.assertj.core.api.InstanceOfAssertFactories;
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
@DisplayName("Relational requests tests")
class ExecutionRequestExecutorServiceTest {

  @Autowired private RequestExecutorService requestExecutorService;

  @BeforeAll
  void setup() {
    ObjectMapper objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
  }

  /** Public user access to a relational service. */
  @Test
  @DisplayName("Request to a JDBC service")
  void jdbcAccess() {
    ResponseEntity<Object> response =
        requestExecutorService.executeRequest("", inMemoryH2Database(false));
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(Objects.requireNonNull(response.getHeaders().get("Content-Type")).get(0))
        .isEqualTo("application/json");
    Object body = response.getBody();
    assertThat(body).isNotNull().asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(35);
  }

  /** Public user access to a relational service filtered. */
  @Test
  @DisplayName("Request to a JDBC service with filters")
  @Disabled(
      "Redundant test: the test is identical to jdbcAccess because the SQL query is built on the Configuration and Authorization API")
  void jdbcAccessWithFilters() {
    ResponseEntity<Object> response =
        requestExecutorService.executeRequest("", inMemoryH2Database(false));
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(Objects.requireNonNull(response.getHeaders().get("Content-Type")).get(0))
        .isEqualTo("application/json");
    Object body = response.getBody();
    assertThat(body).isNotNull().asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(35);
  }
}
