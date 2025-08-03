package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Fail.fail;

import java.io.IOException;
import java.util.List;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HttpClientFactory tests")
class HttpClientFactoryServiceTest {

  @Test
  @DisplayName("Fail with SSLHandshakeException")
  void failWithASSLHandhakeException() {
    String url = "https://self-signed.badssl.com ";
    List<String> unsafeAllowedHosts = Lists.list();
    HttpClient client = new HttpClientFactoryService(unsafeAllowedHosts);

    Request request = new Request.Builder().url(url).header("Accept", "*/*").build();

    try (Response response = client.executeRequest(request)) {
      fail("Unexpected exception");
    } catch (IOException e) {
    }
  }

  @Test
  @DisplayName("Any request use the unsafe client")
  void anyRequestUseTheUnsafeClient() {
    String url = "https://self-signed.badssl.com";
    List<String> unsafeAllowedHosts = Lists.list("*");

    HttpClient client = new HttpClientFactoryService(unsafeAllowedHosts);

    Request request = new Request.Builder().url(url).header("Accept", "*/*").build();
    try (Response response = client.executeRequest(request)) {
      // Do nothing
    } catch (IOException e) {
      fail("Unexpected exception");
    }
  }

  @Test
  @DisplayName("Use unsafe client when domain matches")
  void useUnsafeClientWhenDomainMatches() {
    String url = "https://self-signed.badssl.com";
    List<String> unsafeAllowedHosts = Lists.list("self-signed.badssl.com");

    HttpClient client = new HttpClientFactoryService(unsafeAllowedHosts);

    Request request = new Request.Builder().url(url).header("Accept", "*/*").build();

    try (Response response = client.executeRequest(request)) {
      // Do nothing
    } catch (IOException e) {
      fail("Unexpected exception");
    }
  }
}
