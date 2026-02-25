package org.sitmun.proxy.middleware.protocols.http;

import java.util.Map;

public interface HttpContextSecurity {
  String getUsername();

  String getPassword();

  Map<String, String> getHeaders();
}
