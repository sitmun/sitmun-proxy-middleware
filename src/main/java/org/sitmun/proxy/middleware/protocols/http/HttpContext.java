package org.sitmun.proxy.middleware.protocols.http;

import java.util.Map;
import org.sitmun.proxy.middleware.decorator.Context;

public interface HttpContext extends Context {
  HttpContextSecurity getSecurity();

  Map<String, String> getParameters();

  String getUri();

  String getMethod();

  String getBody();
}
