package org.sitmun.proxy.middleware.test.interceptors;

import java.io.IOException;
import lombok.Getter;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Getter
@Component
public class HostnameCheck implements Interceptor {

  private String expectation;

  @NotNull
  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    expectation = request.url().host();
    return chain.proceed(chain.request());
  }
}
