package org.sitmun.proxy.middleware.decorator.request;

import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.HttpContext;
import org.sitmun.proxy.middleware.decorator.RequestDecorator;
import org.sitmun.proxy.middleware.request.HttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Base64;

@Component
public class HttpBasicSecurityRequestDecorator implements RequestDecorator {

  @Override
  public boolean accept(Object target, Context context) {
    if (context instanceof HttpContext) {
      HttpContext ctx = (HttpContext) context;
      return ctx.getSecurity() != null
        && StringUtils.hasText(ctx.getSecurity().getUsername())
        && StringUtils.hasText(ctx.getSecurity().getPassword());
    } else {
      return false;
    }
  }

  @Override
  public void addBehavior(Object target, Context context) {
    HttpRequest request = (HttpRequest) target;
    HttpContext httpContext = (HttpContext) context;
    String authString = httpContext.getSecurity().getUsername().concat(":").concat(httpContext.getSecurity().getPassword());
    String authEncode = encodeAuthorization(authString);
    request.setHeader("Authorization", "Basic ".concat(authEncode));
  }

  private String encodeAuthorization(String authorization) {
    return Base64.getEncoder().encodeToString(authorization.getBytes());
  }
}
