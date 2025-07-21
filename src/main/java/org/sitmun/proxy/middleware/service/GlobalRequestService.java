package org.sitmun.proxy.middleware.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.decorator.*;
import org.sitmun.proxy.middleware.request.RequestFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GlobalRequestService {

  private final RequestFactory requestFactory;

  private final List<RequestDecorator> requestDecorators;

  private final List<ResponseDecorator> responseDecorators;
  @Getter
  private DecoratedRequest lastRequest;
  @Getter
  private DecoratedResponse<?> lastResponse;
  @Getter
  private Context lastContext;
  public GlobalRequestService(RequestFactory requestFactory, List<RequestDecorator> requestDecorators, List<ResponseDecorator> responseDecorators) {
    this.requestFactory = requestFactory;
    this.requestDecorators = requestDecorators;
    this.responseDecorators = responseDecorators;
  }

  public <T> ResponseEntity<T> executeRequest(String baseUrl, Context context) {
    lastContext = context;
    log.info("Executing request with context: {}", context.describe());

    DecoratedRequest request = requestFactory.create(baseUrl, context);
    log.info("Default request: {}", request.describe());

    requestDecorators.forEach(d -> {
      try {
        d.apply(request, context);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });

    log.info("Final request: {}", request.describe());
    lastRequest = request;

    log.info("Executing request after applying context: {}", context.describe());
    DecoratedResponse<T> response = request.execute();
    responseDecorators.forEach(d -> {
      try {
        d.apply(response, context);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    lastResponse = response;
    return response.asResponseEntity();
  }
}
