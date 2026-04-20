package org.sitmun.proxy.middleware.protocols.http;

import java.util.Map;
import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HttpRequestDecoratorAddApiKeyHeaders implements RequestDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    if (!(context instanceof HttpContext ctx) || !(target instanceof HttpRequestExecutor)) {
      return false;
    }
    HttpContextSecurity security = ctx.getSecurity();
    if (security == null || !StringUtils.hasText(security.getType())) {
      return false;
    }
    if (!HttpSecurityConstants.TYPE_API_KEY.equalsIgnoreCase(security.getType().trim())) {
      return false;
    }
    Map<String, String> headers = security.getHeaders();
    return headers != null && !headers.isEmpty();
  }

  @Override
  public void addBehavior(Object target, Context context) {
    HttpRequestExecutor request = (HttpRequestExecutor) target;
    HttpContext httpContext = (HttpContext) context;
    Map<String, String> headers = httpContext.getSecurity().getHeaders();
    if (headers == null) {
      return;
    }
    for (Map.Entry<String, String> e : headers.entrySet()) {
      String name = e.getKey();
      String value = e.getValue();
      if (StringUtils.hasText(name) && value != null) {
        request.setHeader(name, value);
      }
    }
  }
}
