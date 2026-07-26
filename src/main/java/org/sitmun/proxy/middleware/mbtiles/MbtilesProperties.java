package org.sitmun.proxy.middleware.mbtiles;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sitmun.mbtiles")
public record MbtilesProperties(
    String url,
    String jobHandleSecret,
    Duration jobHandleTtl,
    int maxJsonBytes,
    int maxZoomSpan,
    List<String> allowedSrs,
    Duration connectTimeout,
    Duration readTimeout) {

  public MbtilesProperties {
    if (jobHandleTtl == null) {
      jobHandleTtl = Duration.ofHours(24);
    }
    if (maxJsonBytes <= 0) {
      maxJsonBytes = 65_536;
    }
    if (maxZoomSpan < 0) {
      maxZoomSpan = 12;
    }
    if (allowedSrs == null || allowedSrs.isEmpty()) {
      allowedSrs = List.of("EPSG:4326", "EPSG:3857", "EPSG:25831");
    } else {
      allowedSrs = List.copyOf(allowedSrs);
    }
    if (connectTimeout == null) {
      connectTimeout = Duration.ofSeconds(10);
    }
    if (readTimeout == null) {
      readTimeout = Duration.ofSeconds(60);
    }
  }
}
