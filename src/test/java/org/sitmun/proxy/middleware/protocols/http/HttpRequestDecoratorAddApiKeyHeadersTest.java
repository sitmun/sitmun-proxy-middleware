package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.HEADER_X_API_KEY;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.TYPE_API_KEY;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.TYPE_HTTP;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.proxy.middleware.decorator.Context;

@ExtendWith(MockitoExtension.class)
@DisplayName("HttpRequestDecoratorAddApiKeyHeaders tests")
class HttpRequestDecoratorAddApiKeyHeadersTest {

  private HttpRequestDecoratorAddApiKeyHeaders decorator;
  private HttpRequestExecutor requestExecutor;

  @Mock private HttpContext httpContext;
  @Mock private HttpContextSecurity security;
  @Mock private Context nonHttpContext;

  @BeforeEach
  void setUp() {
    decorator = new HttpRequestDecoratorAddApiKeyHeaders();
    requestExecutor = new HttpRequestExecutor("http://test.com", null);
  }

  @Test
  @DisplayName("Accepts HttpContext with apiKey type and non-empty headers")
  void acceptsApiKeySecurity() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getType()).thenReturn(TYPE_API_KEY);
    when(security.getHeaders()).thenReturn(Map.of(HEADER_X_API_KEY, "secret"));

    assertThat(decorator.accept(requestExecutor, httpContext)).isTrue();
  }

  @Test
  @DisplayName("Rejects when type is not apiKey")
  void rejectsNonApiKeyType() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getType()).thenReturn(TYPE_HTTP);

    assertThat(decorator.accept(requestExecutor, httpContext)).isFalse();
  }

  @Test
  @DisplayName("Rejects when headers map is empty")
  void rejectsEmptyHeaders() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getType()).thenReturn(TYPE_API_KEY);
    when(security.getHeaders()).thenReturn(Map.of());

    assertThat(decorator.accept(requestExecutor, httpContext)).isFalse();
  }

  @Test
  @DisplayName("Adds configured headers to executor")
  void addsHeaders() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getHeaders()).thenReturn(Map.of(HEADER_X_API_KEY, "k", "X-Other", "v"));

    decorator.addBehavior(requestExecutor, httpContext);

    assertThat(requestExecutor.getHeader(HEADER_X_API_KEY)).isEqualTo("k");
    assertThat(requestExecutor.getHeader("X-Other")).isEqualTo("v");
  }
}
