package org.sitmun.proxy.middleware.protocols.http;

import java.util.Map;
import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.springframework.stereotype.Component;

/**
 * Applies all entries from {@link HttpContextSecurity#getHeaders()} to the outbound request. Values
 * come from the proxy HTTP security payload (e.g. OpenAPI {@code apiKey} in header), not from
 * browser response security headers (HSTS, CSP, etc.).
 */
@Component
public class HttpRequestDecoratorAddHeaderSecurity implements RequestDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    if (context instanceof HttpContext ctx && ctx.getSecurity() != null) {
      Map<String, String> headers = ctx.getSecurity().getHeaders();
      return headers != null && !headers.isEmpty();
    }
    return false;
  }

  @Override
  public void addBehavior(Object target, Context context) {
    HttpRequestExecutor request = (HttpRequestExecutor) target;
    HttpContext httpContext = (HttpContext) context;
    Map<String, String> headers = httpContext.getSecurity().getHeaders();
    if (headers != null) {
      headers.forEach(request::setHeader);
    }
  }
}
