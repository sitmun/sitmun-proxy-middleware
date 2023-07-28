package org.sitmun.proxy.middleware.test.services;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;
import org.sitmun.proxy.middleware.service.ClientService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.Priority;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Profile("test")
@Priority(1)
@Service
public class TestClientService implements ClientService {

  private final List<Interceptor> interceptors = new ArrayList<>();

  private OkHttpClient httpClient;

  public TestClientService(List<Interceptor> interceptors) {
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

  public void addInterceptors(Interceptor... interceptor) {
    for (Interceptor i : interceptor) {
      if (!interceptors.contains(i)) {
        interceptors.add(i);
      }
    }
    httpClient = build();
  }

  public void removeInterceptor(Interceptor interceptor) {
    if (interceptors.contains(interceptor)) {
      interceptors.remove(interceptor);
      httpClient = build();
    }
  }

  public void removeAllInterceptors() {
    interceptors.clear();
    httpClient = build();
  }
}
