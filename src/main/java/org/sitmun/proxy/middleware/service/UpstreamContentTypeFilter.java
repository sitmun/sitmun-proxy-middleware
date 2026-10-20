package org.sitmun.proxy.middleware.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
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
    private String springVisibleContentType;
    private boolean upstreamApplied;

    private ContentTypeResponse(HttpServletRequest request, HttpServletResponse response) {
      super(response);
      this.request = request;
    }

    @Override
    public void setHeader(String name, String value) {
      if (applyUpstream(name, value)) {
        return;
      }
      super.setHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
      if (applyUpstream(name, value)) {
        return;
      }
      super.addHeader(name, value);
    }

    @Override
    public void setContentType(String type) {
      if (applyUpstream(HttpHeaders.CONTENT_TYPE, type)) {
        return;
      }
      super.setContentType(type);
    }

    @Override
    public String getHeader(String name) {
      if (springVisibleContentType != null && isContentType(name)) {
        return springVisibleContentType;
      }
      return super.getHeader(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
      if (springVisibleContentType != null && isContentType(name)) {
        return List.of(springVisibleContentType);
      }
      return super.getHeaders(name);
    }

    @Override
    public String getContentType() {
      String upstream = upstream();
      if (upstreamApplied && upstream != null) {
        return upstream;
      }
      return super.getContentType();
    }

    private boolean applyUpstream(String name, String value) {
      String upstream = upstream();
      if (!isContentType(name) || upstream == null) {
        return false;
      }
      springVisibleContentType = value;
      super.setHeader(name, upstream);
      upstreamApplied = true;
      return true;
    }

    private String upstream() {
      Object raw = request.getAttribute(UpstreamContentType.ATTRIBUTE);
      if (raw instanceof String contentType && !contentType.isBlank()) {
        return contentType;
      }
      return null;
    }

    private static boolean isContentType(String name) {
      return HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name);
    }
  }
}
