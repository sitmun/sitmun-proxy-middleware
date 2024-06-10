package org.sitmun.proxy.middleware.test.interceptors;

import lombok.Getter;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Base64;


@Getter
public class CheckBasicAuthorization implements Interceptor {
  private String expectation;

  @NotNull
  @Override
  public Response intercept(@NotNull Chain chain) throws IOException {
    String authorization = chain.request().header("Authorization");
    if (authorization != null && authorization.startsWith("Basic")) {
      expectation = new String(Base64.getDecoder().decode(authorization.substring(6)));
    }
    return chain.proceed(chain.request());
  }
}
