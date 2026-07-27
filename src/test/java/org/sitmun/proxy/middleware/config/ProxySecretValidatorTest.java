package org.sitmun.proxy.middleware.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProxySecretValidatorTest {

  @Test
  void validateSecretRejectsBlankValue() {
    assertThatThrownBy(
            () -> ProxySecretValidator.validateSecret("sitmun.backend.config.secret", " "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("sitmun.backend.config.secret");
  }

  @Test
  void validateSecretRejectsShortValue() {
    assertThatThrownBy(
            () ->
                ProxySecretValidator.validateSecret("sitmun.backend.config.secret", "short-secret"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("sitmun.backend.config.secret");
  }

  @Test
  void validateSecretAcceptsMinimumLengthValue() {
    assertThatCode(
            () ->
                ProxySecretValidator.validateSecret(
                    "sitmun.backend.config.secret", "test-only-insecure-middleware-secret"))
        .doesNotThrowAnyException();
  }
}
