package org.sitmun.proxy.middleware.servicecheck;

import java.net.URI;
import java.time.Instant;

public record UpstreamExchange(
    String method,
    URI uri,
    Integer httpStatus,
    String contentType,
    byte[] body,
    Throwable transportError,
    long elapsedMs,
    Instant observedAt) {}
