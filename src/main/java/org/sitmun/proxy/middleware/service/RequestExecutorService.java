package org.sitmun.proxy.middleware.service;

import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.decorator.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RequestExecutorService {

  private final RequestExecutorFactory requestExecutorFactory;

  private final List<RequestDecorator> requestDecorators;

  private final List<ResponseDecorator> responseDecorators;
  @Getter private RequestExecutor lastRequestExecutor;
  @Getter private RequestExecutorResponse<?> lastResponse;
  @Getter private Context lastContext;

  public RequestExecutorService(
      RequestExecutorFactory requestExecutorFactory,
      List<RequestDecorator> requestDecorators,
      List<ResponseDecorator> responseDecorators) {
    this.requestExecutorFactory = requestExecutorFactory;
    this.requestDecorators = requestDecorators;
    this.responseDecorators = responseDecorators;
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

    log.debug("Executing outbound request; context: {}", context.describe());
    RequestExecutorResponse<T> response = request.execute();
    responseDecorators.forEach(d -> d.apply(response, context));
    lastResponse = response;
    return response.asResponseEntity();
  }
}
