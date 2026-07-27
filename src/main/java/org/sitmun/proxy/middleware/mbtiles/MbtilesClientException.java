package org.sitmun.proxy.middleware.mbtiles;

public class MbtilesClientException extends RuntimeException {

  private final int status;

  public MbtilesClientException(int status, String message) {
    super(message);
    this.status = status;
  }

  public int status() {
    return status;
  }
}
