package org.sitmun.proxy.middleware.response;

import org.sitmun.proxy.middleware.decorator.DecoratedResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class Response<T> implements DecoratedResponse<T> {

  private final int code;
  private final String contentType;

  private final T body;
  public Response(int code, String contentType, T body) {
    this.code = code;
    this.contentType = contentType;
    this.body = body;
  }

  public int getStatusCode() {
    return code;
  }

  public T getBody() {
    return body;
  }

  public String getContentType() {
    return contentType;
  }

  @Override
  public ResponseEntity<T> asResponseEntity() {
    return ResponseEntity
      .status(code)
      .contentType(MediaType.parseMediaType(contentType))
      .body(body);
  }
}

