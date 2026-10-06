package org.sitmun.proxy.middleware.usage;

public class UsageEndpointMissing extends RuntimeException {

  public UsageEndpointMissing() {
    super("service-usage");
  }
}
