package org.sitmun.proxy.middleware.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sitmun.proxy.middleware.protocols.http.HttpContextSecurity;
import org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants;
import org.springframework.util.StringUtils;

/**
 * DTO for HTTP request security (Basic auth and/or custom headers). See {@link
 * org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants} for shared OpenAPI literals.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpSecurityDto implements HttpContextSecurity {

  /** OpenAPI security type (e.g. {@code http} for HTTP auth). */
  private String type;

  /** OpenAPI scheme within type (e.g. {@code basic} for Basic auth). */
  private String scheme;

  /** Username for Basic authentication. */
  private String username;

  /** Password for Basic authentication. */
  private String password;

  /** Custom HTTP headers (e.g. API key header). It may be null. */
  private Map<String, String> headers;

  /**
   * Debug-oriented summary: {@link HttpSecurityConstants#TYPE_API_KEY} → header names (and username
   * if erroneously set); {@link HttpSecurityConstants#TYPE_HTTP} (including legacy blank type with
   * credentials) → scheme, username (literal when set), and password presence; other types → same
   * plus header names. Passwords and header values are never logged. Mismatched fields add {@code
   * warn=[...]}.
   */
  public String describeForLog() {
    if (isApiKeyType()) {
      return describeApiKeyForLog();
    }
    if (isHttpTypeForLog()) {
      return describeHttpForLog();
    }
    return describeOtherForLog();
  }

  private String describeApiKeyForLog() {
    List<String> warns = new ArrayList<>();
    if (StringUtils.hasText(scheme)) {
      warns.add("apiKeyWithScheme");
    }
    if (StringUtils.hasText(username)) {
      warns.add("apiKeyWithUsername");
    }
    if (StringUtils.hasText(password)) {
      warns.add("apiKeyWithPassword");
    }
    String base = "type=" + nullToLog(type) + ", headerNames=" + formatHeaderNameList();
    if (StringUtils.hasText(username)) {
      base += ", username=" + username;
    }
    return appendWarns(base, warns);
  }

  private String describeHttpForLog() {
    List<String> warns = new ArrayList<>();
    if (hasHeaders()) {
      warns.add("httpWithHeaders");
    }
    String base =
        "type="
            + nullToLog(type)
            + ", scheme="
            + nullToLog(scheme)
            + ", username="
            + usernameForLog(username)
            + ", password="
            + presence(password);
    return appendWarns(base, warns);
  }

  private String describeOtherForLog() {
    List<String> warns = new ArrayList<>();
    if (!StringUtils.hasText(type) && hasHeaders() && !hasCredentialFields()) {
      warns.add("headersWithoutType");
    }
    String base =
        "type="
            + nullToLog(type)
            + ", scheme="
            + nullToLog(scheme)
            + ", username="
            + usernameForLog(username)
            + ", password="
            + presence(password)
            + ", headerNames="
            + formatHeaderNameList();
    return appendWarns(base, warns);
  }

  private boolean isApiKeyType() {
    return StringUtils.hasText(type)
        && HttpSecurityConstants.TYPE_API_KEY.equalsIgnoreCase(type.trim());
  }

  /** {@code type=http} or legacy payloads with credentials and no explicit type. */
  private boolean isHttpTypeForLog() {
    if (isApiKeyType()) {
      return false;
    }
    if (StringUtils.hasText(type)
        && HttpSecurityConstants.TYPE_HTTP.equalsIgnoreCase(type.trim())) {
      return true;
    }
    return !StringUtils.hasText(type) && hasCredentialFields();
  }

  private static String nullToLog(String s) {
    return s == null ? "null" : s;
  }

  private static String usernameForLog(String value) {
    return StringUtils.hasText(value) ? value : "unset";
  }

  private static String presence(String value) {
    return StringUtils.hasText(value) ? "set" : "unset";
  }

  private boolean hasCredentialFields() {
    return StringUtils.hasText(username) || StringUtils.hasText(password);
  }

  private boolean hasHeaders() {
    return headers != null && !headers.isEmpty();
  }

  private String formatHeaderNameList() {
    if (headers == null || headers.isEmpty()) {
      return "[]";
    }
    return "[" + headers.keySet().stream().sorted().collect(Collectors.joining(", ")) + "]";
  }

  private static String appendWarns(String base, List<String> warns) {
    if (warns.isEmpty()) {
      return base;
    }
    warns.sort(String::compareTo);
    return base + ", warn=[" + String.join(", ", warns) + "]";
  }
}
