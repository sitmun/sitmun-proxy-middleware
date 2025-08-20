package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.proxy.middleware.decorator.Context;

@ExtendWith(MockitoExtension.class)
@DisplayName("HttpRequestDecoratorAddEndpoint tests")
class HttpRequestDecoratorAddEndpointTest {

  private HttpRequestDecoratorAddEndpoint decorator;
  private HttpRequestExecutor requestExecutor;

  @Mock private HttpContext httpContext;
  @Mock private Context nonHttpContext;

  @BeforeEach
  void setUp() {
    decorator = new HttpRequestDecoratorAddEndpoint();
    requestExecutor = new HttpRequestExecutor("http://test.com", null);
  }

  @Test
  @DisplayName("Should accept when context is HttpContext with non-empty URI")
  void shouldAcceptWhenContextIsHttpContextWithNonEmptyUri() {
    // Given
    when(httpContext.getUri()).thenReturn("http://test.com/api");

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should not accept when context is HttpContext with null URI")
  void shouldNotAcceptWhenContextIsHttpContextWithNullUri() {
    // Given
    when(httpContext.getUri()).thenReturn(null);

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should not accept when context is HttpContext with empty URI")
  void shouldNotAcceptWhenContextIsHttpContextWithEmptyUri() {
    // Given
    when(httpContext.getUri()).thenReturn("");

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should not accept when context is not HttpContext")
  void shouldNotAcceptWhenContextIsNotHttpContext() {
    // When
    boolean result = decorator.accept(requestExecutor, nonHttpContext);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should set URL and parameters when adding behavior")
  void shouldSetUrlAndParametersWhenAddingBehavior() {
    // Given
    String uri = "http://test.com/api/endpoint";
    Map<String, String> parameters = new HashMap<>();
    parameters.put("param1", "value1");
    parameters.put("param2", "value2");

    when(httpContext.getUri()).thenReturn(uri);
    when(httpContext.getParameters()).thenReturn(parameters);

    // When
    decorator.addBehavior(requestExecutor, httpContext);

    // Then
    assertThat(requestExecutor.getUrl())
        .isEqualTo("http://test.com/api/endpoint?PARAM1=value1&PARAM2=value2");
    // Verify parameters are set by checking the describe output
    String description = requestExecutor.describe();
    assertThat(description).contains("param1=value1").contains("param2=value2");
  }

  @Test
  @DisplayName("Should set URL when adding behavior with null parameters")
  void shouldSetUrlWhenAddingBehaviorWithNullParameters() {
    // Given
    String uri = "http://test.com/api/endpoint";
    when(httpContext.getUri()).thenReturn(uri);
    when(httpContext.getParameters()).thenReturn(null);

    // When
    decorator.addBehavior(requestExecutor, httpContext);

    // Then
    assertThat(requestExecutor.getUrl()).isEqualTo(uri);
  }

  @Test
  @DisplayName("Should set URL when adding behavior with empty parameters")
  void shouldSetUrlWhenAddingBehaviorWithEmptyParameters() {
    // Given
    String uri = "http://test.com/api/endpoint";
    Map<String, String> parameters = new HashMap<>();
    when(httpContext.getUri()).thenReturn(uri);
    when(httpContext.getParameters()).thenReturn(parameters);

    // When
    decorator.addBehavior(requestExecutor, httpContext);

    // Then
    assertThat(requestExecutor.getUrl()).isEqualTo(uri);
  }

  @Test
  @DisplayName("Should handle complex URI with query parameters")
  void shouldHandleComplexUriWithQueryParameters() {
    // Given
    String uri = "https://api.example.com/v1/users/search?type=admin&active=true";
    Map<String, String> parameters = new HashMap<>();
    parameters.put("limit", "20");
    parameters.put("offset", "0");

    when(httpContext.getUri()).thenReturn(uri);
    when(httpContext.getParameters()).thenReturn(parameters);

    // When
    decorator.addBehavior(requestExecutor, httpContext);

    // Then
    assertThat(requestExecutor.getUrl()).contains("TYPE=admin");
    assertThat(requestExecutor.getUrl()).contains("ACTIVE=true");
    assertThat(requestExecutor.getUrl()).contains("LIMIT=20");
    assertThat(requestExecutor.getUrl()).contains("OFFSET=0");
    assertThat(requestExecutor.getUrl()).startsWith("https://api.example.com/v1/users/search");
  }

  @Test
  @DisplayName("Should handle URI with special characters")
  void shouldHandleUriWithSpecialCharacters() {
    // Given
    String uri = "https://api.example.com/search?query=test%20with%20spaces&filter=active";
    Map<String, String> parameters = new HashMap<>();
    parameters.put("sort", "name");

    when(httpContext.getUri()).thenReturn(uri);
    when(httpContext.getParameters()).thenReturn(parameters);

    // When
    decorator.addBehavior(requestExecutor, httpContext);

    // Then
    assertThat(requestExecutor.getUrl()).contains("QUERY=test%2520with%2520spaces");
    assertThat(requestExecutor.getUrl()).contains("FILTER=active");
    assertThat(requestExecutor.getUrl()).contains("SORT=name");
    assertThat(requestExecutor.getUrl()).startsWith("https://api.example.com/search");
  }
}
