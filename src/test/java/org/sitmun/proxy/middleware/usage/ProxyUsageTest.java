package org.sitmun.proxy.middleware.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.proxy.contract.ServiceUsage;
import org.sitmun.proxy.contract.ServiceUsageReport;
import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.protocols.http.HttpClient;
import org.sitmun.proxy.middleware.protocols.http.HttpContext;
import org.sitmun.proxy.middleware.protocols.http.HttpRequestDecoratorAddEndpoint;
import org.sitmun.proxy.middleware.protocols.http.HttpRequestExecutor;
import org.sitmun.proxy.middleware.service.RequestConfigurationService;
import org.sitmun.proxy.middleware.service.RequestExecutor;
import org.sitmun.proxy.middleware.service.RequestExecutorFactory;
import org.sitmun.proxy.middleware.service.RequestExecutorResponse;
import org.sitmun.proxy.middleware.service.RequestExecutorService;
import org.sitmun.proxy.middleware.servicecheck.ProxyCheckSubject;
import org.sitmun.proxy.middleware.servicecheck.ProxyServiceCheckProperties;
import org.sitmun.proxy.middleware.servicecheck.ProxyServiceCheckReporter;
import org.sitmun.proxy.middleware.servicecheck.UpstreamExchange;
import org.sitmun.upstream.signal.ServiceCheckClassifier;
import org.sitmun.upstream.signal.capture.AuthFailedCapture;
import org.sitmun.upstream.signal.capture.ClientErrorCapture;
import org.sitmun.upstream.signal.capture.ConnectCapture;
import org.sitmun.upstream.signal.capture.InterruptedReadCapture;
import org.sitmun.upstream.signal.capture.OgcExceptionCapture;
import org.sitmun.upstream.signal.capture.OtherTransportCapture;
import org.sitmun.upstream.signal.capture.ProtocolSuccessCapture;
import org.sitmun.upstream.signal.capture.ServerErrorCapture;
import org.sitmun.upstream.signal.capture.SocketTimeoutCapture;
import org.sitmun.upstream.signal.capture.TlsCapture;
import org.sitmun.upstream.signal.capture.UnknownHostCapture;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ProxyUsageTest {

  private static final Instant NOW = Instant.parse("2026-10-05T12:10:00Z");
  private static final ProxyCheckSubject WMS = new ProxyCheckSubject(7L, 9L, "WMS");
  private static final String GET_MAP = "http://maps.example/wms?SERVICE=WMS&REQUEST=GetMap";

  @Mock private HttpClient httpClient;
  @Mock private Response response;
  @Mock private ResponseBody responseBody;
  @Mock private HttpContext httpContext;

  private final List<ServiceUsageReport> posted = new ArrayList<>();
  private ProxyUsage usage;
  private RequestExecutorService executor;

  @BeforeEach
  void setUp() {
    ProxyServiceCheckReporter reporter = reporter();
    usage =
        new ProxyUsage(
            new ProxyUsageProperties(true, Duration.ofMinutes(5), Duration.ofHours(24)),
            reporter,
            posted::add,
            Clock.fixed(NOW, ZoneOffset.UTC));
    executor = executor(reporter, usage);
  }

  @AfterEach
  void clearSubject() {
    ProxyCheckSubject.clear();
  }

  @Test
  @DisplayName("A real request name is kept and a missing request stays Other")
  void realRequestNameIsKept() {
    assertThat(ProxyUsage.operation(URI.create("http://maps.example/wms?REQUEST=GetLegendGraphic")))
        .isEqualTo("GetLegendGraphic");
    assertThat(ProxyUsage.operation(URI.create("http://maps.example/wms?REQUEST=getmap")))
        .isEqualTo("GetMap");
    assertThat(ProxyUsage.operation(URI.create("http://maps.example/wms?REQUEST=Other")))
        .isEqualTo("Other");
    assertThat(ProxyUsage.operation(URI.create("http://maps.example/wms?SERVICE=WMS")))
        .isEqualTo("Other");
  }

  @Test
  @DisplayName("Every OGC forward counts once and a GetMap 404 is failed")
  void ogcForwardCountsOnceAndAGetMap404IsFailed() throws Exception {
    stubHttp(200, "image/png", new byte[0]);
    ProxyCheckSubject.set(WMS);

    executor.executeRequest("http://maps.example", httpContext);
    executor.executeRequest("http://maps.example", httpContext);

    assertThat(usage.pending())
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.serviceId()).isEqualTo(7L);
              assertThat(row.applicationId()).isEqualTo(9L);
              assertThat(row.operation()).isEqualTo("GetMap");
              assertThat(row.requests()).isEqualTo(2);
              assertThat(row.failed()).isZero();
            });

    stubHttp(404, "text/plain", new byte[0]);
    executor.executeRequest("http://maps.example", httpContext);

    assertThat(usage.pending())
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.requests()).isEqualTo(3);
              assertThat(row.failed()).isEqualTo(1);
            });
  }

  @Test
  @DisplayName("sql and api forwards are not counted")
  void sqlAndApiAreNotCounted() throws Exception {
    assertThat(RequestConfigurationService.subjectFor(9, "sql", 4)).isNull();
    assertThat(RequestConfigurationService.subjectFor(9, "api", 4)).isNull();
    assertThat(RequestConfigurationService.subjectFor(9, "URL", 4)).isNull();

    stubHttp(200, "image/png", new byte[0]);
    executor.executeRequest("http://maps.example", httpContext);
    executor.executeRequest("jdbc:example", (Context) () -> "sql");
    ProxyCheckSubject.set(WMS);
    executor.executeRequest("jdbc:example", (Context) () -> "sql");

    assertThat(usage.pending()).isEmpty();
  }

  @Test
  @DisplayName("A flush clears only the counts it sent")
  void flushClearsWhatItSent() {
    AtomicReference<ProxyUsage> current = new AtomicReference<>();
    usage =
        new ProxyUsage(
            new ProxyUsageProperties(true, Duration.ofMinutes(5), Duration.ofHours(24)),
            reporter(),
            report -> {
              posted.add(report);
              current
                  .get()
                  .record(WMS, exchange(200, "image/png", new byte[0], NOW.plusSeconds(1)));
            },
            Clock.fixed(NOW, ZoneOffset.UTC));
    current.set(usage);

    usage.record(WMS, exchange(200, "image/png", new byte[0], NOW));
    usage.flush();

    assertThat(posted).hasSize(1);
    assertThat(posted.get(0).usages())
        .singleElement()
        .extracting(ServiceUsage::requests)
        .isEqualTo(1L);
    assertThat(usage.pending()).singleElement().extracting(ServiceUsage::requests).isEqualTo(1L);
  }

  @Test
  @DisplayName("A failed flush keeps the counts")
  void failedFlushKeepsTheCounts() {
    usage =
        new ProxyUsage(
            new ProxyUsageProperties(true, Duration.ofMinutes(5), Duration.ofHours(24)),
            reporter(),
            report -> {
              throw new IllegalStateException("down");
            },
            Clock.fixed(NOW, ZoneOffset.UTC));

    usage.record(WMS, exchange(200, "image/png", new byte[0], NOW));
    usage.flush();

    assertThat(posted).isEmpty();
    assertThat(usage.pending()).singleElement().extracting(ServiceUsage::requests).isEqualTo(1L);
  }

  @Test
  @DisplayName("A missing usage endpoint drops the batch and logs once")
  void missingEndpointDropsTheBatch() {
    Logger logger = (Logger) LoggerFactory.getLogger(ProxyUsage.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      usage =
          new ProxyUsage(
              new ProxyUsageProperties(true, Duration.ofMinutes(5), Duration.ofHours(24)),
              reporter(),
              report -> {
                throw new UsageEndpointMissing();
              },
              Clock.fixed(NOW, ZoneOffset.UTC));

      usage.record(WMS, exchange(200, "image/png", new byte[0], NOW));
      usage.flush();
      usage.flush();

      assertThat(usage.pending()).isEmpty();
      assertThat(appender.list).hasSize(1);
      assertThat(appender.list.get(0).getFormattedMessage()).contains("missing");
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  @DisplayName("The backlog drops the oldest hour past usage.backlog")
  void backlogDropsTheOldestHour() {
    usage.record(WMS, exchange(200, "image/png", new byte[0], NOW.minus(Duration.ofHours(25))));
    usage.record(WMS, exchange(200, "image/png", new byte[0], NOW.minus(Duration.ofHours(1))));
    usage.flush();

    assertThat(posted).hasSize(1);
    assertThat(posted.get(0).usages())
        .singleElement()
        .extracting(ServiceUsage::hourStart)
        .isEqualTo(NOW.minus(Duration.ofHours(1)).truncatedTo(java.time.temporal.ChronoUnit.HOURS));
    assertThat(usage.pending()).isEmpty();
  }

  private void stubHttp(int status, String contentType, byte[] body) throws Exception {
    when(httpContext.getUri()).thenReturn(GET_MAP);
    when(httpContext.getParameters()).thenReturn(Map.of());
    when(response.body()).thenReturn(responseBody);
    when(responseBody.bytes()).thenReturn(body);
    when(response.code()).thenReturn(status);
    when(response.header("content-type")).thenReturn(contentType);
    when(httpClient.executeRequest(any(Request.class))).thenReturn(response);
  }

  private RequestExecutorService executor(ProxyServiceCheckReporter reporter, ProxyUsage counted) {
    RequestExecutorFactory factory =
        new RequestExecutorFactory(httpClient) {
          @Override
          public RequestExecutor create(String baseUrl, Context context) {
            if (context instanceof HttpContext) {
              return new HttpRequestExecutor(baseUrl, httpClient);
            }
            return new RequestExecutor() {
              @Override
              public <T> RequestExecutorResponse<T> execute() {
                return () -> ResponseEntity.ok().build();
              }

              @Override
              public String describe() {
                return "sql";
              }
            };
          }
        };
    return new RequestExecutorService(
        factory,
        List.of(new HttpRequestDecoratorAddEndpoint()),
        List.of(),
        reporter,
        new ProxyServiceCheckProperties(false, 5, Duration.ofSeconds(20), 128, 40),
        counted,
        new ProxyUsageProperties(true, Duration.ofMinutes(5), Duration.ofHours(24)));
  }

  private static ProxyServiceCheckReporter reporter() {
    return new ProxyServiceCheckReporter(
        new ServiceCheckClassifier(
            List.of(
                new SocketTimeoutCapture(),
                new InterruptedReadCapture(),
                new UnknownHostCapture(),
                new ConnectCapture(),
                new TlsCapture(),
                new AuthFailedCapture(),
                new ProtocolSuccessCapture(),
                new ClientErrorCapture(),
                new ServerErrorCapture(),
                new OgcExceptionCapture(),
                new OtherTransportCapture())),
        new ProxyServiceCheckProperties(false, 5, Duration.ofSeconds(20), 128, 40),
        report -> {});
  }

  private static UpstreamExchange exchange(
      int status, String contentType, byte[] body, Instant observedAt) {
    return new UpstreamExchange(
        "GET", java.net.URI.create(GET_MAP), status, contentType, body, null, 1L, observedAt);
  }
}
