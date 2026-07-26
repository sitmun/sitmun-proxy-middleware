package org.sitmun.proxy.middleware.protocols.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.proxy.middleware.test.fixtures.AuthorizationProxyFixtures.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sitmun.proxy.middleware.dto.ProblemDetail;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@SpringBootTest
@AutoConfigureTestDatabase
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("WMS request tests")
class ExecutionRequestExecutorServiceTest {

  @Autowired private RequestExecutorService requestExecutorService;

  @Autowired private HttpClientFactoryService httpClientFactoryService;

  @SuppressWarnings("unused")
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

  @ParameterizedTest
  @ValueSource(ints = {401, 403})
  @DisplayName("Upstream authorization failures remain sanitized through WMS response decoration")
  void upstreamAuthorizationFailureThroughDecoratorChain(int upstreamStatus) {
    httpClientFactoryService.addInterceptors(
        chain ->
            new Response.Builder()
                .code(upstreamStatus)
                .body(
                    ResponseBody.create(
                        "{\"detail\":\"upstream secret\"}",
                        MediaType.parse("application/problem+json")))
                .addHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"private\"")
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/problem+json")
                .protocol(Protocol.HTTP_2)
                .message("Upstream authorization failure")
                .request(chain.request())
                .build());
    var payload =
        WmsPayloadDto.builder()
            .method("GET")
            .uri("https://internal.example/geoserver/wms")
            .parameters(Map.of("REQUEST", "GetCapabilities", "SERVICE", "WMS"))
            .build();

    ResponseEntity<Object> response =
        requestExecutorService.executeRequest("https://proxy.example/proxy/1/2/WMS/3", payload);

    assertThat(response.getStatusCode().value()).isEqualTo(502);
    assertThat(response.getHeaders()).doesNotContainKey(HttpHeaders.WWW_AUTHENTICATE);
    assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
    var problem = (ProblemDetail) response.getBody();
    assertThat(problem.getType())
        .isEqualTo("https://sitmun.org/problems/proxy-upstream-auth-error");
    assertThat(problem.getStatus()).isEqualTo(502);
    assertThat(problem.getInstance()).isEqualTo("/proxy");
    assertThat(problem.getProperties()).containsEntry("origin", "upstream-service");
    assertThat(problem.toString()).doesNotContain("upstream secret", "internal.example");
  }
}
