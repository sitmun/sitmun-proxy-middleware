package org.sitmun.proxy.middleware.protocols.http;

/**
 * OpenAPI-style HTTP security literals shared by request decorators, tests, and fixtures. Header
 * names in {@link HttpContextSecurity#getHeaders()} are case-sensitive map keys.
 */
public final class HttpSecurityConstants {

  private HttpSecurityConstants() {}

  /** OpenAPI {@code type} for API key (header, query, or cookie). */
  public static final String TYPE_API_KEY = "apiKey";

  /** OpenAPI {@code type} for HTTP authentication (see {@link #SCHEME_BASIC}). */
  public static final String TYPE_HTTP = "http";

  /** OpenAPI {@code scheme} when {@link #TYPE_HTTP} is used for Basic credentials. */
  public static final String SCHEME_BASIC = "basic";

  public static final String HEADER_AUTHORIZATION = "Authorization";

  /** Value prefix for Basic credentials in the Authorization header. */
  public static final String AUTH_SCHEME_BASIC_PREFIX = "Basic ";

  /** Separator between username and password before Base64 encoding for Basic auth. */
  public static final String BASIC_CREDENTIAL_SEPARATOR = ":";

  /**
   * Typical API key header name (matches backend-core examples and JSON payloads). Used by {@link
   * HttpRequestDecoratorAddApiKeyHeader} for legacy single-header configuration.
   */
  public static final String HEADER_X_API_KEY = "X-API-Key";
}
