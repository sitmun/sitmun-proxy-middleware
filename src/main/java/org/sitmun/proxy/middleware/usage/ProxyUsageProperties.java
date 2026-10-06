package org.sitmun.proxy.middleware.usage;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sitmun.proxy.usage")
public record ProxyUsageProperties(
    @NotNull Boolean enabled, @NotNull Duration flushInterval, @NotNull Duration backlog) {}
