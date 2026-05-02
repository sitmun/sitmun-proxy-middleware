package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import javax.net.ssl.SSLHandshakeException;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HttpClientFactory tests")
class HttpClientFactoryServiceTest {

  @Test
  @DisplayName("Fail with SSLHandshakeException")
  void failWithSSLHandshakeException() {
    String url = "https://self-signed.badssl.com";
    List<String> unsafeAllowedHosts = Lists.list();
    HttpClient client = new HttpClientFactoryService(unsafeAllowedHosts);

    Request request = new Request.Builder().url(url).header("Accept", "*/*").build();

    assertThatThrownBy(
            () -> {
              try (Response ignored = client.executeRequest(request)) {
                fail(
                    "Expected IOException when using safe client against self-signed certificate,"
                        + " but request completed");
              }
            })
        .isInstanceOf(SSLHandshakeException.class);
  }

  @Test
  @DisplayName("Any request use the unsafe client")
  void anyRequestUseTheUnsafeClient() {
    String url = "https://self-signed.badssl.com";
    List<String> unsafeAllowedHosts = Lists.list("*");

    HttpClient client = new HttpClientFactoryService(unsafeAllowedHosts);

    Request request = new Request.Builder().url(url).header("Accept", "*/*").build();
    assertConnectsToSelfSignedHost(
        client, request, "TLS to self-signed host with wildcard allow list");
  }

  @Test
  @DisplayName("Use unsafe client when domain matches")
  void useUnsafeClientWhenDomainMatches() {
    String url = "https://self-signed.badssl.com";
    List<String> unsafeAllowedHosts = Lists.list("self-signed.badssl.com");

    HttpClient client = new HttpClientFactoryService(unsafeAllowedHosts);

    Request request = new Request.Builder().url(url).header("Accept", "*/*").build();
    assertConnectsToSelfSignedHost(
        client, request, "TLS to self-signed host with explicit host allow list");
  }

  private static void assertConnectsToSelfSignedHost(
      HttpClient client, Request request, String context) {
    assertThatCode(
            () -> {
              try (Response response = client.executeRequest(request)) {
                assertThat(response.isSuccessful()).as(context).isTrue();
              }
            })
        .as(context)
        .doesNotThrowAnyException();
  }
}
