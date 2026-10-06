package org.sitmun.proxy.middleware.servicecheck;

public record ProxyCheckSubject(long serviceId, long applicationId, String protocol) {

  private static final ThreadLocal<ProxyCheckSubject> CURRENT = new ThreadLocal<>();

  public static void set(ProxyCheckSubject subject) {
    if (subject == null) {
      CURRENT.remove();
    } else {
      CURRENT.set(subject);
    }
  }

  public static ProxyCheckSubject get() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
