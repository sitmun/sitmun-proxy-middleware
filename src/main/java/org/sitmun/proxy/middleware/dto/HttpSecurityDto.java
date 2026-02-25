package org.sitmun.proxy.middleware.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sitmun.proxy.middleware.protocols.http.HttpContextSecurity;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HttpSecurityDto implements HttpContextSecurity {

  private String type;

  private String scheme;

  private String username;

  private String password;

  private Map<String, String> headers;
}