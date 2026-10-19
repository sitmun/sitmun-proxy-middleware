package org.sitmun.proxy.middleware.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class UpstreamContentTypeFilterTest {

  private final UpstreamContentTypeFilter filter = new UpstreamContentTypeFilter();

  @Test
  void replacesTheConverterContentTypeWithTheUpstreamValue() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(UpstreamContentType.ATTRIBUTE, "text/xml; subtype=gml/2.1.2");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request,
        response,
        (req, res) -> {
          res.setContentType("application/octet-stream");
          res.getOutputStream().write(new byte[] {1});
        });

    assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE))
        .isEqualTo("text/xml; subtype=gml/2.1.2");
    assertThat(response.getContentAsByteArray()).containsExactly(1);
  }

  @Test
  void writesTheUpstreamContentTypeWhenTheConverterSetsNone() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(UpstreamContentType.ATTRIBUTE, "application/json; charset=utf-8");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE))
        .isEqualTo("application/json; charset=utf-8");
  }

  @Test
  void leavesAConverterContentTypeWhenNothingWasRemembered() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        new MockHttpServletRequest(), response, (req, res) -> res.setContentType("image/png"));

    assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo("image/png");
  }

  @Test
  void addHeaderFromTheMessageConverterUsesTheUpstreamValue() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(UpstreamContentType.ATTRIBUTE, "text/xml; subtype=gml/2.1.2");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request,
        response,
        (req, res) ->
            ((HttpServletResponse) res)
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/octet-stream"));

    assertThat(response.getHeaders(HttpHeaders.CONTENT_TYPE))
        .containsExactly("text/xml; subtype=gml/2.1.2");
  }
}
