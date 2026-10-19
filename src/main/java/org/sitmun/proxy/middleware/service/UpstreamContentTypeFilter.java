package org.sitmun.proxy.middleware.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class UpstreamContentTypeFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    chain.doFilter(request, new ContentTypeResponse(request, response));
    Object raw = request.getAttribute(UpstreamContentType.ATTRIBUTE);
    if (raw instanceof String contentType && !contentType.isBlank() && !response.isCommitted()) {
      response.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
    }
  }

  private static final class ContentTypeResponse extends HttpServletResponseWrapper {

    private final HttpServletRequest request;

    private ContentTypeResponse(HttpServletRequest request, HttpServletResponse response) {
      super(response);
      this.request = request;
    }

    @Override
    public void setHeader(String name, String value) {
      super.setHeader(name, contentType(name, value));
    }

    @Override
    public void addHeader(String name, String value) {
      super.addHeader(name, contentType(name, value));
    }

    @Override
    public void setContentType(String type) {
      super.setContentType(contentType(HttpHeaders.CONTENT_TYPE, type));
    }

    private String contentType(String name, String value) {
      if (!HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
        return value;
      }
      Object raw = request.getAttribute(UpstreamContentType.ATTRIBUTE);
      if (raw instanceof String contentType && !contentType.isBlank()) {
        return contentType;
      }
      return value;
    }
  }
}
