package org.sitmun.proxy.middleware.usage;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.sitmun.proxy.contract.ServiceUsage;
import org.sitmun.proxy.contract.ServiceUsageReport;
import org.sitmun.proxy.middleware.servicecheck.ProxyCheckSubject;
import org.sitmun.proxy.middleware.servicecheck.ProxyServiceCheckReporter;
import org.sitmun.proxy.middleware.servicecheck.UpstreamExchange;
import org.sitmun.upstream.signal.ExchangeSignals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProxyUsage {

  private static final Logger log = LoggerFactory.getLogger(ProxyUsage.class);
  private static final int OPERATION_LENGTH = 32;

  private final ProxyUsageProperties properties;
  private final ProxyServiceCheckReporter reporter;
  private final ServiceUsagePoster poster;
  private final Clock clock;
  private final Object lock = new Object();
  private final Map<Bucket, long[]> pending = new HashMap<>();
  private final ScheduledExecutorService scheduler;

  @Autowired
  public ProxyUsage(
      ProxyUsageProperties properties,
      ProxyServiceCheckReporter reporter,
      ServiceUsagePoster poster) {
    this(properties, reporter, poster, Clock.systemUTC(), true);
  }

  ProxyUsage(
      ProxyUsageProperties properties,
      ProxyServiceCheckReporter reporter,
      ServiceUsagePoster poster,
      Clock clock) {
    this(properties, reporter, poster, clock, false);
  }

  private ProxyUsage(
      ProxyUsageProperties properties,
      ProxyServiceCheckReporter reporter,
      ServiceUsagePoster poster,
      Clock clock,
      boolean schedule) {
    this.properties = properties;
    this.reporter = reporter;
    this.poster = poster;
    this.clock = clock;
    if (schedule && Boolean.TRUE.equals(properties.enabled())) {
      scheduler =
          Executors.newSingleThreadScheduledExecutor(
              runnable -> {
                Thread thread = new Thread(runnable, "proxy-usage");
                thread.setDaemon(true);
                return thread;
              });
      long delay = properties.flushInterval().toMillis();
      scheduler.scheduleWithFixedDelay(this::flushQuietly, delay, delay, TimeUnit.MILLISECONDS);
    } else {
      scheduler = null;
    }
  }

  public void record(ProxyCheckSubject subject, UpstreamExchange exchange) {
    if (!Boolean.TRUE.equals(properties.enabled()) || subject == null || exchange.uri() == null) {
      return;
    }
    ExchangeSignals signals = reporter.signals(exchange);
    boolean failed = failed(exchange, signals);
    Bucket bucket =
        new Bucket(
            subject.serviceId(),
            subject.applicationId(),
            exchange.observedAt().truncatedTo(ChronoUnit.HOURS),
            operation(exchange.uri()));
    synchronized (lock) {
      long[] counts = pending.computeIfAbsent(bucket, key -> new long[2]);
      counts[0] += 1;
      if (failed) {
        counts[1] += 1;
      }
    }
  }

  public void flush() {
    List<ServiceUsage> batch = snapshot();
    if (batch.isEmpty()) {
      return;
    }
    try {
      poster.post(new ServiceUsageReport(batch));
      subtract(batch);
    } catch (UsageEndpointMissing ex) {
      log.warn("Service usage endpoint is missing; dropping this flush");
      subtract(batch);
    } catch (RuntimeException ex) {
      log.warn("Service usage flush failed: {}", ex.getClass().getSimpleName());
    }
  }

  @PreDestroy
  void shutdown() {
    if (Boolean.TRUE.equals(properties.enabled())) {
      flush();
    }
    if (scheduler != null) {
      scheduler.shutdown();
    }
  }

  List<ServiceUsage> pending() {
    return snapshot();
  }

  private void flushQuietly() {
    try {
      flush();
    } catch (RuntimeException ex) {
      log.warn("Service usage flush failed: {}", ex.getClass().getSimpleName());
    }
  }

  private List<ServiceUsage> snapshot() {
    Instant cutoff = clock.instant().minus(properties.backlog());
    synchronized (lock) {
      pending.keySet().removeIf(bucket -> bucket.hourStart().isBefore(cutoff));
      List<ServiceUsage> batch = new ArrayList<>();
      pending.forEach(
          (bucket, counts) ->
              batch.add(
                  new ServiceUsage(
                      bucket.serviceId(),
                      bucket.applicationId(),
                      bucket.hourStart(),
                      bucket.operation(),
                      counts[0],
                      counts[1])));
      return batch;
    }
  }

  private void subtract(List<ServiceUsage> batch) {
    synchronized (lock) {
      for (ServiceUsage sent : batch) {
        Bucket bucket =
            new Bucket(sent.serviceId(), sent.applicationId(), sent.hourStart(), sent.operation());
        long[] counts = pending.get(bucket);
        if (counts == null) {
          continue;
        }
        counts[0] -= sent.requests();
        counts[1] -= sent.failed();
        if (counts[0] <= 0 && counts[1] <= 0) {
          pending.remove(bucket);
        }
      }
    }
  }

  private static boolean failed(UpstreamExchange exchange, ExchangeSignals signals) {
    if (exchange.transportError() != null) {
      return true;
    }
    if (exchange.httpStatus() != null && exchange.httpStatus() >= 400) {
      return true;
    }
    return notBlank(signals.ogcExceptionCode()) || notBlank(signals.ogcExceptionText());
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }

  static String operation(URI uri) {
    String raw = uri.getRawQuery();
    if (raw == null || raw.isEmpty()) {
      return "Other";
    }
    for (String part : raw.split("&")) {
      int eq = part.indexOf('=');
      String key = eq >= 0 ? part.substring(0, eq) : part;
      String value = eq >= 0 ? part.substring(eq + 1) : "";
      if ("request".equalsIgnoreCase(URLDecoder.decode(key, StandardCharsets.UTF_8))) {
        return canonical(URLDecoder.decode(value, StandardCharsets.UTF_8));
      }
    }
    return "Other";
  }

  private static String canonical(String requestName) {
    if (requestName == null || requestName.isBlank()) {
      return "Other";
    }
    String trimmed = requestName.trim();
    return switch (trimmed.toLowerCase(Locale.ROOT)) {
      case "getmap" -> "GetMap";
      case "gettile" -> "GetTile";
      case "getfeatureinfo" -> "GetFeatureInfo";
      case "getcapabilities" -> "GetCapabilities";
      case "other" -> "Other";
      case "viewerconfig" -> "ViewerConfig";
      default ->
          trimmed.length() > OPERATION_LENGTH ? trimmed.substring(0, OPERATION_LENGTH) : trimmed;
    };
  }

  private record Bucket(long serviceId, long applicationId, Instant hourStart, String operation) {}
}
