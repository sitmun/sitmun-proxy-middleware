package org.sitmun.proxy.middleware.test.interceptors;

import java.io.IOException;
import lombok.Getter;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Getter
@Component
public class QueryCheck implements Interceptor {

  private String expectation;

  @NotNull
  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    expectation = request.url().query();
    return chain.proceed(chain.request());
  }
}
