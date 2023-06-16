package org.sitmun.proxy.middleware.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AuthenticationResponse {
  @JsonProperty("id_token")
  private String idToken;
}
