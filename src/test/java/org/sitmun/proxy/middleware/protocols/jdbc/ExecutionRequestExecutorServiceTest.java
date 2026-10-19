package org.sitmun.proxy.middleware.protocols.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.proxy.middleware.test.fixtures.AuthorizationProxyFixtures.*;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.*;
import org.sitmun.proxy.middleware.service.RequestExecutorService;
import org.sitmun.proxy.middleware.service.UpstreamContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@SpringBootTest
@AutoConfigureTestDatabase
@DisplayName("Relational requests tests")
class ExecutionRequestExecutorServiceTest {

  @Autowired private RequestExecutorService requestExecutorService;

  @BeforeEach
  void bindRequest() {
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(new MockHttpServletRequest()));
  }

  @AfterEach
  void resetRequest() {
    RequestContextHolder.resetRequestAttributes();
  }

  /** Public user access to a relational service. */
  @Test
  @DisplayName("Request to a JDBC service")
  void jdbcAccess() {
    ResponseEntity<Object> response =
        requestExecutorService.executeRequest("", inMemoryH2Database(false));
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(UpstreamContentType.current()).isEqualTo("application/json");
    Object body = response.getBody();
    assertThat(body).isNotNull().asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(35);
  }
}
