package org.sitmun.proxy.middleware.servicecheck;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.contract.ServiceCheckReport;
import org.sitmun.upstream.signal.Classification;
import org.sitmun.upstream.signal.ExchangeSignals;
import org.sitmun.upstream.signal.RequestContext;
import org.sitmun.upstream.signal.ServiceCheckClassifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProxyServiceCheckReporter {

  private final ServiceCheckClassifier classifier;
  private final ServiceCheckDebounce debounce;
  private final ProxyServiceCheckProperties properties;
  private final ServiceCheckPoster poster;

  public ProxyServiceCheckReporter(
      ServiceCheckClassifier classifier,
      ProxyServiceCheckProperties properties,
      ServiceCheckPoster poster) {
    this.classifier = classifier;
    this.properties = properties;
    this.poster = poster;
    this.debounce =
        new ServiceCheckDebounce(properties.debounceCount(), properties.debounceWindow());
  }

  public void accept(ProxyCheckSubject subject, UpstreamExchange exchange) {
    if (exchange.uri() == null) {
      return;
    }
    ExchangeSignals signals = signals(exchange);
    RequestContext context =
        RequestContext.from(
            subject.protocol(), false, exchange.uri(), exchange.method(), query(exchange.uri()));
    Optional<Classification> classification = classifier.classify(context, signals);
    boolean post =
        debounce.observe(
            subject.serviceId(),
            classification.map(found -> found.status().code()),
            exchange.observedAt());
    if (!post) {
      return;
    }
    try {
      poster.post(
          new ServiceCheckReport(
              subject.serviceId(), context, signals, exchange.elapsedMs(), exchange.observedAt()));
    } catch (RuntimeException ex) {
      log.warn(
          "Service check report failed for service {}: {}",
          subject.serviceId(),
          ex.getClass().getSimpleName());
    }
  }

  public ExchangeSignals signals(UpstreamExchange exchange) {
    if (exchange.transportError() != null) {
      return ExchangeSignals.transport(exchange.transportError());
    }
    ExchangeSignals http = ExchangeSignals.http(exchange.httpStatus());
    if (exchange.body() == null || !scannable(exchange.contentType())) {
      return http;
    }
    return http.scan(
        new ByteArrayInputStream(exchange.body()),
        properties.scanMaxBytes(),
        properties.exceptionTextMaxLength());
  }

  static boolean scannable(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return false;
    }
    String type = contentType.toLowerCase();
    int semi = type.indexOf(';');
    if (semi >= 0) {
      type = type.substring(0, semi).trim();
    }
    if (type.startsWith("image/") || type.contains("octet-stream")) {
      return false;
    }
    return type.contains("xml") || type.startsWith("text/");
  }

  private static Map<String, String> query(URI uri) {
    String raw = uri.getRawQuery();
    if (raw == null || raw.isEmpty()) {
      return Map.of();
    }
    Map<String, String> query = new LinkedHashMap<>();
    for (String part : raw.split("&")) {
      int eq = part.indexOf('=');
      String key = eq >= 0 ? part.substring(0, eq) : part;
      String value = eq >= 0 ? part.substring(eq + 1) : "";
      query.put(
          URLDecoder.decode(key, StandardCharsets.UTF_8),
          URLDecoder.decode(value, StandardCharsets.UTF_8));
    }
    return query;
  }
}
