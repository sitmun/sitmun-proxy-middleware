package org.sitmun.proxy.middleware.protocols.http;

import java.util.Map;

/** HTTP request security configuration (e.g. Basic auth, API key headers). */
public interface HttpContextSecurity {

  /**
   * OpenAPI-style security scheme type (e.g. {@code http}, {@code apiKey}). Null or blank means
   * unspecified (legacy payloads); consumers may infer behavior from other fields.
   */
  String getType();

  /**
   * OpenAPI-style scheme when {@link #getType()} is {@code http} (e.g. {@code basic}). Null or
   * blank means unspecified.
   */
  String getScheme();

  /** Username for Basic authentication. */
  String getUsername();

  /** Password for Basic authentication. */
  String getPassword();

  /**
   * Custom HTTP headers (e.g. {@link HttpSecurityConstants#HEADER_X_API_KEY}). May be null if none
   * is configured.
   */
  Map<String, String> getHeaders();
}
