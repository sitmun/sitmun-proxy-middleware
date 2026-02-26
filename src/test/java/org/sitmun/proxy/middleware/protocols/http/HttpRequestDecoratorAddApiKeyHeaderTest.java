package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sitmun.proxy.middleware.protocols.http.HttpRequestDecoratorAddApiKeyHeader.API_KEY_HEADER;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.proxy.middleware.decorator.Context;

@ExtendWith(MockitoExtension.class)
@DisplayName("HttpRequestDecoratorAddApiKeyHeader tests")
class HttpRequestDecoratorAddApiKeyHeaderTest {

  private HttpRequestDecoratorAddApiKeyHeader decorator;
  private HttpRequestExecutor requestExecutor;

  @Mock private HttpContext httpContext;
  @Mock private HttpContextSecurity security;
  @Mock private Context nonHttpContext;

  @BeforeEach
  void setUp() {
    decorator = new HttpRequestDecoratorAddApiKeyHeader();
    requestExecutor = new HttpRequestExecutor("http://test.com", null);
  }

  @Test
  @DisplayName(
      "Should accept when context is HttpContext with security and headers containing X-API-Key")
  void shouldAcceptWhenContextHasApiKeyHeader() {
    // Given
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getHeaders()).thenReturn(Map.of(API_KEY_HEADER, "test-api-key"));

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isTrue();
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
  @DisplayName("Should not accept when context is HttpContext with null security")
  void shouldNotAcceptWhenSecurityIsNull() {
    // Given
    when(httpContext.getSecurity()).thenReturn(null);

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should not accept when getHeaders() is null")
  void shouldNotAcceptWhenHeadersIsNull() {
    // Given
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getHeaders()).thenReturn(null);

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should not accept when headers do not contain X-API-Key")
  void shouldNotAcceptWhenHeadersMissingApiKey() {
    // Given
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getHeaders()).thenReturn(Map.of("Other-Header", "value"));

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should add X-API-Key header when adding behavior")
  void shouldAddApiKeyHeaderWhenAddingBehavior() {
    // Given
    String apiKey = "test-api-key";
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getHeaders()).thenReturn(Map.of(API_KEY_HEADER, apiKey));

    // When
    decorator.addBehavior(requestExecutor, httpContext);

    // Then
    String description = requestExecutor.describe();
    assertThat(description).contains(API_KEY_HEADER).contains(apiKey);
  }
}
