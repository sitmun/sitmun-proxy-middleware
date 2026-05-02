package org.sitmun.proxy.middleware.protocols.wms;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sitmun.ogc.capabilities")
@Getter
@Setter
public class WmsCapabilitiesProperties {

  private List<String> servicePaths = new ArrayList<>(List.of("wms", "wfs", "wcs", "ows"));

  private List<String> extraSources = new ArrayList<>();
}
