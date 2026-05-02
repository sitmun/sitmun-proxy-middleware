package org.sitmun.proxy.middleware.protocols.wms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class WmsCapabilitiesPropertiesBindingTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(TestConfig.class)
          .withPropertyValues(
              "sitmun.ogc.capabilities.service-paths[0]=foo",
              "sitmun.ogc.capabilities.service-paths[1]=bar",
              "sitmun.ogc.capabilities.extra-sources[0]=https://example.com");

  @Test
  void bindsServicePathsAndExtraSources() {
    contextRunner.run(
        ctx -> {
          assertThat(ctx).hasSingleBean(WmsCapabilitiesProperties.class);
          WmsCapabilitiesProperties props = ctx.getBean(WmsCapabilitiesProperties.class);
          assertThat(props.getServicePaths()).containsExactly("foo", "bar");
          assertThat(props.getExtraSources()).containsExactly("https://example.com");
        });
  }

  @Configuration
  @EnableConfigurationProperties(WmsCapabilitiesProperties.class)
  static class TestConfig {}
}
