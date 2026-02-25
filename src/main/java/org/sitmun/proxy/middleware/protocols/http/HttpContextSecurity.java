package org.sitmun.proxy.middleware.protocols.http;

import java.util.Map;

/** HTTP request security configuration (e.g. Basic auth, API key headers). */
public interface HttpContextSecurity {

  /** Username for Basic authentication. */
  String getUsername();

  /** Password for Basic authentication. */
  String getPassword();

  /** Custom HTTP headers (e.g. X-API-Key). May be null if none is configured. */
  Map<String, String> getHeaders();
}
