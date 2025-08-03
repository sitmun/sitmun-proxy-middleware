package org.sitmun.proxy.middleware.service;

import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Data
public class RequestExecutorResponseImpl<T> implements RequestExecutorResponse<T> {

  private String baseUrl;
  private int statusCode;
  private String contentType;
  private T body;

  public RequestExecutorResponseImpl(String baseUrl, int statusCode, String contentType, T body) {
    this.baseUrl = baseUrl;
    this.statusCode = statusCode;
    this.contentType = contentType;
    this.body = body;
  }

  @Override
  public ResponseEntity<T> asResponseEntity() {
    return ResponseEntity.status(statusCode)
        .contentType(MediaType.parseMediaType(contentType))
        .body(body);
  }
}
