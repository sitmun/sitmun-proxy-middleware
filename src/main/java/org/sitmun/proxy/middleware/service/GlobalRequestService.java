package org.sitmun.proxy.middleware.service;

import org.sitmun.proxy.middleware.decorator.*;
import org.sitmun.proxy.middleware.request.RequestFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GlobalRequestService {

  private final RequestFactory requestFactory;

  private final List<RequestDecorator> requestDecorators;

  private final List<ResponseDecorator> responseDecorators;

  public GlobalRequestService(RequestFactory requestFactory, List<RequestDecorator> requestDecorators, List<ResponseDecorator> responseDecorators) {
    this.requestFactory = requestFactory;
    this.requestDecorators = requestDecorators;
    this.responseDecorators = responseDecorators;
  }

  public <T> ResponseEntity<T> executeRequest(Context context) {
    lastContext = context;
    DecoratedRequest request = requestFactory.create(context);
    requestDecorators.forEach(d -> d.apply(request, context));
    lastRequest = request;
    DecoratedResponse<T> response = request.execute();
    responseDecorators.forEach(d -> d.apply(response, context));
    lastResponse = response;
    return response.asResponseEntity();
  }

  private DecoratedRequest lastRequest;
  private DecoratedResponse<?> lastResponse;

  private Context lastContext;

  public DecoratedRequest getLastRequest() {
    return lastRequest;
  }

  public DecoratedResponse<?> getLastResponse() {
    return lastResponse;
  }

  public Context getLastContext() {
    return lastContext;
  }

}
