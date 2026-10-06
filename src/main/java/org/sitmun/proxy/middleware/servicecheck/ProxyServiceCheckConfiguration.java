package org.sitmun.proxy.middleware.servicecheck;

import org.sitmun.proxy.middleware.usage.ProxyUsageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ProxyServiceCheckProperties.class, ProxyUsageProperties.class})
public class ProxyServiceCheckConfiguration {}
