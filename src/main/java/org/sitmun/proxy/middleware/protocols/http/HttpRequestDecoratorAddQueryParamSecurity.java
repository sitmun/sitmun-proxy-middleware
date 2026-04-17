package org.sitmun.proxy.middleware.protocols.http;

import java.util.Map;
import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.springframework.stereotype.Component;

/** Decorator that appends security query parameters to the request URL. */
@Component
public class HttpRequestDecoratorAddQueryParamSecurity implements RequestDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    if (context instanceof HttpContext ctx && ctx.getSecurity() != null) {
      Map<String, String> queryParams = ctx.getSecurity().getQueryParams();
      return queryParams != null && !queryParams.isEmpty();
    }
    return false;
  }

  @Override
  public void addBehavior(Object target, Context context) {
    HttpRequestExecutor request = (HttpRequestExecutor) target;
    HttpContext httpContext = (HttpContext) context;
    Map<String, String> queryParams = httpContext.getSecurity().getQueryParams();
    if (queryParams != null) {
      queryParams.forEach(request::addParameter);
    }
  }
}
