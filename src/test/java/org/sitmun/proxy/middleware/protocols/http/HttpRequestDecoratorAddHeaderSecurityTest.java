package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.HEADER_X_API_KEY;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.utils.logging.SensitiveDataMasking;

@ExtendWith(MockitoExtension.class)
@DisplayName("HttpRequestDecoratorAddHeaderSecurity tests")
class HttpRequestDecoratorAddHeaderSecurityTest {

  private HttpRequestDecoratorAddHeaderSecurity decorator;
  private HttpRequestExecutor requestExecutor;

  @Mock private HttpContext httpContext;
  @Mock private HttpContextSecurity security;
  @Mock private Context nonHttpContext;

  @BeforeEach
  void setUp() {
    decorator = new HttpRequestDecoratorAddHeaderSecurity();
    requestExecutor = new HttpRequestExecutor("http://test.com", null);
  }

  @Test
  @DisplayName("Should accept when context is HttpContext with security and non-empty headers")
  void shouldAcceptWhenContextHasHeaders() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getHeaders()).thenReturn(Map.of(HEADER_X_API_KEY, "test-api-key"));

    assertThat(decorator.accept(requestExecutor, httpContext)).isTrue();
  }

  @Test
  @DisplayName("Should accept for any non-empty header map (not limited to X-API-Key)")
  void shouldAcceptWhenHeadersAreNotApiKeyNamed() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getHeaders()).thenReturn(Map.of("Other-Header", "value"));

    assertThat(decorator.accept(requestExecutor, httpContext)).isTrue();
  }

  @Test
  @DisplayName("Should not accept when context is not HttpContext")
  void shouldNotAcceptWhenContextIsNotHttpContext() {
    assertThat(decorator.accept(requestExecutor, nonHttpContext)).isFalse();
  }

  @Test
  @DisplayName("Should not accept when context is HttpContext with null security")
  void shouldNotAcceptWhenSecurityIsNull() {
    when(httpContext.getSecurity()).thenReturn(null);

    assertThat(decorator.accept(requestExecutor, httpContext)).isFalse();
  }

  @Test
  @DisplayName("Should not accept when getHeaders() is null")
  void shouldNotAcceptWhenHeadersIsNull() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getHeaders()).thenReturn(null);

    assertThat(decorator.accept(requestExecutor, httpContext)).isFalse();
  }

  @Test
  @DisplayName("Should not accept when headers map is empty")
  void shouldNotAcceptWhenHeadersEmpty() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getHeaders()).thenReturn(Map.of());

    assertThat(decorator.accept(requestExecutor, httpContext)).isFalse();
  }

  @Test
  @DisplayName("Should add all configured headers when adding behavior")
  void shouldAddAllHeadersWhenAddingBehavior() {
    String apiKey = "test-api-key";
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getHeaders())
        .thenReturn(Map.of(HEADER_X_API_KEY, apiKey, "X-Other", "other-value"));

    decorator.addBehavior(requestExecutor, httpContext);

    assertThat(requestExecutor.getHeader(HEADER_X_API_KEY)).isEqualTo(apiKey);
    assertThat(requestExecutor.getHeader("X-Other")).isEqualTo("other-value");
    assertThat(requestExecutor.describe())
        .contains(HEADER_X_API_KEY)
        .contains(SensitiveDataMasking.REDACTED)
        .doesNotContain(apiKey);
  }
}
