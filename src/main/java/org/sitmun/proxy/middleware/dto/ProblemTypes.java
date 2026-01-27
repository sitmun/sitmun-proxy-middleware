package org.sitmun.proxy.middleware.dto;

/**
 * RFC 9457 Problem Detail type URIs for proxy middleware errors.
 *
 * <p>These are specific to proxy middleware operations and align with the main backend problem
 * types.
 */
public final class ProblemTypes {

  private static final String BASE_URI = "https://sitmun.org/problems/";

  /** Proxy configuration error. HTTP 500. */
  public static final String PROXY_CONFIG_ERROR = BASE_URI + "proxy-config-error";

  /** Backend service unreachable. HTTP 502 Bad Gateway. */
  public static final String PROXY_BACKEND_ERROR = BASE_URI + "proxy-backend-error";

  /** External service error. HTTP 503 Service Unavailable. */
  public static final String PROXY_SERVICE_ERROR = BASE_URI + "proxy-service-error";

  /** Proxy authentication required. HTTP 401. */
  public static final String PROXY_UNAUTHORIZED = BASE_URI + "proxy-unauthorized";

  private ProblemTypes() {
    // Prevent instantiation
  }
}
