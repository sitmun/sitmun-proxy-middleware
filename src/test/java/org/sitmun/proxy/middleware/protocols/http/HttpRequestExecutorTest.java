package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.proxy.middleware.dto.ProblemDetail;
import org.sitmun.proxy.middleware.service.RequestExecutorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

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
  @DisplayName("Should use JSON content type from header for POST body")
  void shouldUseJsonContentTypeFromHeaderForPostBody() throws IOException {
    httpRequestExecutor.setUrl(TEST_URL);
    httpRequestExecutor.setHeader("Content-Type", "application/json");
    httpRequestExecutor.setBody("{\"a\":1}");

    byte[] responseBytes = "{\"ok\":true}".getBytes();
    when(response.body()).thenReturn(responseBody);
    when(responseBody.bytes()).thenReturn(responseBytes);
    when(response.code()).thenReturn(200);
    when(response.header("content-type")).thenReturn("application/json");
    when(httpClient.executeRequest(any(Request.class))).thenReturn(response);

    RequestExecutorResponse<?> result = httpRequestExecutor.execute();

    var requestCaptor = org.mockito.ArgumentCaptor.forClass(Request.class);
    verify(httpClient).executeRequest(requestCaptor.capture());
    assertThat(requestCaptor.getValue().body().contentType().toString())
        .contains("application/json");
    assertThat(result.asResponseEntity().getBody()).isEqualTo(responseBytes);
  }

  @Test
  @DisplayName("Should stream response body without buffering via bytes()")
  void shouldStreamResponseBodyWithoutBuffering() throws IOException {
    httpRequestExecutor.setUrl(TEST_URL);
    byte[] payload = "streamed-bytes".getBytes();
    when(responseBody.byteStream()).thenReturn(new java.io.ByteArrayInputStream(payload));
    when(response.body()).thenReturn(responseBody);
    when(httpClient.executeRequest(any(Request.class))).thenReturn(response);

    try (HttpRequestExecutor.StreamedHttpResponse streamed =
        httpRequestExecutor.executeStreaming()) {
      assertThat(streamed.bodyStream().readAllBytes()).isEqualTo(payload);
      verify(responseBody, never()).bytes();
    }
  }

  @Test
  @DisplayName("Should default POST content type to text/xml when header absent")
  void shouldDefaultPostContentTypeToTextXml() throws IOException {
    httpRequestExecutor.setUrl(TEST_URL);
    httpRequestExecutor.setBody("<xml/>");
    when(response.body()).thenReturn(responseBody);
    when(responseBody.bytes()).thenReturn(new byte[0]);
    when(response.code()).thenReturn(200);
    when(httpClient.executeRequest(any(Request.class))).thenReturn(response);

    httpRequestExecutor.execute();

    var requestCaptor = org.mockito.ArgumentCaptor.forClass(Request.class);
    verify(httpClient).executeRequest(requestCaptor.capture());
    assertThat(requestCaptor.getValue().body().contentType().toString()).contains("text/xml");
  }

  @Test
  @DisplayName("isForwardableResponseHeader strips hop-by-hop and auth headers")
  void isForwardableResponseHeaderStripsHopByHopAndAuth() {
    assertThat(HttpRequestExecutor.isForwardableResponseHeader("Content-Type")).isTrue();
    assertThat(HttpRequestExecutor.isForwardableResponseHeader("Authorization")).isFalse();
    assertThat(HttpRequestExecutor.isForwardableResponseHeader("Set-Cookie")).isFalse();
    assertThat(HttpRequestExecutor.isForwardableResponseHeader("Connection")).isFalse();
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
    assertThat(result.asResponseEntity().getStatusCode().value()).isEqualTo(503);
    assertThat(result.asResponseEntity().getHeaders().getFirst("content-type"))
        .isEqualTo("application/problem+json");

    Object body = result.asResponseEntity().getBody();
    assertThat(body).isInstanceOf(ProblemDetail.class);

    ProblemDetail problemDetail = (ProblemDetail) body;
    Assertions.assertNotNull(problemDetail);
    assertThat(problemDetail.getStatus()).isEqualTo(503);
    assertThat(problemDetail.getTitle()).isEqualTo("Service Error");
    assertThat(problemDetail.getDetail()).isEqualTo("Error with the request to final service");
  }

  @ParameterizedTest
  @ValueSource(ints = {401, 403})
  @DisplayName("Should sanitize upstream authorization failures as bad gateway")
  void shouldSanitizeUpstreamAuthorizationFailuresAsBadGateway(int upstreamStatus)
      throws IOException {
    // Given
    httpRequestExecutor.setUrl(TEST_URL);
    when(response.code()).thenReturn(upstreamStatus);
    lenient().when(response.body()).thenReturn(responseBody);
    lenient()
        .when(response.header(HttpHeaders.WWW_AUTHENTICATE))
        .thenReturn("Bearer realm=\"hostile\"");
    when(httpClient.executeRequest(any(Request.class))).thenReturn(response);

    // When
    ResponseEntity<?> result = httpRequestExecutor.execute().asResponseEntity();

    // Then
    assertThat(result.getStatusCode().value()).isEqualTo(502);
    assertThat(result.getHeaders().getContentType().toString())
        .isEqualTo("application/problem+json");
    assertThat(result.getHeaders()).doesNotContainKey(HttpHeaders.WWW_AUTHENTICATE);
    assertThat(result.getBody()).isInstanceOf(ProblemDetail.class);
    ProblemDetail problem = (ProblemDetail) result.getBody();
    assertThat(problem.getType())
        .isEqualTo("https://sitmun.org/problems/proxy-upstream-auth-error");
    assertThat(problem.getStatus()).isEqualTo(502);
    assertThat(problem.getTitle()).isEqualTo("Upstream Authorization Error");
    assertThat(problem.getDetail()).isEqualTo("The upstream service rejected proxy authorization");
    assertThat(problem.getInstance()).isEqualTo("/proxy");
    assertThat(problem.getProperties()).containsEntry("origin", "upstream-service");
    verify(response, never()).body();
    verify(responseBody, never()).bytes();
    verify(response, never()).header(HttpHeaders.WWW_AUTHENTICATE);
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
        .contains("query=test")
        .contains("limit=10")
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
        .contains("existing=value")
        .contains("query=test")
        .contains("limit=10")
        .startsWith("https://api.example.com/search");
  }

  @Test
  @DisplayName("Should merge addParameter entries with existing query parameters")
  void shouldMergeAddParameterWithExistingQueryParameters() {
    httpRequestExecutor = new HttpRequestExecutor("https://api.example.com", httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/search?existing=value");
    httpRequestExecutor.addParameter("query", "test");
    httpRequestExecutor.addParameter("limit", "10");

    assertThat(httpRequestExecutor.getUrl())
        .contains("existing=value")
        .contains("query=test")
        .contains("limit=10")
        .startsWith("https://api.example.com/search");
  }

  @Test
  @DisplayName("Should ignore addParameter with blank key or null value")
  void shouldIgnoreInvalidAddParameter() {
    httpRequestExecutor = new HttpRequestExecutor("https://api.example.com", httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/search");
    httpRequestExecutor.addParameter("", "x");
    httpRequestExecutor.addParameter("   ", "x");
    httpRequestExecutor.addParameter("k", null);
    httpRequestExecutor.addParameter("valid", "1");

    assertThat(httpRequestExecutor.getUrl()).contains("valid=1").doesNotContain("k=");
  }

  @Test
  @DisplayName("Last addParameter value wins for same key")
  void addParameterOverwritesSameKey() {
    httpRequestExecutor = new HttpRequestExecutor("https://api.example.com", httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/search");
    httpRequestExecutor.addParameter("k", "first");
    httpRequestExecutor.addParameter("k", "second");

    assertThat(httpRequestExecutor.getUrl()).contains("k=second").doesNotContain("k=first");
  }

  @Test
  @DisplayName("Should keep distinct parameter keys that differ only by case")
  void shouldKeepDistinctParameterKeysThatDifferOnlyByCase() {
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
    assertThat(result)
        .contains("param=value1")
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
    assertThat(result).contains("param=value1").startsWith("https://api.example.com/search");
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
        .contains("type=admin")
        .contains("active=true")
        .contains("limit=20")
        .contains("offset=0")
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
    assertThat(result).contains("query=test").startsWith("https://api.example.com:8080/search");
  }

  @Test
  @DisplayName("Should set and retrieve headers correctly")
  void shouldSetAndRetrieveHeadersCorrectly() {
    // Given
    httpRequestExecutor.setHeader(HttpSecurityConstants.HEADER_AUTHORIZATION, "Bearer token123");
    httpRequestExecutor.setHeader("Content-Type", "application/json");
    httpRequestExecutor.setHeader("Accept", "application/json");

    // When
    String description = httpRequestExecutor.describe();

    // Then
    assertThat(description)
        .contains("Authorization", "Content-Type", "Accept")
        .doesNotContain("token123", "application/json");
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
    assertThat(description).contains("param1", "param2").doesNotContain("value1", "value2");
  }

  @Test
  @DisplayName("Should provide descriptive string representation")
  void shouldProvideDescriptiveStringRepresentation() {
    // Given
    httpRequestExecutor.setUrl("https://api.example.com/test");
    httpRequestExecutor.setHeader(HttpSecurityConstants.HEADER_AUTHORIZATION, "Bearer token");
    Map<String, String> parameters = new HashMap<>();
    parameters.put("query", "test");
    httpRequestExecutor.setParameters(parameters);

    // When
    String description = httpRequestExecutor.describe();

    // Then
    assertThat(description)
        .contains("HttpRequest{")
        .contains("Authorization", "query")
        .doesNotContain(
            "https://api.example.com/test",
            "Bearer token",
            "query=test",
            "http://test-service.com");
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

  @Test
  @DisplayName("Should return expanded URL when all parameters map to template variables")
  void shouldReturnExpandedUrlWhenAllParametersMapToTemplateVariables() {
    // Given — early return when usedVariables.size() == parameters.size()
    String baseUrl = "https://api.example.com";
    httpRequestExecutor = new HttpRequestExecutor(baseUrl, httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/users/{id}");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("id", "42");
    httpRequestExecutor.setParameters(parameters);

    // When
    String result = httpRequestExecutor.getUrl();

    // Then
    assertThat(result).isEqualTo("https://api.example.com/users/42");
    assertThat(result).doesNotContain("?ID=").doesNotContain("&ID=");
  }

  @Test
  @DisplayName("Should merge non-template parameters when URL has template variables")
  void shouldMergeNonTemplateParametersWhenUrlHasTemplateVariables() {
    // Given
    String baseUrl = "https://api.example.com";
    httpRequestExecutor = new HttpRequestExecutor(baseUrl, httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/users/{id}");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("id", "42");
    parameters.put("limit", "10");
    httpRequestExecutor.setParameters(parameters);

    // When
    String result = httpRequestExecutor.getUrl();

    // Then
    assertThat(result)
        .contains("limit=10")
        .startsWith("https://api.example.com/users/42")
        .doesNotContain("?ID=")
        .doesNotContain("&ID=");
  }

  @Test
  @DisplayName("Should not re-append template-bound parameters as query when merging")
  void shouldNotReAppendTemplateBoundParametersAsQueryWhenMerging() {
    // Given
    String baseUrl = "https://api.example.com";
    httpRequestExecutor = new HttpRequestExecutor(baseUrl, httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/v1/{resource}/{id}");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("resource", "items");
    parameters.put("id", "7");
    parameters.put("offset", "0");
    httpRequestExecutor.setParameters(parameters);

    // When
    String result = httpRequestExecutor.getUrl();

    // Then
    assertThat(result)
        .contains("offset=0")
        .startsWith("https://api.example.com/v1/items/7")
        .doesNotContain("?RESOURCE=")
        .doesNotContain("&RESOURCE=")
        .doesNotContain("?ID=")
        .doesNotContain("&ID=");
  }

  @Test
  @DisplayName("Should merge template-expanded URL with existing query string")
  void shouldMergeTemplateExpandedUrlWithExistingQueryString() {
    // Given
    String baseUrl = "https://api.example.com";
    httpRequestExecutor = new HttpRequestExecutor(baseUrl, httpClient);
    httpRequestExecutor.setUrl("https://api.example.com/search/{term}?fmt=json");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("term", "alpha");
    parameters.put("page", "2");
    httpRequestExecutor.setParameters(parameters);

    // When
    String result = httpRequestExecutor.getUrl();

    // Then
    assertThat(result)
        .contains("fmt=json")
        .contains("page=2")
        .startsWith("https://api.example.com/search/alpha")
        .doesNotContain("?TERM=")
        .doesNotContain("&TERM=");
  }
}
