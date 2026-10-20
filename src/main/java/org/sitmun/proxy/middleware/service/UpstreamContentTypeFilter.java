package org.sitmun.proxy.middleware.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import org.apache.james.mime4j.stream.NameValuePair;
import org.apache.james.mime4j.stream.RawBody;
import org.apache.james.mime4j.stream.RawField;
import org.apache.james.mime4j.stream.RawFieldParser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
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

    @Override
    public void setCharacterEncoding(String charset) {
      if (upstreamApplied) {
        return;
      }
      super.setCharacterEncoding(charset);
    }

    @Override
    public void setCharacterEncoding(Charset charset) {
      if (upstreamApplied) {
        return;
      }
      super.setCharacterEncoding(charset);
    }

    private boolean applyUpstream(String name, String value) {
      String upstream = upstream();
      if (!isContentType(name) || upstream == null) {
        return false;
      }
      String visible = visibleToSpring(upstream);
      springVisibleContentType = visible != null ? visible : value;
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

  static String visibleToSpring(String upstream) {
    RawBody body = RawFieldParser.DEFAULT.parseRawBody(new RawField("Content-Type", upstream));
    String type = body.getValue();
    if (type == null || !parses(type)) {
      return null;
    }
    String visible = type;
    for (NameValuePair parameter : body.getParams()) {
      String rendered = render(parameter);
      if (rendered == null) {
        continue;
      }
      String candidate = visible + "; " + rendered;
      if (parses(candidate)) {
        visible = candidate;
      }
    }
    return visible;
  }

  private static String render(NameValuePair parameter) {
    String name = parameter.getName();
    if (name == null || name.isBlank()) {
      return null;
    }
    String value = parameter.getValue();
    if (value == null) {
      return name;
    }
    return name + "=" + value;
  }

  private static boolean parses(String value) {
    try {
      MediaType.parseMediaType(value);
      return true;
    } catch (InvalidMediaTypeException ex) {
      return false;
    }
  }
}
