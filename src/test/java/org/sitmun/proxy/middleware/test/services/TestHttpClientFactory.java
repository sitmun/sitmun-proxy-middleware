package org.sitmun.proxy.middleware.test.services;

import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.proxy.middleware.service.ClientService;
import org.sitmun.proxy.middleware.service.HttpClientFactory;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("HttpClientFactory tests")
public class TestHttpClientFactory {

    @Test
    @DisplayName("Fail with SSLHandshakeException")
    public void failWithASSLHandhakeException() {
      String url = "https://ovc.catastro.meh.es/Cartografia/WMS/ServidorWMS.aspx";
      List<String> unsafeAllowedHosts = Lists.list();
      ClientService client = new HttpClientFactory(unsafeAllowedHosts);

      Request request = new Request.Builder()
        .url(url)
        .header("Accept", "*/*")
        .build();
      assertThrows(SSLHandshakeException.class, () -> {
        try (Response response = client.executeRequest(request)) {
          // Do nothing
        }
      });
    }

    @Test
    @DisplayName("Any request use the unsafe client")
    public void anyRequestUseTheUnsafeClient() {
      String url = "https://ovc.catastro.meh.es/Cartografia/WMS/ServidorWMS.aspx";
      List<String> unsafeAllowedHosts = Lists.list("*");

      ClientService client = new HttpClientFactory(unsafeAllowedHosts);

      Request request = new Request.Builder()
        .url(url)
        .header("Accept", "*/*")
        .build();
      try(Response response = client.executeRequest(request)) {
        // Do nothing
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Test
    @DisplayName("Use unsafe client when domain matches")
    public void useUnsafeClientWhenDomainMatches() {
      String url = "https://ovc.catastro.meh.es/Cartografia/WMS/ServidorWMS.aspx";
      List<String> unsafeAllowedHosts = Lists.list("ovc.catastro.meh.es");

      ClientService client = new HttpClientFactory(unsafeAllowedHosts);

      Request request = new Request.Builder()
        .url(url)
        .header("Accept", "*/*")
        .build();

      try(Response response = client.executeRequest(request)) {
        // Do nothing
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
