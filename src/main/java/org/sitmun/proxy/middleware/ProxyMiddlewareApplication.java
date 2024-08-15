package org.sitmun.proxy.middleware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ProxyMiddlewareApplication {

  public static void main(String[] args) {
    SpringApplication.run(ProxyMiddlewareApplication.class, args);
  }

}