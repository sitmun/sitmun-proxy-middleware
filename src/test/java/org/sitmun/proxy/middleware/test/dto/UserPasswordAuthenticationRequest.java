package org.sitmun.proxy.middleware.test.dto;

import lombok.Builder;
import lombok.Data;

/** DTO object for storing a user's credentials. */
@Data
@Builder
public class UserPasswordAuthenticationRequest {
  private String username;
  private String password;
}
