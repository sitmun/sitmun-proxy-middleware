package org.sitmun.proxy.middleware.service;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public final class UpstreamContentType {

  public static final String ATTRIBUTE = "org.sitmun.proxy.upstream-content-type";

  private UpstreamContentType() {}

  public static void remember(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return;
    }
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return;
    }
    attributes.setAttribute(ATTRIBUTE, contentType, RequestAttributes.SCOPE_REQUEST);
  }

  public static String current() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return null;
    }
    Object value = attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    if (value instanceof String contentType) {
      return contentType;
    }
    return null;
  }
}
