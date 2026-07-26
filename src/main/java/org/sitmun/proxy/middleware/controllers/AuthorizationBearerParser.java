package org.sitmun.proxy.middleware.controllers;

import java.util.regex.Pattern;

/** Shared Bearer parsing for proxy routes. Never logs token values. */
public final class AuthorizationBearerParser {

  private static final Pattern BEARER_AUTHORIZATION =
      Pattern.compile("(?i)^Bearer ([A-Za-z0-9\\-._~+/]+=*)$");

  private AuthorizationBearerParser() {}

  /**
   * @param authorization raw Authorization header value, or {@code null}
   * @param required when true, missing header is invalid; when false, missing means public (null
   *     token)
   */
  public static AuthorizationToken parse(String authorization, boolean required) {
    if (authorization == null) {
      return required ? AuthorizationToken.invalid() : AuthorizationToken.publicAccess();
    }
    var matcher = BEARER_AUTHORIZATION.matcher(authorization);
    return matcher.matches()
        ? AuthorizationToken.valid(matcher.group(1))
        : AuthorizationToken.invalid();
  }

  public record AuthorizationToken(boolean valid, String token) {
    static AuthorizationToken valid(String token) {
      return new AuthorizationToken(true, token);
    }

    static AuthorizationToken invalid() {
      return new AuthorizationToken(false, null);
    }

    static AuthorizationToken publicAccess() {
      return new AuthorizationToken(true, null);
    }
  }
}
