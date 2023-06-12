package org.sitmun.proxy.middleware.interceptors;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Base64;

@Component
public class BasicSecurityRequestInterceptor implements TestInterceptor {

  @NotNull
  @Override
  public Response intercept(Chain arg0) throws IOException {
    Request request = arg0.request();
    String authorization = request.header("Authorization");
    Response response = arg0.proceed(request);
    if (authorization != null && authorization.startsWith("Basic")) {
      String token = decodeAuthorization(authorization);
      ResponseBody body = ResponseBody.create(token, MediaType.get("application/json"));
      response = response.newBuilder().body(body).code(200).build();
    }
    return response;
	}

	private String decodeAuthorization(String authorization) {
		String token = null;
		if (StringUtils.hasText(authorization)) {
			token = new String(Base64.getDecoder().decode(authorization.substring(6)));
		}
		return token;
	}

}
