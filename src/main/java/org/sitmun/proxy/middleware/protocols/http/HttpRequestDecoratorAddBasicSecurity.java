package org.sitmun.proxy.middleware.protocols.http;

import java.util.Base64;
import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HttpRequestDecoratorAddBasicSecurity implements RequestDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    if (context instanceof HttpContext ctx) {
      return acceptsHttpBasic(ctx.getSecurity());
    }
    return false;
  }

  /**
   * HTTP Basic when credentials are present and {@code type}/{@code scheme} match backend OpenAPI
   * semantics ({@code http}/{@code basic}), or are omitted (legacy). Never when {@code type} is
   * {@code apiKey}.
   */
  static boolean acceptsHttpBasic(HttpContextSecurity security) {
    if (security == null
        || !StringUtils.hasText(security.getUsername())
        || !StringUtils.hasText(security.getPassword())) {
      return false;
    }
    if (StringUtils.hasText(security.getType())
        && HttpSecurityConstants.TYPE_API_KEY.equalsIgnoreCase(security.getType().trim())) {
      return false;
    }
    if (StringUtils.hasText(security.getType())
        && !HttpSecurityConstants.TYPE_HTTP.equalsIgnoreCase(security.getType().trim())) {
      return false;
    }
    return !StringUtils.hasText(security.getScheme())
        || HttpSecurityConstants.SCHEME_BASIC.equalsIgnoreCase(security.getScheme().trim());
  }

  @Override
  public void addBehavior(Object target, Context context) {
    HttpRequestExecutor request = (HttpRequestExecutor) target;
    HttpContext httpContext = (HttpContext) context;
    String authString =
        httpContext
            .getSecurity()
            .getUsername()
            .concat(HttpSecurityConstants.BASIC_CREDENTIAL_SEPARATOR)
            .concat(httpContext.getSecurity().getPassword());
    String authEncode = encodeAuthorization(authString);
    request.setHeader(
        HttpSecurityConstants.HEADER_AUTHORIZATION,
        HttpSecurityConstants.AUTH_SCHEME_BASIC_PREFIX.concat(authEncode));
  }

  private String encodeAuthorization(String authorization) {
    return Base64.getEncoder().encodeToString(authorization.getBytes());
  }
}
