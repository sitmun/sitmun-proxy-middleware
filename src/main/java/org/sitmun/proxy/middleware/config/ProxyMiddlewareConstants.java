package org.sitmun.proxy.middleware.config;

/**
 * Constants for SITMUN Proxy Middleware. These constants must match the corresponding values in the
 * backend-core module to ensure proper communication.
 */
public class ProxyMiddlewareConstants {
  private ProxyMiddlewareConstants() {
    // Private constructor to prevent instantiation
  }

  /**
   * Header name for authenticating proxy middleware requests to the backend configuration API. This
   * must match SecurityConstants.PROXY_MIDDLEWARE_KEY in backend-core.
   */
  public static final String PROXY_MIDDLEWARE_KEY = "X-SITMUN-Proxy-Key";

  /** Config/request type for WMS services. Must match backend DomainConstants.Services.TYPE_WMS. */
  public static final String TYPE_WMS = "WMS";

  /** Config/request type for WMTS services. Must match backend DomainConstants.Services.TYPE_WMTS. */
  public static final String TYPE_WMTS = "WMTS";

  /** Config/request type for SQL tasks. Must match backend DomainConstants.Tasks.PROXY_TYPE_SQL. */
  public static final String TYPE_SQL = "SQL";

  /** Config/request type for API tasks. Must match backend DomainConstants.Services.TYPE_API. */
  public static final String TYPE_API = "API";
}
