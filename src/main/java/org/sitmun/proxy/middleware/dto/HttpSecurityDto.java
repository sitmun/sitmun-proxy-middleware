package org.sitmun.proxy.middleware.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sitmun.proxy.middleware.protocols.http.HttpContextSecurity;

/** DTO for HTTP request security (Basic auth and/or custom headers such as X-API-Key). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpSecurityDto implements HttpContextSecurity {

  /** Security type (e.g. "basic"). */
  private String type;

  /** Auth scheme (e.g. "http" for Basic). */
  private String scheme;

  /** Username for Basic authentication. */
  private String username;

  /** Password for Basic authentication. */
  private String password;

  /** Custom HTTP headers (e.g. X-API-Key). It may be null. */
  private Map<String, String> headers;
}
