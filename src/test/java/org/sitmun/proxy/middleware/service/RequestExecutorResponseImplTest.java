package org.sitmun.proxy.middleware.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class RequestExecutorResponseImplTest {

  @AfterEach
  void resetRequest() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void keepsALegalContentTypeUnchanged() {
    bindRequest();
    ResponseEntity<byte[]> entity =
        new RequestExecutorResponseImpl<>(
                "https://example.test", 200, "application/json; charset=utf-8", new byte[] {1})
            .asResponseEntity();

    assertThat(entity.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE)).isNull();
    assertThat(UpstreamContentType.current()).isEqualTo("application/json; charset=utf-8");
    assertThat(entity.getStatusCode().value()).isEqualTo(200);
    assertThat(entity.getBody()).containsExactly(1);
  }

  @Test
  void keepsGeoServerGml2ContentTypeUnchanged() {
    bindRequest();
    ResponseEntity<byte[]> entity =
        new RequestExecutorResponseImpl<>(
                "https://ide.cime.es/geoserver/turisme/ows",
                200,
                "text/xml; subtype=gml/2.1.2",
                new byte[] {1})
            .asResponseEntity();

    assertThat(entity.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE)).isNull();
    assertThat(UpstreamContentType.current()).isEqualTo("text/xml; subtype=gml/2.1.2");
    assertThat(entity.getStatusCode().value()).isEqualTo(200);
    assertThat(entity.getBody()).containsExactly(1);
  }

  private static void bindRequest() {
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(new MockHttpServletRequest()));
  }
}
