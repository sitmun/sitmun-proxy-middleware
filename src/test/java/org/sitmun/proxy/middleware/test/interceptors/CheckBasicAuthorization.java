package org.sitmun.proxy.middleware.test.interceptors;

import java.io.IOException;
import java.util.Base64;
import lombok.Getter;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.sitmun.proxy.middleware.protocols.http.HttpSecurityConstants;

@Getter
public class CheckBasicAuthorization implements Interceptor {
  private String expectation;

  @NotNull
  @Override
  public Response intercept(@NotNull Chain chain) throws IOException {
    String authorization = chain.request().header(HttpSecurityConstants.HEADER_AUTHORIZATION);
    if (authorization != null
        && authorization.startsWith(HttpSecurityConstants.AUTH_SCHEME_BASIC_PREFIX.trim())) {
      expectation =
          new String(
              Base64.getDecoder()
                  .decode(
                      authorization.substring(
                          HttpSecurityConstants.AUTH_SCHEME_BASIC_PREFIX.length())));
    }
    return chain.proceed(chain.request());
  }
}
