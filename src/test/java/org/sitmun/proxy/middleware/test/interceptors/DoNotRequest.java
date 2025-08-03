package org.sitmun.proxy.middleware.test.interceptors;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

public class DoNotRequest implements Interceptor {
  public static final Interceptor INSTANCE = new DoNotRequest();

  @NotNull
  @Override
  public Response intercept(@NotNull Chain chain) {
    return new Response.Builder()
        .code(418) // Whatever code
        .body(ResponseBody.create("", MediaType.parse("plain/text"))) // Whatever body
        .addHeader("Content-Type", "plain/text")
        .protocol(Protocol.HTTP_2)
        .message("Dummy response")
        .request(chain.request())
        .build();
  }
}
