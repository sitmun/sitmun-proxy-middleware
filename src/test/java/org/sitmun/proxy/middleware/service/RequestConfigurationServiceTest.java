package org.sitmun.proxy.middleware.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.sitmun.proxy.middleware.config.ProxyMiddlewareConstants.PROXY_MIDDLEWARE_KEY;
import static org.sitmun.proxy.middleware.config.ProxyMiddlewareConstants.TYPE_SQL;
import static org.sitmun.proxy.middleware.config.ProxyMiddlewareConstants.TYPE_WMS;
import static org.sitmun.proxy.middleware.config.ProxyMiddlewareConstants.TYPE_WMTS;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.proxy.middleware.dto.ConfigProxyDto;
import org.sitmun.proxy.middleware.dto.ConfigProxyRequestDto;
import org.sitmun.proxy.middleware.dto.ProblemDetail;
import org.sitmun.proxy.middleware.protocols.wms.WmsPayloadDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestConfigurationService tests")
class RequestConfigurationServiceTest {

  @Mock private RestTemplate restTemplate;
  @Mock private RequestExecutorService requestExecutorService;

  @InjectMocks private RequestConfigurationService requestConfigurationService;

  private static final String CONFIG_URL = "http://test-config-url.com/api/config";
  private static final String SECRET = "test-secret-key";
  private static final String TEST_URL = "http://test-service.com/api";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(requestConfigurationService, "configUrl", CONFIG_URL);
    ReflectionTestUtils.setField(requestConfigurationService, "secret", SECRET);
  }

  @Test
  @DisplayName("Should successfully process request with valid configuration")
  void shouldSuccessfullyProcessRequestWithValidConfiguration() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = TYPE_WMS;
    Integer typeId = 3;
    String token = "test-token";
    Map<String, String> params = new HashMap<>();
    params.put("param1", "value1");

    ConfigProxyDto configProxyDto = createValidConfigProxyDto();
    ResponseEntity<ConfigProxyDto> configResponse = ResponseEntity.ok(configProxyDto);

    ResponseEntity<Object> executorResponse = ResponseEntity.ok("success");

    when(restTemplate.exchange(
            eq(CONFIG_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConfigProxyDto.class)))
        .thenReturn(configResponse);

    when(requestExecutorService.executeRequest(TEST_URL, configProxyDto.getPayload()))
        .thenReturn(executorResponse);

    // When
    ResponseEntity<?> result =
        requestConfigurationService.doRequest(
            appId, terId, type, typeId, token, params, TEST_URL, null);

    // Then
    assertThat(result).isEqualTo(executorResponse);
  }

  @Test
  @DisplayName("Should return 401 error when configuration response body is null")
  void shouldReturn401ErrorWhenConfigurationResponseBodyIsNull() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = TYPE_WMS;
    Integer typeId = 3;
    String token = "test-token";
    Map<String, String> params = new HashMap<>();

    ResponseEntity<ConfigProxyDto> configResponse = ResponseEntity.ok(null);

    when(restTemplate.exchange(
            eq(CONFIG_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConfigProxyDto.class)))
        .thenReturn(configResponse);

    // When
    ResponseEntity<?> result =
        requestConfigurationService.doRequest(
            appId, terId, type, typeId, token, params, TEST_URL, null);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(result.getBody()).isInstanceOf(ProblemDetail.class);

    ProblemDetail problemDetail = (ProblemDetail) result.getBody();
    Assertions.assertNotNull(problemDetail);
    assertThat(problemDetail.getStatus()).isEqualTo(401);
    assertThat(problemDetail.getTitle()).isEqualTo("Unauthorized");
    assertThat(problemDetail.getDetail()).isEqualTo("Request not valid");
    assertThat(problemDetail.getInstance()).isEqualTo(CONFIG_URL);
  }

  @Test
  @DisplayName("Should return original response when configuration request returns non-200 status")
  void shouldReturnOriginalResponseWhenConfigurationRequestReturnsNon200Status() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = TYPE_WMS;
    Integer typeId = 3;
    String token = "test-token";
    Map<String, String> params = new HashMap<>();

    ResponseEntity<ConfigProxyDto> configResponse =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);

    when(restTemplate.exchange(
            eq(CONFIG_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConfigProxyDto.class)))
        .thenReturn(configResponse);

    // When
    ResponseEntity<?> result =
        requestConfigurationService.doRequest(
            appId, terId, type, typeId, token, params, TEST_URL, null);

    // Then
    assertThat(result).isEqualTo(configResponse);
  }

  @Test
  @DisplayName("Should handle HttpClientErrorException and return error response")
  void shouldHandleHttpClientErrorExceptionAndReturnErrorResponse() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = TYPE_WMS;
    Integer typeId = 3;
    String token = "test-token";
    Map<String, String> params = new HashMap<>();

    HttpClientErrorException exception =
        new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");

    when(restTemplate.exchange(
            eq(CONFIG_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConfigProxyDto.class)))
        .thenThrow(exception);

    // When
    ResponseEntity<?> result =
        requestConfigurationService.doRequest(
            appId, terId, type, typeId, token, params, TEST_URL, null);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(result.getBody()).isInstanceOf(ProblemDetail.class);

    ProblemDetail problemDetail = (ProblemDetail) result.getBody();
    Assertions.assertNotNull(problemDetail);
    assertThat(problemDetail.getStatus()).isEqualTo(400);
    assertThat(problemDetail.getTitle()).isEqualTo("Backend Error");
    assertThat(problemDetail.getDetail()).isEqualTo("400 Bad Request");
    assertThat(problemDetail.getInstance()).isEqualTo(CONFIG_URL);
  }

  @Test
  @DisplayName("Should handle general exception and return 500 error response")
  void shouldHandleGeneralExceptionAndReturn500ErrorResponse() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = TYPE_WMS;
    Integer typeId = 3;
    String token = "test-token";
    Map<String, String> params = new HashMap<>();

    RuntimeException exception = new RuntimeException("Network error");

    when(restTemplate.exchange(
            eq(CONFIG_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConfigProxyDto.class)))
        .thenThrow(exception);

    // When
    ResponseEntity<?> result =
        requestConfigurationService.doRequest(
            appId, terId, type, typeId, token, params, TEST_URL, null);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(result.getBody()).isInstanceOf(ProblemDetail.class);

    ProblemDetail problemDetail = (ProblemDetail) result.getBody();
    Assertions.assertNotNull(problemDetail);
    assertThat(problemDetail.getStatus()).isEqualTo(500);
    assertThat(problemDetail.getTitle()).isEqualTo("Proxy Configuration Error");
    assertThat(problemDetail.getDetail()).isEqualTo("Network error");
    assertThat(problemDetail.getInstance()).isEqualTo(CONFIG_URL);
  }

  @Test
  @DisplayName("Should create correct ConfigProxyRequestDto with all parameters")
  void shouldCreateCorrectConfigProxyRequestDtoWithAllParameters() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = TYPE_WMS;
    Integer typeId = 3;
    String token = "test-token";
    Map<String, String> params = new HashMap<>();
    params.put("param1", "value1");
    params.put("param2", "value2");

    ConfigProxyDto configProxyDto = createValidConfigProxyDto();
    ResponseEntity<ConfigProxyDto> configResponse = ResponseEntity.ok(configProxyDto);
    ResponseEntity<Object> executorResponse = ResponseEntity.ok("success");

    when(restTemplate.exchange(
            eq(CONFIG_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConfigProxyDto.class)))
        .thenReturn(configResponse);

    when(requestExecutorService.executeRequest(TEST_URL, configProxyDto.getPayload()))
        .thenReturn(executorResponse);

    // When
    ResponseEntity<?> result =
        requestConfigurationService.doRequest(
            appId, terId, type, typeId, token, params, TEST_URL, null);

    // Then
    assertThat(result).isEqualTo(executorResponse);
  }

  @Test
  @DisplayName("Should handle null parameters map")
  void shouldHandleNullParametersMap() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = TYPE_WMS;
    Integer typeId = 3;
    String token = "test-token";

    ConfigProxyDto configProxyDto = createValidConfigProxyDto();
    ResponseEntity<ConfigProxyDto> configResponse = ResponseEntity.ok(configProxyDto);
    ResponseEntity<Object> executorResponse = ResponseEntity.ok("success");

    when(restTemplate.exchange(
            eq(CONFIG_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConfigProxyDto.class)))
        .thenReturn(configResponse);

    when(requestExecutorService.executeRequest(TEST_URL, configProxyDto.getPayload()))
        .thenReturn(executorResponse);

    // When
    ResponseEntity<?> result =
        requestConfigurationService.doRequest(
            appId, terId, type, typeId, token, null, TEST_URL, null);

    // Then
    assertThat(result).isEqualTo(executorResponse);
  }

  @Test
  @DisplayName("Should handle empty parameters map")
  void shouldHandleEmptyParametersMap() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = TYPE_WMS;
    Integer typeId = 3;
    String token = "test-token";
    Map<String, String> params = new HashMap<>();

    ConfigProxyDto configProxyDto = createValidConfigProxyDto();
    ResponseEntity<ConfigProxyDto> configResponse = ResponseEntity.ok(configProxyDto);
    ResponseEntity<Object> executorResponse = ResponseEntity.ok("success");

    when(restTemplate.exchange(
            eq(CONFIG_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConfigProxyDto.class)))
        .thenReturn(configResponse);

    when(requestExecutorService.executeRequest(TEST_URL, configProxyDto.getPayload()))
        .thenReturn(executorResponse);

    // When
    ResponseEntity<?> result =
        requestConfigurationService.doRequest(
            appId, terId, type, typeId, token, params, TEST_URL, null);

    // Then
    assertThat(result).isEqualTo(executorResponse);
  }

  @Test
  @DisplayName("Should successfully process POST request with body")
  void shouldSuccessfullyProcessPostRequestWithBody() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = TYPE_WMS;
    Integer typeId = 3;
    String token = "test-token";
    Map<String, String> params = new HashMap<>();
    params.put("param1", "value1");
    String body = "{\"key\":\"value\",\"data\":\"test\"}";

    ConfigProxyDto configProxyDto = createValidConfigProxyDto();
    ResponseEntity<ConfigProxyDto> configResponse = ResponseEntity.ok(configProxyDto);
    ResponseEntity<Object> executorResponse = ResponseEntity.ok("success");

    when(restTemplate.exchange(
            eq(CONFIG_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConfigProxyDto.class)))
        .thenReturn(configResponse);

    when(requestExecutorService.executeRequest(TEST_URL, configProxyDto.getPayload()))
        .thenReturn(executorResponse);

    // When
    ResponseEntity<?> result =
        requestConfigurationService.doRequest(
            appId, terId, type, typeId, token, params, TEST_URL, body);

    // Then
    assertThat(result).isEqualTo(executorResponse);
  }

  @ParameterizedTest
  @DisplayName("Should process multiple kind of bodies in POST requests")
  @CsvSource({
    "request body content",
    "{}", // Null value
    "''",
    """
      {"key":"value"}
    """,
    """
      <request>data</request>
    """
  })
  void shouldProcessMultipleKindOfBodiesInPostRequests(String body) {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = TYPE_WMS;
    Integer typeId = 3;
    String token = "test-token";
    Map<String, String> params = new HashMap<>();

    ConfigProxyDto configProxyDto = createValidConfigProxyDto();
    ResponseEntity<ConfigProxyDto> configResponse = ResponseEntity.ok(configProxyDto);
    ResponseEntity<Object> executorResponse = ResponseEntity.ok("success");

    when(restTemplate.exchange(
            eq(CONFIG_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConfigProxyDto.class)))
        .thenReturn(configResponse);

    when(requestExecutorService.executeRequest(TEST_URL, configProxyDto.getPayload()))
        .thenReturn(executorResponse);

    // When
    ResponseEntity<?> result =
        requestConfigurationService.doRequest(
            appId, terId, type, typeId, token, params, TEST_URL, body);

    // Then
    assertThat(result).isEqualTo(executorResponse);
  }

  @Test
  @DisplayName("Should handle error response with body in request")
  void shouldHandleErrorResponseWithBodyInRequest() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = TYPE_WMS;
    Integer typeId = 3;
    String token = "test-token";
    Map<String, String> params = new HashMap<>();
    String body = "{\"error\":\"test\"}";

    HttpClientErrorException exception =
        new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");

    when(restTemplate.exchange(
            eq(CONFIG_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConfigProxyDto.class)))
        .thenThrow(exception);

    // When
    ResponseEntity<?> result =
        requestConfigurationService.doRequest(
            appId, terId, type, typeId, token, params, TEST_URL, body);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(result.getBody()).isInstanceOf(ProblemDetail.class);

    ProblemDetail problemDetail = (ProblemDetail) result.getBody();
    Assertions.assertNotNull(problemDetail);
    assertThat(problemDetail.getStatus()).isEqualTo(400);
    assertThat(problemDetail.getTitle()).isEqualTo("Backend Error");
    assertThat(problemDetail.getDetail()).isEqualTo("400 Bad Request");
    assertThat(problemDetail.getInstance()).isEqualTo(CONFIG_URL);
  }

  private ConfigProxyDto createValidConfigProxyDto() {
    WmsPayloadDto wmsPayload =
        WmsPayloadDto.builder()
            .method("GET")
            .parameters(new HashMap<>())
            .uri("https://test-wms-service.com/wms")
            .build();

    return ConfigProxyDto.builder()
        .type(TYPE_WMS)
        .exp(System.currentTimeMillis() + 3600000) // 1 hour from now
        .payload(wmsPayload)
        .build();
  }

  @Test
  @DisplayName(
      "Request DTO includes all required fields for validateUserAccess (appId, terId, type, typeId, token)")
  void requestDtoIncludesAllRequiredFieldsForValidation() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = TYPE_WMS;
    Integer typeId = 100;
    String token = "test-jwt-token";
    Map<String, String> params = new HashMap<>();
    params.put("LAYERS", "test:layer");

    ConfigProxyDto configProxyDto = createValidConfigProxyDto();
    ResponseEntity<ConfigProxyDto> configResponse = ResponseEntity.ok(configProxyDto);

    when(restTemplate.exchange(
            eq(CONFIG_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConfigProxyDto.class)))
        .thenReturn(configResponse);

    when(requestExecutorService.executeRequest(TEST_URL, configProxyDto.getPayload()))
        .thenReturn(ResponseEntity.ok("success"));

    // When
    requestConfigurationService.doRequest(
        appId, terId, type, typeId, token, params, TEST_URL, null);

    // Then: Verify the request sent to backend includes all required fields
    var httpEntityCaptor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(
            eq(CONFIG_URL),
            eq(HttpMethod.POST),
            httpEntityCaptor.capture(),
            eq(ConfigProxyDto.class));

    @SuppressWarnings("unchecked")
    HttpEntity<ConfigProxyRequestDto> capturedEntity = httpEntityCaptor.getValue();
    ConfigProxyRequestDto requestDto = capturedEntity.getBody();

    assertThat(requestDto).isNotNull();
    assertThat(requestDto.getAppId()).isEqualTo(appId);
    assertThat(requestDto.getTerId()).isEqualTo(terId);
    assertThat(requestDto.getType()).isEqualTo(type);
    assertThat(requestDto.getTypeId()).isEqualTo(typeId);
    assertThat(requestDto.getToken()).isEqualTo(token);
    assertThat(requestDto.getParameters()).containsEntry("LAYERS", "test:layer");
  }

  @Test
  @DisplayName("Request includes X-SITMUN-Proxy-Key header for backend authentication")
  void requestIncludesProxyKeyHeaderForBackendAuthentication() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = TYPE_SQL;
    Integer typeId = 23;
    String token = "user-token";
    Map<String, String> params = new HashMap<>();

    ConfigProxyDto configProxyDto = createValidConfigProxyDto();
    ResponseEntity<ConfigProxyDto> configResponse = ResponseEntity.ok(configProxyDto);

    when(restTemplate.exchange(
            eq(CONFIG_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConfigProxyDto.class)))
        .thenReturn(configResponse);

    when(requestExecutorService.executeRequest(TEST_URL, configProxyDto.getPayload()))
        .thenReturn(ResponseEntity.ok("success"));

    // When
    requestConfigurationService.doRequest(
        appId, terId, type, typeId, token, params, TEST_URL, null);

    // Then: Verify the X-SITMUN-Proxy-Key header is present
    var httpEntityCaptor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(
            eq(CONFIG_URL),
            eq(HttpMethod.POST),
            httpEntityCaptor.capture(),
            eq(ConfigProxyDto.class));

    HttpEntity<?> capturedEntity = httpEntityCaptor.getValue();
    HttpHeaders headers = capturedEntity.getHeaders();

    assertThat(headers.containsKey(PROXY_MIDDLEWARE_KEY)).isTrue();
    assertThat(headers.getFirst(PROXY_MIDDLEWARE_KEY)).isEqualTo(SECRET);
  }

  @Test
  @DisplayName(
      "Request with null token includes all other fields for validateUserAccess (public user case)")
  void requestWithNullTokenIncludesOtherFieldsForValidation() {
    // Given
    Integer appId = 1;
    Integer terId = 0;
    String type = TYPE_WMTS;
    Integer typeId = 1;
    Map<String, String> params = new HashMap<>();

    ConfigProxyDto configProxyDto = createValidConfigProxyDto();
    ResponseEntity<ConfigProxyDto> configResponse = ResponseEntity.ok(configProxyDto);

    when(restTemplate.exchange(
            eq(CONFIG_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(ConfigProxyDto.class)))
        .thenReturn(configResponse);

    when(requestExecutorService.executeRequest(TEST_URL, configProxyDto.getPayload()))
        .thenReturn(ResponseEntity.ok("success"));

    // When
    requestConfigurationService.doRequest(appId, terId, type, typeId, null, params, TEST_URL, null);

    // Then: Verify all fields except token are present (token can be null for public users)
    var httpEntityCaptor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(
            eq(CONFIG_URL),
            eq(HttpMethod.POST),
            httpEntityCaptor.capture(),
            eq(ConfigProxyDto.class));

    @SuppressWarnings("unchecked")
    HttpEntity<ConfigProxyRequestDto> capturedEntity = httpEntityCaptor.getValue();
    ConfigProxyRequestDto requestDto = capturedEntity.getBody();

    assertThat(requestDto).isNotNull();
    assertThat(requestDto.getAppId()).isEqualTo(appId);
    assertThat(requestDto.getTerId()).isEqualTo(terId);
    assertThat(requestDto.getType()).isEqualTo(type);
    assertThat(requestDto.getTypeId()).isEqualTo(typeId);
    assertThat(requestDto.getToken()).isNull();
  }
}
