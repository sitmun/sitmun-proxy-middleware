package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.AUTH_SCHEME_BASIC_PREFIX;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.BASIC_CREDENTIAL_SEPARATOR;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.HEADER_AUTHORIZATION;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.SCHEME_BASIC;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.TYPE_API_KEY;
import static org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants.TYPE_HTTP;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.proxy.middleware.decorator.Context;

@ExtendWith(MockitoExtension.class)
@DisplayName("HttpRequestDecoratorAddBasicSecurity tests")
class HttpRequestDecoratorAddBasicSecurityTest {

  private HttpRequestDecoratorAddBasicSecurity decorator;
  private HttpRequestExecutor requestExecutor;

  @Mock private HttpContext httpContext;
  @Mock private HttpContextSecurity security;
  @Mock private Context nonHttpContext;

  @BeforeEach
  void setUp() {
    decorator = new HttpRequestDecoratorAddBasicSecurity();
    requestExecutor = new HttpRequestExecutor("http://test.com", null);
  }

  @Test
  @DisplayName("Should accept when context is HttpContext with valid security credentials")
  void shouldAcceptWhenContextIsHttpContextWithValidSecurityCredentials() {
    // Given
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getUsername()).thenReturn("testuser");
    when(security.getPassword()).thenReturn("testpass");

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should not accept when context is HttpContext with null security")
  void shouldNotAcceptWhenContextIsHttpContextWithNullSecurity() {
    // Given
    when(httpContext.getSecurity()).thenReturn(null);

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should not accept when context is HttpContext with null username")
  void shouldNotAcceptWhenContextIsHttpContextWithNullUsername() {
    // Given
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getUsername()).thenReturn(null);

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should not accept when context is HttpContext with empty username")
  void shouldNotAcceptWhenContextIsHttpContextWithEmptyUsername() {
    // Given
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getUsername()).thenReturn("");

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should not accept when context is HttpContext with null password")
  void shouldNotAcceptWhenContextIsHttpContextWithNullPassword() {
    // Given
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getUsername()).thenReturn("testuser");
    when(security.getPassword()).thenReturn(null);

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should not accept when context is HttpContext with empty password")
  void shouldNotAcceptWhenContextIsHttpContextWithEmptyPassword() {
    // Given
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getUsername()).thenReturn("testuser");
    when(security.getPassword()).thenReturn("");

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
  @DisplayName("Should add Basic Authorization header when adding behavior")
  void shouldAddBasicAuthorizationHeaderWhenAddingBehavior() {
    // Given
    String username = "testuser";
    String password = "testpass";
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getUsername()).thenReturn(username);
    when(security.getPassword()).thenReturn(password);

    // When
    decorator.addBehavior(requestExecutor, httpContext);

    // Then
    assertThat(requestExecutor.getHeader(HEADER_AUTHORIZATION))
        .isEqualTo(
            AUTH_SCHEME_BASIC_PREFIX
                + Base64.getEncoder()
                    .encodeToString((username + BASIC_CREDENTIAL_SEPARATOR + password).getBytes()));
  }

  @Test
  @DisplayName("Should not accept when type is apiKey even with username and password")
  void shouldNotAcceptWhenTypeIsApiKey() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getType()).thenReturn(TYPE_API_KEY);
    when(security.getUsername()).thenReturn("u");
    when(security.getPassword()).thenReturn("p");

    assertThat(decorator.accept(requestExecutor, httpContext)).isFalse();
  }

  @Test
  @DisplayName("Should not accept oauth/bearer style type with credentials")
  void shouldNotAcceptOauthStyleType() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getType()).thenReturn("oauth");
    when(security.getUsername()).thenReturn("u");
    when(security.getPassword()).thenReturn("p");

    assertThat(decorator.accept(requestExecutor, httpContext)).isFalse();
  }

  @Test
  @DisplayName("Should accept explicit http/basic OpenAPI-style security")
  void shouldAcceptExplicitHttpBasic() {
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getType()).thenReturn(TYPE_HTTP);
    when(security.getScheme()).thenReturn(SCHEME_BASIC);
    when(security.getUsername()).thenReturn("u");
    when(security.getPassword()).thenReturn("p");

    assertThat(decorator.accept(requestExecutor, httpContext)).isTrue();
  }

  @Test
  @DisplayName("Should handle whitespace-only username")
  void shouldHandleWhitespaceOnlyUsername() {
    // Given
    String username = "   "; // whitespace only
    when(httpContext.getSecurity()).thenReturn(security);
    when(security.getUsername()).thenReturn(username);

    // When
    boolean result = decorator.accept(requestExecutor, httpContext);

    // Then
    assertThat(result).isFalse(); // Should not accept whitespace-only credentials
  }
}
