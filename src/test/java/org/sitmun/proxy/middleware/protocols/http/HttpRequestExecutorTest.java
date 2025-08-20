package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.proxy.middleware.dto.ErrorResponseDto;
import org.sitmun.proxy.middleware.service.RequestExecutorResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("HttpRequestExecutor tests")
class HttpRequestExecutorTest {

  @Mock private HttpClient httpClient;
  @Mock private Response response;
  @Mock private ResponseBody responseBody;

  private HttpRequestExecutor httpRequestExecutor;
  private static final String BASE_URL = "http://test-service.com";
  private static final String TEST_URL = "http://test-service.com/api/test";

  @BeforeEach
  void setUp() {
    httpRequestExecutor = new HttpRequestExecutor(BASE_URL, httpClient);
  }

  @Test
  @DisplayName("Should successfully execute request with response body")
  void shouldSuccessfullyExecuteRequestWithResponseBody() throws IOException {
    // Given
    httpRequestExecutor.setUrl(TEST_URL);
    httpRequestExecutor.setHeader("Content-Type", "application/json");

    byte[] responseBytes = "{\"status\":\"success\"}".getBytes();
    when(response.body()).thenReturn(responseBody);
    when(responseBody.bytes()).thenReturn(responseBytes);
    when(response.code()).thenReturn(200);
    when(response.header("content-type")).thenReturn("application/json");
    when(httpClient.executeRequest(any(Request.class))).thenReturn(response);

    // When
    RequestExecutorResponse<?> result = httpRequestExecutor.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.asResponseEntity().getStatusCode().value()).isEqualTo(200);
    assertThat(result.asResponseEntity().getHeaders().getFirst("content-type"))
        .isEqualTo("application/json");
    assertThat(result.asResponseEntity().getBody()).isEqualTo(responseBytes);
  }

  @Test
  @DisplayName("Should handle response with null body")
  void shouldHandleResponseWithNullBody() throws IOException {
    // Given
    httpRequestExecutor.setUrl(TEST_URL);
    when(response.body()).thenReturn(null);
    when(response.code()).thenReturn(204);
    when(response.header("content-type")).thenReturn("text/plain");
    when(httpClient.executeRequest(any(Request.class))).thenReturn(response);

    // When
    RequestExecutorResponse<?> result = httpRequestExecutor.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.asResponseEntity().getStatusCode().value()).isEqualTo(204);
    assertThat(result.asResponseEntity().getBody()).isNull();
  }

  @Test
  @DisplayName("Should handle IOException and return error response")
  void shouldHandleIOExceptionAndReturnErrorResponse() throws IOException {
    // Given
    httpRequestExecutor.setUrl(TEST_URL);
    when(httpClient.executeRequest(any(Request.class))).thenThrow(new IOException("Network error"));

    // When
    RequestExecutorResponse<?> result = httpRequestExecutor.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.asResponseEntity().getStatusCode().value()).isEqualTo(500);
    assertThat(result.asResponseEntity().getHeaders().getFirst("content-type"))
        .isEqualTo("application/json");

    Object body = result.asResponseEntity().getBody();
    assertThat(body).isInstanceOf(ErrorResponseDto.class);

    ErrorResponseDto errorResponse = (ErrorResponseDto) body;
    Assertions.assertNotNull(errorResponse);
    assertThat(errorResponse.getStatus()).isEqualTo(500);
    assertThat(errorResponse.getError()).isEqualTo("ServiceError");
    assertThat(errorResponse.getMessage()).isEqualTo("Error with the request to final service");
  }

  @Test
  @DisplayName("Should throw IllegalStateException when URL is not set")
  void shouldThrowIllegalStateExceptionWhenUrlIsNotSet() {
    // Given - URL not set

    // When & Then
    assertThatThrownBy(() -> httpRequestExecutor.execute())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Url is not set");
  }

  @Test
  @DisplayName("Should throw IllegalStateException when URL is empty")
  void shouldThrowIllegalStateExceptionWhenUrlIsEmpty() {
    // Given
    httpRequestExecutor.setUrl("");

    // When & Then
    assertThatThrownBy(() -> httpRequestExecutor.execute())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Url is not set");
  }

  @Test
  @DisplayName("Should throw IllegalStateException when URL is null")
  void shouldThrowIllegalStateExceptionWhenUrlIsNull() {
    // Given
    httpRequestExecutor.setUrl(null);

    // When & Then
    assertThatThrownBy(() -> httpRequestExecutor.execute())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Url is not set");
  }

  @Test
  @DisplayName("Should construct URL with parameters")
  void shouldConstructUrlWithParameters() {
    // Given
    String baseUrl = "https://api.example.com";
    httpRequestExecutor = new HttpRequestExecutor(baseUrl, httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/search");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("query", "test");
    parameters.put("limit", "10");
    httpRequestExecutor.setParameters(parameters);

    // When
    String result = httpRequestExecutor.getUrl();

    // Then
    assertThat(result)
        .contains("QUERY=test")
        .contains("LIMIT=10")
        .startsWith("https://api.example.com/search");
  }

  @Test
  @DisplayName("Should merge parameters with existing query parameters")
  void shouldMergeParametersWithExistingQueryParameters() {
    // Given
    String baseUrl = "https://api.example.com";
    httpRequestExecutor = new HttpRequestExecutor(baseUrl, httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/search?existing=value");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("query", "test");
    parameters.put("limit", "10");
    httpRequestExecutor.setParameters(parameters);

    // When
    String result = httpRequestExecutor.getUrl();

    // Then
    assertThat(result)
        .contains("EXISTING=value")
        .contains("QUERY=test")
        .contains("LIMIT=10")
        .startsWith("https://api.example.com/search");
  }

  @Test
  @DisplayName("Should handle case-insensitive parameter keys")
  void shouldHandleCaseInsensitiveParameterKeys() {
    // Given
    String baseUrl = "https://api.example.com";
    httpRequestExecutor = new HttpRequestExecutor(baseUrl, httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/search");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("param", "value1");
    parameters.put("PARAM", "value2");
    httpRequestExecutor.setParameters(parameters);

    // When
    String result = httpRequestExecutor.getUrl();

    // Then
    // The implementation converts all keys to uppercase
    assertThat(result)
        .contains("PARAM=value1")
        .contains("PARAM=value2")
        .startsWith("https://api.example.com/search");
  }

  @Test
  @DisplayName("Should not add duplicate parameter values")
  void shouldNotAddDuplicateParameterValues() {
    // Given
    String baseUrl = "https://api.example.com";
    httpRequestExecutor = new HttpRequestExecutor(baseUrl, httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/search?param=value1");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("param", "value1"); // Same value as existing
    httpRequestExecutor.setParameters(parameters);

    // When
    String result = httpRequestExecutor.getUrl();

    // Then
    // Should not add duplicate parameter with same value
    assertThat(result).contains("PARAM=value1").startsWith("https://api.example.com/search");
  }

  @Test
  @DisplayName("Should return original URL when no parameters are set")
  void shouldReturnOriginalUrlWhenNoParametersAreSet() {
    // Given
    String baseUrl = "https://api.example.com";
    httpRequestExecutor = new HttpRequestExecutor(baseUrl, httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/search");

    // When
    String result = httpRequestExecutor.getUrl();

    // Then
    assertThat(result).isEqualTo("https://api.example.com/search");
  }

  @Test
  @DisplayName("Should handle URL with complex path and query")
  void shouldHandleUrlWithComplexPathAndQuery() {
    // Given
    String baseUrl = "https://api.example.com";
    httpRequestExecutor = new HttpRequestExecutor(baseUrl, httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/v1/users/search?type=admin&active=true");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("limit", "20");
    parameters.put("offset", "0");
    httpRequestExecutor.setParameters(parameters);

    // When
    String result = httpRequestExecutor.getUrl();

    // Then
    assertThat(result)
        .contains("TYPE=admin")
        .contains("ACTIVE=true")
        .contains("LIMIT=20")
        .contains("OFFSET=0")
        .startsWith("https://api.example.com/v1/users/search");
  }

  @Test
  @DisplayName("Should handle URL with port number")
  void shouldHandleUrlWithPortNumber() {
    // Given
    String baseUrl = "https://api.example.com";
    httpRequestExecutor = new HttpRequestExecutor(baseUrl, httpClient);
    httpRequestExecutor.setUrl("https://api.example.com:8080/search");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("query", "test");
    httpRequestExecutor.setParameters(parameters);

    // When
    String result = httpRequestExecutor.getUrl();

    // Then
    assertThat(result).contains("QUERY=test").startsWith("https://api.example.com:8080/search");
  }

  @Test
  @DisplayName("Should set and retrieve headers correctly")
  void shouldSetAndRetrieveHeadersCorrectly() {
    // Given
    httpRequestExecutor.setHeader("Authorization", "Bearer token123");
    httpRequestExecutor.setHeader("Content-Type", "application/json");
    httpRequestExecutor.setHeader("Accept", "application/json");

    // When
    String description = httpRequestExecutor.describe();

    // Then
    assertThat(description)
        .contains("Authorization=Bearer token123")
        .contains("Content-Type=application/json")
        .contains("Accept=application/json");
  }

  @Test
  @DisplayName("Should set and retrieve parameters correctly")
  void shouldSetAndRetrieveParametersCorrectly() {
    // Given
    Map<String, String> parameters = new HashMap<>();
    parameters.put("param1", "value1");
    parameters.put("param2", "value2");
    httpRequestExecutor.setParameters(parameters);

    // When
    String description = httpRequestExecutor.describe();

    // Then
    assertThat(description).contains("param1=value1").contains("param2=value2");
  }

  @Test
  @DisplayName("Should provide descriptive string representation")
  void shouldProvideDescriptiveStringRepresentation() {
    // Given
    httpRequestExecutor.setUrl("https://api.example.com/test");
    httpRequestExecutor.setHeader("Authorization", "Bearer token");
    Map<String, String> parameters = new HashMap<>();
    parameters.put("query", "test");
    httpRequestExecutor.setParameters(parameters);

    // When
    String description = httpRequestExecutor.describe();

    // Then
    assertThat(description)
        .contains("HttpRequest{")
        .contains("url='https://api.example.com/test'")
        .contains("Authorization=Bearer token")
        .contains("query=test")
        .contains("baseUrl=http://test-service.com");
  }

  @Test
  @DisplayName("Should handle empty parameters map")
  void shouldHandleEmptyParametersMap() {
    // Given
    String baseUrl = "https://api.example.com";
    httpRequestExecutor = new HttpRequestExecutor(baseUrl, httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/search");

    Map<String, String> parameters = new HashMap<>();
    httpRequestExecutor.setParameters(parameters);

    // When
    String result = httpRequestExecutor.getUrl();

    // Then
    assertThat(result).isEqualTo("https://api.example.com/search");
  }

  @Test
  @DisplayName("Should handle null parameters map")
  void shouldHandleNullParametersMap() {
    // Given
    String baseUrl = "https://api.example.com";
    httpRequestExecutor = new HttpRequestExecutor(baseUrl, httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/search");

    httpRequestExecutor.setParameters(null);

    // When
    String result = httpRequestExecutor.getUrl();

    // Then
    assertThat(result).isEqualTo("https://api.example.com/search");
  }
}
