package org.sitmun.proxy.middleware.test.interceptors;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HostnameCheck implements Interceptor {

  private String expectation;

  public String getExpectation() {
    return expectation;
  }

  @NotNull
  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    expectation = request.url().host();
    return chain.proceed(chain.request());
  }
}
