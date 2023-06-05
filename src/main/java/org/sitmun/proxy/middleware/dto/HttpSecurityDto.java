package org.sitmun.proxy.middleware.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sitmun.proxy.middleware.decorator.HttpContextSecurity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HttpSecurityDto implements HttpContextSecurity {

  private String type;

  private String scheme;

  private String username;

  private String password;
}
