package org.sitmun.proxy.middleware.servicecheck;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sitmun.proxy.service-check")
public record ProxyServiceCheckProperties(
    @NotNull Boolean enabled,
    @NotNull Integer debounceCount,
    @NotNull Duration debounceWindow,
    @NotNull Integer scanMaxBytes,
    @NotNull Integer exceptionTextMaxLength) {}
