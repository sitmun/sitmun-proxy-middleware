package org.sitmun.proxy.middleware.mbtiles;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class MbtilesDtos {

  private MbtilesDtos() {}

  public record ClientCreateRequest(
      @JsonProperty("bbox") BboxDto bbox,
      @JsonProperty("minZoom") Integer minZoom,
      @JsonProperty("maxZoom") Integer maxZoom,
      @JsonProperty("srs") String srs,
      @JsonProperty("services") List<ServiceRefDto> services) {}

  public record BboxDto(
      @JsonProperty("minX") Double minX,
      @JsonProperty("minY") Double minY,
      @JsonProperty("maxX") Double maxX,
      @JsonProperty("maxY") Double maxY) {}

  public record ServiceRefDto(
      @JsonProperty("serviceId") Integer serviceId,
      @JsonProperty("layerIds") List<Integer> layerIds) {}

  public record BackendConfigRequest(
      @JsonProperty("appId") int appId,
      @JsonProperty("territoryId") int territoryId,
      @JsonProperty("action") String action,
      @JsonProperty("jobHandle") String jobHandle,
      @JsonProperty("bbox") BboxDto bbox,
      @JsonProperty("minZoom") Integer minZoom,
      @JsonProperty("maxZoom") Integer maxZoom,
      @JsonProperty("srs") String srs,
      @JsonProperty("services") List<ServiceRefDto> services) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record BackendConfigResponse(
      @JsonProperty("appId") int appId,
      @JsonProperty("territoryId") int territoryId,
      @JsonProperty("action") String action,
      @JsonProperty("username") String username,
      @JsonProperty("exp") long exp,
      @JsonProperty("tileRequest") Object tileRequest) {}

  public record CreateResponse(@JsonProperty("jobHandle") String jobHandle) {}
}
