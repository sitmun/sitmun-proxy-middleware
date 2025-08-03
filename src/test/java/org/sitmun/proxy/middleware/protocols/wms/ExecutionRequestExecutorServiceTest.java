package org.sitmun.proxy.middleware.protocols.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.proxy.middleware.test.fixtures.AuthorizationProxyFixtures.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.sitmun.proxy.middleware.protocols.http.HttpClientFactoryService;
import org.sitmun.proxy.middleware.service.RequestExecutorService;
import org.sitmun.proxy.middleware.test.interceptors.CheckBasicAuthorization;
import org.sitmun.proxy.middleware.test.interceptors.DoNotRequest;
import org.sitmun.proxy.middleware.test.interceptors.HostnameCheck;
import org.sitmun.proxy.middleware.test.interceptors.QueryCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.ResponseEntity;

@SpringBootTest
@AutoConfigureTestDatabase
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("WMS request tests")
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

  /** Public user access to the public WMS service. */
  @Test
  @DisplayName("Request to a public WMS service")
  void publicWms() {
    ResponseEntity<Object> response = requestExecutorService.executeRequest("", wmsService(false));
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(Objects.requireNonNull(response.getHeaders().get("Content-Type")).get(0))
        .isEqualTo("image/png");
  }

  /** Public user access to the public WMS service. */
  @Test
  @DisplayName("Request to a public WMS service with parameters in the URI")
  void publicWmsWithURI() {
    ResponseEntity<Object> response =
        requestExecutorService.executeRequest("", wmsServiceWithURIWithParameters());
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(Objects.requireNonNull(response.getHeaders().get("Content-Type")).get(0))
        .isEqualTo("image/png");
  }

  /** Public user access to a private WMS service with basic authentication. */
  @Test
  @DisplayName("Basic Authentication added to service")
  void privateWmsBasicAuthentication() {
    CheckBasicAuthorization interceptor = new CheckBasicAuthorization();
    httpClientFactoryService.addInterceptors(interceptor, DoNotRequest.INSTANCE);
    requestExecutorService.executeRequest("", wmsService(true));
    assertThat(interceptor.getExpectation()).isEqualTo("userServ:passwordServ");
  }

  /** Public user access to a private WMS service with an IP on a private network. */
  @Test
  @DisplayName("Request to service with an IP instead of a Hostname")
  void privateWmsIpPrivateRed() {
    HostnameCheck interceptor = new HostnameCheck();
    httpClientFactoryService.addInterceptors(interceptor, DoNotRequest.INSTANCE);
    requestExecutorService.executeRequest("", wmsServiceWithIPasHostname());
    assertThat(interceptor.getExpectation()).isEqualTo("154.58.18.33");
  }

  /** Public user access to a private WMS service, adding a filter to the request. */
  @Test
  @DisplayName("Request to service with filters")
  void privateWfsWithFilter() {
    QueryCheck interceptor = new QueryCheck();
    httpClientFactoryService.addInterceptors(interceptor, DoNotRequest.INSTANCE);
    requestExecutorService.executeRequest("", wfsService(true));
    assertThat(interceptor.getExpectation())
        .isEqualToIgnoringCase(
            "REQUEST=GetFeature&VERSION=2.0.0&outputformat=application/json&SERVICE=WFS&CQL_FILTER=tr_05=5&typename=grid:gridp_250");
  }
}
