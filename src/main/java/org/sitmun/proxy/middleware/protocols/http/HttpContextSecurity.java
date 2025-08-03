package org.sitmun.proxy.middleware.protocols.http;

public interface HttpContextSecurity {
  String getUsername();

  String getPassword();
}
