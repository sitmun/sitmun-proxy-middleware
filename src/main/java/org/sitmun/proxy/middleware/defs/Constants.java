package org.sitmun.proxy.middleware.defs;

public class Constants {

  private Constants() {
    throw new IllegalStateException("Utility class");
  }

  public static final String SERVICE_URL = "/services/{id}";

  public static final String SERVICE_PARAMETERS_URL = "/services/{id}/parameters";
}
