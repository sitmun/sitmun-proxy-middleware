package org.sitmun.proxy.middleware.protocols.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HttpClientFactoryService implements HttpClient {

  private final List<String> unsafeAllowedHosts;
  private OkHttpClient safeClient;
  private OkHttpClient unsafeClient;
  private final List<Interceptor> interceptors = new ArrayList<>();

  public HttpClientFactoryService(
      @Value("${sitmun.client.unsafe-allowed-hosts:*}") List<String> unsafeAllowedHosts) {
    this.unsafeAllowedHosts = unsafeAllowedHosts;
    safeClient = createSafeClient();
    unsafeClient = createUnsafeClient();
  }

  private OkHttpClient createUnsafeClient() {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    interceptors.forEach(builder::addInterceptor);
    return configureToIgnoreCertificate(builder).build();
  }

  private OkHttpClient createSafeClient() {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    interceptors.forEach(builder::addInterceptor);
    return builder.build();
  }

  @Override
  public Response executeRequest(Request httpRequest) throws IOException {
    return getClient(httpRequest.url().host()).newCall(httpRequest).execute();
  }

  public OkHttpClient getClient(String host) {
    try {
      if (unsafeAllowedHosts.contains("*") || unsafeAllowedHosts.contains(host)) {
        log.warn("Using Unsafe Client");
        return unsafeClient;
      } else {
        return safeClient;
      }
    } catch (Exception e) {
      log.warn("Exception while creating client: {}", e.getMessage(), e);
      return safeClient;
    }
  }

  private static OkHttpClient.Builder configureToIgnoreCertificate(OkHttpClient.Builder builder) {
    log.warn("Ignore SSL Certificate");
    try {

      // Create a trust manager that does not validate certificate chains
      final TrustManager[] trustAllCerts =
          new TrustManager[] {
            new X509TrustManager() {
              @Override
              public void checkClientTrusted(
                  java.security.cert.X509Certificate[] chain, String authType) {
                // No implementation needed for ignoring SSL certificate validation
              }

              @Override
              public void checkServerTrusted(
                  java.security.cert.X509Certificate[] chain, String authType) {
                // No implementation needed for ignoring SSL certificate validation
              }

              @Override
              public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
              }
            }
          };

      // Install the all-trusting trust manager
      final SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
      // Create a ssl socket factory with our all-trusting manager
      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

      builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
      builder.hostnameVerifier((hostname, session) -> true);
    } catch (Exception e) {
      log.warn("Exception while configuring IgnoreSslCertificate: {}", e.getMessage(), e);
    }
    return builder;
  }

  public void removeAllInterceptors() {
    interceptors.clear();
    safeClient = createSafeClient();
    unsafeClient = createUnsafeClient();
  }

  public void addInterceptor(Interceptor interceptor) {
    if (!interceptors.contains(interceptor)) {
      interceptors.add(interceptor);
      safeClient = createSafeClient();
      unsafeClient = createUnsafeClient();
    }
  }

  public void addInterceptors(Interceptor... interceptor) {
    for (Interceptor i : interceptor) {
      if (!interceptors.contains(i)) {
        interceptors.add(i);
      }
    }
    safeClient = createSafeClient();
    unsafeClient = createUnsafeClient();
  }

  public void removeInterceptor(Interceptor interceptor) {
    if (interceptors.contains(interceptor)) {
      interceptors.remove(interceptor);
      safeClient = createSafeClient();
      unsafeClient = createUnsafeClient();
    }
  }
}
