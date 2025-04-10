package org.sitmun.proxy.middleware.response;

import lombok.Data;
import org.sitmun.proxy.middleware.decorator.DecoratedResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Data
public class Response<T> implements DecoratedResponse<T> {

  private String baseUrl;
  private int statusCode;
  private String contentType;
  private T body;

  public Response(String baseUrl, int statusCode, String contentType, T body) {
    this.baseUrl = baseUrl;
    this.statusCode = statusCode;
    this.contentType = contentType;
    this.body = body;
  }

  @Override
  public ResponseEntity<T> asResponseEntity() {
    return ResponseEntity
      .status(statusCode)
      .contentType(MediaType.parseMediaType(contentType))
      .body(body);
  }
}

