package org.sitmun.proxy.middleware.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Date;
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
import org.sitmun.proxy.middleware.dto.ErrorResponseDto;
import org.sitmun.proxy.middleware.protocols.wms.WmsPayloadDto;
import org.springframework.http.HttpEntity;
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
    String type = "wms";
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
    String type = "wms";
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
    assertThat(result.getBody()).isInstanceOf(ErrorResponseDto.class);

    ErrorResponseDto errorResponse = (ErrorResponseDto) result.getBody();
    Assertions.assertNotNull(errorResponse);
    assertThat(errorResponse.getStatus()).isEqualTo(401);
    assertThat(errorResponse.getError()).isEqualTo("Bad Request");
    assertThat(errorResponse.getMessage()).isEqualTo("Request not valid");
    assertThat(errorResponse.getPath()).isEqualTo(CONFIG_URL);
    assertThat(errorResponse.getTimestamp()).isInstanceOf(Date.class);
  }

  @Test
  @DisplayName("Should return original response when configuration request returns non-200 status")
  void shouldReturnOriginalResponseWhenConfigurationRequestReturnsNon200Status() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = "wms";
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
    String type = "wms";
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
    assertThat(result.getBody()).isInstanceOf(ErrorResponseDto.class);

    ErrorResponseDto errorResponse = (ErrorResponseDto) result.getBody();
    Assertions.assertNotNull(errorResponse);
    assertThat(errorResponse.getStatus()).isEqualTo(400);
    assertThat(errorResponse.getMessage()).isEqualTo("400 Bad Request");
    assertThat(errorResponse.getPath()).isEqualTo(CONFIG_URL);
    assertThat(errorResponse.getTimestamp()).isInstanceOf(Date.class);
  }

  @Test
  @DisplayName("Should handle general exception and return 500 error response")
  void shouldHandleGeneralExceptionAndReturn500ErrorResponse() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = "wms";
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
    assertThat(result.getBody()).isInstanceOf(ErrorResponseDto.class);

    ErrorResponseDto errorResponse = (ErrorResponseDto) result.getBody();
    Assertions.assertNotNull(errorResponse);
    assertThat(errorResponse.getStatus()).isEqualTo(500);
    assertThat(errorResponse.getMessage()).isEqualTo("Network error");
    assertThat(errorResponse.getPath()).isEqualTo(CONFIG_URL);
    assertThat(errorResponse.getTimestamp()).isInstanceOf(Date.class);
  }

  @Test
  @DisplayName("Should create correct ConfigProxyRequestDto with all parameters")
  void shouldCreateCorrectConfigProxyRequestDtoWithAllParameters() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = "wms";
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
    String type = "wms";
    Integer typeId = 3;
    String token = "test-token";
    Map<String, String> params = null;

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
  @DisplayName("Should handle empty parameters map")
  void shouldHandleEmptyParametersMap() {
    // Given
    Integer appId = 1;
    Integer terId = 2;
    String type = "wms";
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
    String type = "wms";
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
    String type = "wms";
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
    String type = "wms";
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
    assertThat(result.getBody()).isInstanceOf(ErrorResponseDto.class);

    ErrorResponseDto errorResponse = (ErrorResponseDto) result.getBody();
    Assertions.assertNotNull(errorResponse);
    assertThat(errorResponse.getStatus()).isEqualTo(400);
    assertThat(errorResponse.getMessage()).isEqualTo("400 Bad Request");
    assertThat(errorResponse.getPath()).isEqualTo(CONFIG_URL);
    assertThat(errorResponse.getTimestamp()).isInstanceOf(Date.class);
  }

  private ConfigProxyDto createValidConfigProxyDto() {
    WmsPayloadDto wmsPayload =
        WmsPayloadDto.builder()
            .method("GET")
            .parameters(new HashMap<>())
            .uri("https://test-wms-service.com/wms")
            .build();

    return ConfigProxyDto.builder()
        .type("wms")
        .exp(System.currentTimeMillis() + 3600000) // 1 hour from now
        .payload(wmsPayload)
        .build();
  }
}
