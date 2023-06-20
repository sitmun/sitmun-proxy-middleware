package org.sitmun.proxy.middleware.service;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.Priority;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Profile("test")
@Priority(1)
@Service
public class ClientServiceTestImpl implements ClientService {

  private final List<Interceptor> interceptors = new ArrayList<>();

  private OkHttpClient httpClient;

  public ClientServiceTestImpl(List<Interceptor> interceptors) {
    this.interceptors.addAll(interceptors);
    httpClient = build();
  }

  private OkHttpClient build() {
    Builder builder = new Builder();
    for (Interceptor ti : interceptors) {
      builder.addInterceptor(ti);
    }
    return builder.build();
  }

  @Override
  public Response executeRequest(Request httpRequest) throws IOException {
    return httpClient.newCall(httpRequest).execute();
  }

  public void addInterceptor(Interceptor interceptor) {
    if (!interceptors.contains(interceptor)) {
      interceptors.add(interceptor);
      httpClient = build();
    }
  }

  public void removeInterceptor(Interceptor interceptor) {
    if (interceptors.contains(interceptor)) {
      interceptors.remove(interceptor);
      httpClient = build();
    }
  }

}
