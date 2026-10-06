package org.sitmun.proxy.middleware.service;

import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.decorator.*;
import org.sitmun.proxy.middleware.protocols.http.HttpRequestExecutor;
import org.sitmun.proxy.middleware.servicecheck.ProxyCheckSubject;
import org.sitmun.proxy.middleware.servicecheck.ProxyServiceCheckProperties;
import org.sitmun.proxy.middleware.servicecheck.ProxyServiceCheckReporter;
import org.sitmun.proxy.middleware.usage.ProxyUsage;
import org.sitmun.proxy.middleware.usage.ProxyUsageProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RequestExecutorService {

  private final RequestExecutorFactory requestExecutorFactory;

  private final List<RequestDecorator> requestDecorators;

  private final List<ResponseDecorator> responseDecorators;
  private final ProxyServiceCheckReporter serviceCheckReporter;
  private final ProxyServiceCheckProperties serviceCheckProperties;
  private final ProxyUsage usage;
  private final ProxyUsageProperties usageProperties;
  @Getter private RequestExecutor lastRequestExecutor;
  @Getter private RequestExecutorResponse<?> lastResponse;
  @Getter private Context lastContext;

  public RequestExecutorService(
      RequestExecutorFactory requestExecutorFactory,
      List<RequestDecorator> requestDecorators,
      List<ResponseDecorator> responseDecorators,
      ProxyServiceCheckReporter serviceCheckReporter,
      ProxyServiceCheckProperties serviceCheckProperties,
      ProxyUsage usage,
      ProxyUsageProperties usageProperties) {
    this.requestExecutorFactory = requestExecutorFactory;
    this.requestDecorators = requestDecorators;
    this.responseDecorators = responseDecorators;
    this.serviceCheckReporter = serviceCheckReporter;
    this.serviceCheckProperties = serviceCheckProperties;
    this.usage = usage;
    this.usageProperties = usageProperties;
  }

  public <T> ResponseEntity<T> executeRequest(String baseUrl, Context context) {
    lastContext = context;
    log.debug("Executing request with context: {}", context.describe());

    RequestExecutor request = requestExecutorFactory.create(baseUrl, context);
    log.debug("Default request: {}", request.describe());

    log.debug("Applying {} request decorator(s)", requestDecorators.size());
    requestDecorators.forEach(d -> d.apply(request, context));

    log.debug("Final request: {}", request.describe());
    lastRequestExecutor = request;
    ProxyCheckSubject subject = ProxyCheckSubject.get();
    if (subject != null
        && Boolean.TRUE.equals(serviceCheckProperties.enabled())
        && request instanceof HttpRequestExecutor http) {
      http.setExchangeListener(exchange -> serviceCheckReporter.accept(subject, exchange));
    }

    log.debug("Executing outbound request; context: {}", context.describe());
    RequestExecutorResponse<T> response = request.execute();
    if (subject != null
        && Boolean.TRUE.equals(usageProperties.enabled())
        && request instanceof HttpRequestExecutor http
        && http.upstreamExchange() != null) {
      usage.record(subject, http.upstreamExchange());
    }
    responseDecorators.forEach(d -> d.apply(response, context));
    lastResponse = response;
    return response.asResponseEntity();
  }
}
