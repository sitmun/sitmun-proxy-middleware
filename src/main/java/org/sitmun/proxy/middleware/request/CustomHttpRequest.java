package org.sitmun.proxy.middleware.request;


import lombok.Getter;
import okhttp3.Request;
import okhttp3.Request.Builder;

@Getter
public class CustomHttpRequest {

  private final Builder requestBuilder;

  public CustomHttpRequest() {
    requestBuilder = new Request.Builder();
  }
}
