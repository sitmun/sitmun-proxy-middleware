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
      return ctx.getSecurity() != null
          && StringUtils.hasText(ctx.getSecurity().getUsername())
          && StringUtils.hasText(ctx.getSecurity().getPassword());
    } else {
      return false;
    }
  }

  @Override
  public void addBehavior(Object target, Context context) {
    HttpRequestExecutor request = (HttpRequestExecutor) target;
    HttpContext httpContext = (HttpContext) context;
    String authString =
        httpContext
            .getSecurity()
            .getUsername()
            .concat(":")
            .concat(httpContext.getSecurity().getPassword());
    String authEncode = encodeAuthorization(authString);
    request.setHeader("Authorization", "Basic ".concat(authEncode));
  }

  private String encodeAuthorization(String authorization) {
    return Base64.getEncoder().encodeToString(authorization.getBytes());
  }
}
