package org.sitmun.proxy.middleware.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.sitmun.proxy.middleware.test.interceptors.CheckBasicAuthorization;
import org.sitmun.proxy.middleware.test.interceptors.DoNotRequest;
import org.sitmun.proxy.middleware.test.interceptors.HostnameCheck;
import org.sitmun.proxy.middleware.test.interceptors.QueryCheck;
import org.sitmun.proxy.middleware.test.services.TestClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.proxy.middleware.test.fixtures.AuthorizationProxyFixtures.*;


@SpringBootTest
@AutoConfigureTestDatabase
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("GlobalRequestService tests")
class GlobalRequestServiceTest {

  @Autowired
  private GlobalRequestService globalRequestService;

  @Autowired
  private TestClientService testClientService;

  private JacksonTester<Object> jsonTester;

  @BeforeAll
  public void setup() {
    ObjectMapper objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
  }

  @AfterEach
  public void clearInterceptors() {
    testClientService.removeAllInterceptors();
  }

  /**
   * Public user access to the public WMS service.
   */
  @Test
  @DisplayName("Request to a public WMS service")
  void publicWms() {
    ResponseEntity<Object> response = globalRequestService.executeRequest("", wmsService(false));
    assertThat(response.getStatusCodeValue()).isEqualTo(200);
    assertThat(Objects.requireNonNull(response.getHeaders().get("Content-Type")).get(0)).isEqualTo("image/png");
  }

  /**
   * Public user access to the public WFS service.
   *
   * @throws Exception for unexpected failures
   */
  @Test
  @DisplayName("Request to a public WFS service")
  void publicWfs() throws Exception {
    ResponseEntity<Object> response = globalRequestService.executeRequest("", wfsService(false));
    assertThat(response.getStatusCodeValue()).isEqualTo(200);
    assertThat(Objects.requireNonNull(response.getHeaders().get("Content-Type")).get(0)).isEqualTo("application/json;charset=UTF-8");
    Object body = response.getBody();
    assertThat(body).isNotNull().isInstanceOf(byte[].class);
    String text = new String((byte[]) body, StandardCharsets.UTF_8);
    assertThat(jsonTester.parse(text))
      .extracting("totalFeatures").isEqualTo(268);
  }

  /**
   * Public user access to a private WMS service with basic authentication.
   */
  @Test
  @DisplayName("Basic Authentication added to service")
  void privateWmsBasicAuthentication() {
    CheckBasicAuthorization interceptor = new CheckBasicAuthorization();
    testClientService.addInterceptors(interceptor, DoNotRequest.INSTANCE);
    globalRequestService.executeRequest("", wmsService(true));
    assertThat(interceptor.getExpectation()).isEqualTo("userServ:passwordServ");
  }

  /**
   * Public user access to a private WMS service with an IP on a private network.
   */
  @Test
  @DisplayName("Request to service with an IP instead of a Hostname")
  void privateWmsIpPrivateRed() {
    HostnameCheck interceptor = new HostnameCheck();
    testClientService.addInterceptors(interceptor, DoNotRequest.INSTANCE);
    globalRequestService.executeRequest("", wmsServiceWithIPasHostname());
    assertThat(interceptor.getExpectation()).isEqualTo("154.58.18.33");
  }

  /**
   * Public user access to a private WMS service, adding a filter to the
   * request.
   */
  @Test
  @DisplayName("Request to service with filters")
  void privateWfsWithFilter() {
    QueryCheck interceptor = new QueryCheck();
    testClientService.addInterceptors(interceptor, DoNotRequest.INSTANCE);
    globalRequestService.executeRequest("", wfsService(true));
    assertThat(interceptor.getExpectation()).isEqualTo("REQUEST=GetFeature&VERSION=2.0.0&outputformat=application/json&SERVICE=WFS&CQL_FILTER=tr_05=5&typename=grid:gridp_250");
  }

  /**
   * Public user access to a relational service.
   */
  @Test
  @DisplayName("Request to a JDBC service")
  void jdbcAccess() {
    ResponseEntity<Object> response = globalRequestService.executeRequest("", inMemoryH2Database(false));
    assertThat(response.getStatusCodeValue()).isEqualTo(200);
    assertThat(Objects.requireNonNull(response.getHeaders().get("Content-Type")).get(0)).isEqualTo("application/json");
    Object body = response.getBody();
    assertThat(body).isNotNull().isInstanceOf(List.class).asList().hasSize(35);
  }

  /**
   * Public user access to a relational service filtered.
   */
  @Test
  @DisplayName("Request to a JDBC service with filters")
  @Disabled("Redundant test: the test is identical to jdbcAccess because the SQL query is built on the Configuration and Authorization API")
  void jdbcAccessWithFilters() {
    ResponseEntity<Object> response = globalRequestService.executeRequest("", inMemoryH2Database(false));
    assertThat(response.getStatusCodeValue()).isEqualTo(200);
    assertThat(Objects.requireNonNull(response.getHeaders().get("Content-Type")).get(0)).isEqualTo("application/json");
    Object body = response.getBody();
    assertThat(body).isNotNull().isInstanceOf(List.class).asList().hasSize(35);
  }

}
