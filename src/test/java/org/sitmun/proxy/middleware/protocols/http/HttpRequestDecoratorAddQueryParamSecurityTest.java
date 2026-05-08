package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.proxy.middleware.decorator.Context;

@ExtendWith(MockitoExtension.class)
@DisplayName("HttpRequestDecoratorAddQueryParamSecurity tests")
class HttpRequestDecoratorAddQueryParamSecurityTest {

  private HttpRequestDecoratorAddQueryParamSecurity decorator;
  private HttpRequestExecutor requestExecutor;

  @Mock private HttpContext httpContext;
  @Mock private HttpContextSecurity security;
  @Mock private Context nonHttpContext;

  @BeforeEach
  void setUp() {
    decorator = new HttpRequestDecoratorAddQueryParamSecurity();
    requestExecutor = new HttpRequestExecutor("http://test.com", null);
  }

  @Test
  @DisplayName("Should accept when security has non-empty query params")
  void shouldAcceptWhenQueryParamsPresent() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getQueryParams()).thenReturn(Map.of("api_key", "secret"));

    assertThat(decorator.accept(requestExecutor, httpContext)).isTrue();
  }

  @Test
  @DisplayName("Should not accept when context is not HttpContext")
  void shouldNotAcceptWhenContextIsNotHttpContext() {
    assertThat(decorator.accept(requestExecutor, nonHttpContext)).isFalse();
  }

  @Test
  @DisplayName("Should not accept when security is null")
  void shouldNotAcceptWhenSecurityIsNull() {
    when(httpContext.getSecurity()).thenReturn(null);

    assertThat(decorator.accept(requestExecutor, httpContext)).isFalse();
  }

  @Test
  @DisplayName("Should not accept when query params map is null")
  void shouldNotAcceptWhenQueryParamsNull() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getQueryParams()).thenReturn(null);

    assertThat(decorator.accept(requestExecutor, httpContext)).isFalse();
  }

  @Test
  @DisplayName("Should not accept when query params map is empty")
  void shouldNotAcceptWhenQueryParamsEmpty() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getQueryParams()).thenReturn(Map.of());

    assertThat(decorator.accept(requestExecutor, httpContext)).isFalse();
  }

  @Test
  @DisplayName("Should merge security query params into final URL")
  void shouldMergeQueryParamsIntoUrl() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getQueryParams()).thenReturn(Map.of("token", "abc", "format", "json"));

    requestExecutor.setUrl("https://api.example.com/data?existing=1");
    decorator.addBehavior(requestExecutor, httpContext);

    String url = requestExecutor.getUrl();
    assertThat(url)
        .contains("existing=1")
        .contains("token=abc")
        .contains("format=json")
        .startsWith("https://api.example.com/data");
  }
}
