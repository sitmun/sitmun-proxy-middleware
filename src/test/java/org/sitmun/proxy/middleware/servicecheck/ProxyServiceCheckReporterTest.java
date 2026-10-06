package org.sitmun.proxy.middleware.servicecheck;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.proxy.contract.ServiceCheckReport;
import org.sitmun.proxy.middleware.service.RequestConfigurationService;
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

class ProxyServiceCheckReporterTest {

  private static final Instant START = Instant.parse("2026-10-05T11:30:00Z");
  private static final ProxyCheckSubject WMS = new ProxyCheckSubject(7L, 3L, "WMS");
  private static final URI MAP = URI.create("http://maps.example/wms?SERVICE=WMS&REQUEST=GetMap");

  private final List<ServiceCheckReport> posted = new ArrayList<>();
  private ProxyServiceCheckReporter reporter;

  @BeforeEach
  void setUp() {
    reporter =
        new ProxyServiceCheckReporter(
            classifier(),
            new ProxyServiceCheckProperties(true, 5, Duration.ofSeconds(20), 128, 40),
            posted::add);
  }

  @Test
  @DisplayName("XML 200 fills exception text")
  void xml200FillsExceptionText() {
    byte[] body = exceptionReport("NoApplicableCode", "boom").getBytes(StandardCharsets.UTF_8);
    var signals = reporter.signals(exchange(200, "text/xml", body, START));

    assertThat(signals.ogcExceptionText()).isEqualTo("boom");
    assertThat(signals.ogcExceptionCode()).isEqualTo("NoApplicableCode");
  }

  @Test
  @DisplayName("Image 200 skips the scan")
  void image200SkipsTheScan() {
    byte[] body = exceptionReport("NoApplicableCode", "boom").getBytes(StandardCharsets.UTF_8);
    var signals = reporter.signals(exchange(200, "image/png", body, START));

    assertThat(signals.ogcExceptionText()).isEmpty();
    assertThat(signals.ogcExceptionCode()).isNull();
  }

  @Test
  @DisplayName("One server error does not post and the fifth does")
  void fifthServerErrorPosts() {
    for (int i = 0; i < 4; i++) {
      reporter.accept(WMS, exchange(500, "text/xml", new byte[0], START));
    }
    assertThat(posted).isEmpty();

    reporter.accept(WMS, exchange(500, "text/xml", new byte[0], START));

    assertThat(posted).hasSize(1);
    assertThat(posted.get(0).serviceId()).isEqualTo(7L);
    assertThat(posted.get(0).requestContext().probe()).isFalse();
    assertThat(posted.get(0).requestContext().request()).isEqualTo("GetMap");
  }

  @Test
  @DisplayName("HTTP 404 leaves a pending failure untouched")
  void http404LeavesPendingUntouched() {
    reporter.accept(WMS, exchange(500, "text/xml", new byte[0], START));
    reporter.accept(WMS, exchange(500, "text/xml", new byte[0], START));
    reporter.accept(WMS, exchange(404, "text/xml", new byte[0], START));
    reporter.accept(WMS, exchange(500, "text/xml", new byte[0], START));
    reporter.accept(WMS, exchange(500, "text/xml", new byte[0], START));
    assertThat(posted).isEmpty();

    reporter.accept(WMS, exchange(500, "text/xml", new byte[0], START));

    assertThat(posted).hasSize(1);
  }

  @Test
  @DisplayName("One up between failures clears pending")
  void upBetweenFailuresClearsPending() {
    reporter.accept(WMS, exchange(500, "text/xml", new byte[0], START));
    reporter.accept(WMS, exchange(500, "text/xml", new byte[0], START));
    reporter.accept(WMS, exchange(200, "image/png", new byte[0], START));
    for (int i = 0; i < 4; i++) {
      reporter.accept(WMS, exchange(500, "text/xml", new byte[0], START.plusSeconds(1)));
    }
    assertThat(posted).isEmpty();
  }

  @Test
  @DisplayName("A single pending failure posts when the window elapses")
  void singleFailurePostsAfterTheWindow() {
    reporter.accept(WMS, exchange(500, "text/xml", new byte[0], START));
    assertThat(posted).isEmpty();

    reporter.accept(WMS, exchange(500, "text/xml", new byte[0], START.plusSeconds(20)));

    assertThat(posted).hasSize(1);
  }

  @Test
  @DisplayName("An already reported status does not post again")
  void reportedStatusDoesNotPostAgain() {
    for (int i = 0; i < 5; i++) {
      reporter.accept(WMS, exchange(500, "text/xml", new byte[0], START));
    }
    reporter.accept(WMS, exchange(500, "text/xml", new byte[0], START.plusSeconds(1)));

    assertThat(posted).hasSize(1);
  }

  @Test
  @DisplayName("sql and api are not service-check subjects")
  void sqlAndApiAreNotSubjects() {
    assertThat(RequestConfigurationService.subjectFor(9, "sql", 4)).isNull();
    assertThat(RequestConfigurationService.subjectFor(9, "api", 4)).isNull();
    assertThat(RequestConfigurationService.subjectFor(9, "URL", 4)).isNull();
    assertThat(RequestConfigurationService.subjectFor(9, "wms", 4))
        .isEqualTo(new ProxyCheckSubject(4L, 9L, "WMS"));
  }

  private static UpstreamExchange exchange(
      int status, String contentType, byte[] body, Instant observedAt) {
    return new UpstreamExchange("GET", MAP, status, contentType, body, null, 15L, observedAt);
  }

  private static String exceptionReport(String code, String text) {
    return "<ExceptionReport><Exception exceptionCode=\""
        + code
        + "\"><ExceptionText>"
        + text
        + "</ExceptionText></Exception></ExceptionReport>";
  }

  private static ServiceCheckClassifier classifier() {
    return new ServiceCheckClassifier(
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
            new OtherTransportCapture()));
  }
}
