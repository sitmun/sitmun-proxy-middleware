package org.sitmun.proxy.middleware.decorator;

import java.util.Map;

public interface HttpContext extends Context {
  HttpContextSecurity getSecurity();

  Map<String, String> getParameters();

  String getUri();
}
