package org.sitmun.proxy.middleware.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fails startup when proxy secrets are blank or shorter than {@value #MIN_SECRET_LENGTH}
 * characters.
 */
@Component
public class ProxySecretValidator {

  private static final int MIN_SECRET_LENGTH = 32;

  private final String backendConfigSecret;

  public ProxySecretValidator(
      @Value("${sitmun.backend.config.secret}") String backendConfigSecret) {
    this.backendConfigSecret = backendConfigSecret;
  }

  @PostConstruct
  public void validate() {
    validateSecret("sitmun.backend.config.secret", backendConfigSecret);
  }

  static void validateSecret(String propertyName, String secret) {
    if (!StringUtils.hasText(secret)) {
      throw new IllegalStateException("%s must not be blank".formatted(propertyName));
    }
    if (secret.trim().length() < MIN_SECRET_LENGTH) {
      throw new IllegalStateException(
          "%s must be at least %d characters".formatted(propertyName, MIN_SECRET_LENGTH));
    }
  }
}
