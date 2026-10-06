package org.sitmun.proxy.middleware.protocols.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.proxy.middleware.service.RequestExecutorResponse;

@ExtendWith(MockitoExtension.class)
@Timeout(2)
class HttpRequestExecutorReportTest {

  @Mock private HttpClient httpClient;
  @Mock private Response response;
  @Mock private ResponseBody responseBody;

  private final ExecutorService listenerPool = Executors.newSingleThreadExecutor();

  @AfterEach
  void stopPool() {
    listenerPool.shutdownNow();
  }

  @Test
  @DisplayName("The viewer response does not wait on the service-check report")
  void viewerResponseDoesNotWaitOnTheReport() throws Exception {
    HttpRequestExecutor executor = new HttpRequestExecutor("http://maps.example", httpClient);
    executor.setUrl("http://maps.example/wms");
    when(response.body()).thenReturn(responseBody);
    when(responseBody.bytes()).thenReturn(new byte[] {1, 2, 3});
    when(response.code()).thenReturn(200);
    when(response.header("content-type")).thenReturn("image/png");
    when(httpClient.executeRequest(any(Request.class))).thenReturn(response);

    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    executor.setListenerExecutor(listenerPool);
    executor.setExchangeListener(
        exchange -> {
          started.countDown();
          try {
            release.await();
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          }
        });

    long startedAt = System.nanoTime();
    RequestExecutorResponse<?> result = executor.execute();
    long elapsed = System.nanoTime() - startedAt;

    assertThat(result.asResponseEntity().getStatusCode().value()).isEqualTo(200);
    assertThat(Duration.ofNanos(elapsed)).isLessThan(Duration.ofMillis(500));
    assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
    release.countDown();
  }

  @Test
  @DisplayName("A transport failure still returns before the report")
  void transportFailureReturnsBeforeTheReport() throws Exception {
    HttpRequestExecutor executor = new HttpRequestExecutor("http://maps.example", httpClient);
    executor.setUrl("http://maps.example/wms");
    when(httpClient.executeRequest(any(Request.class))).thenThrow(new IOException("reset"));
    CountDownLatch started = new CountDownLatch(1);
    executor.setListenerExecutor(listenerPool);
    executor.setExchangeListener(exchange -> started.countDown());

    RequestExecutorResponse<?> result = executor.execute();

    assertThat(result.asResponseEntity().getStatusCode().value()).isEqualTo(503);
    assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
  }
}
