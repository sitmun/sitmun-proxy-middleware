package org.sitmun.proxy.middleware.mbtiles;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.sitmun.proxy.middleware.mbtiles.MbtilesDtos.BboxDto;
import org.sitmun.proxy.middleware.mbtiles.MbtilesDtos.ClientCreateRequest;
import org.sitmun.proxy.middleware.mbtiles.MbtilesDtos.ServiceRefDto;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MbtilesRequestValidator {

  private final MbtilesProperties properties;

  public MbtilesRequestValidator(MbtilesProperties properties) {
    this.properties = properties;
  }

  public void validateJsonSize(String rawJson) {
    if (rawJson == null) {
      return;
    }
    if (rawJson.getBytes(StandardCharsets.UTF_8).length > properties.maxJsonBytes()) {
      throw new MbtilesClientException(400, "Request body exceeds configured size limit");
    }
  }

  public void validateCreateOrEstimate(ClientCreateRequest request) {
    if (request == null) {
      throw new MbtilesClientException(400, "Request body is required");
    }
    validateBbox(request.bbox());
    if (request.minZoom() == null || request.maxZoom() == null) {
      throw new MbtilesClientException(400, "minZoom and maxZoom are required");
    }
    if (request.minZoom() < 0 || request.maxZoom() < 0) {
      throw new MbtilesClientException(400, "Zoom levels must be non-negative");
    }
    if (request.minZoom() > request.maxZoom()) {
      throw new MbtilesClientException(400, "minZoom must be less than or equal to maxZoom");
    }
    if (request.maxZoom() - request.minZoom() > properties.maxZoomSpan()) {
      throw new MbtilesClientException(400, "Zoom span exceeds configured limit");
    }
    if (!StringUtils.hasText(request.srs()) || !properties.allowedSrs().contains(request.srs())) {
      throw new MbtilesClientException(400, "Unsupported or missing SRS");
    }
    List<ServiceRefDto> services = request.services();
    if (services == null || services.isEmpty()) {
      throw new MbtilesClientException(400, "At least one service reference is required");
    }
    for (ServiceRefDto service : services) {
      if (service == null
          || service.serviceId() == null
          || service.layerIds() == null
          || service.layerIds().isEmpty()) {
        throw new MbtilesClientException(400, "Invalid service reference");
      }
    }
  }

  private static void validateBbox(BboxDto bbox) {
    if (bbox == null
        || bbox.minX() == null
        || bbox.minY() == null
        || bbox.maxX() == null
        || bbox.maxY() == null) {
      throw new MbtilesClientException(400, "bbox is required");
    }
    if (!Double.isFinite(bbox.minX())
        || !Double.isFinite(bbox.minY())
        || !Double.isFinite(bbox.maxX())
        || !Double.isFinite(bbox.maxY())) {
      throw new MbtilesClientException(400, "bbox coordinates must be finite");
    }
    if (bbox.minX() > bbox.maxX() || bbox.minY() > bbox.maxY()) {
      throw new MbtilesClientException(400, "bbox bounds are invalid");
    }
  }
}
