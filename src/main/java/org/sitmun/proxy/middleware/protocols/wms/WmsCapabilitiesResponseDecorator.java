package org.sitmun.proxy.middleware.protocols.wms;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.proxy.middleware.decorator.Context;
import org.sitmun.proxy.middleware.decorator.ResponseDecorator;
import org.sitmun.proxy.middleware.service.RequestExecutorResponseImpl;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WmsCapabilitiesResponseDecorator implements ResponseDecorator {

  private final WmsCapabilitiesProperties properties;

  @Override
  public boolean accept(Object target, Context context) {
    if (context instanceof WmsPayloadDto wmsPayloadDto) {
      return wmsPayloadDto
          .getParameters()
          .getOrDefault("REQUEST", "")
          .equalsIgnoreCase("GetCapabilities");
    }
    return false;
  }

  @Override
  public void addBehavior(Object response, Context context) {
    log.info("Adding behavior to response {} of {}", response, context);
    if (context instanceof WmsPayloadDto wmsPayloadDto) {
      //noinspection unchecked
      RequestExecutorResponseImpl<byte[]> requestExecutionResponseImpl1 =
          (RequestExecutorResponseImpl<byte[]>) response;
      String s = new String(requestExecutionResponseImpl1.getBody(), StandardCharsets.UTF_8);
      String fullUri = wmsPayloadDto.getUri();
      String baseUri = fullUri.split("\\?")[0];

      log.debug("WMS PAYLOAD FULL URI: {}", fullUri);
      log.debug("WMS PAYLOAD BASE URI: {}", baseUri);
      log.debug("REQ EXEC RES URL: {}", requestExecutionResponseImpl1.getBaseUrl());

      String baseUrl = requestExecutionResponseImpl1.getBaseUrl();
      List<String> servicePaths = properties.getServicePaths();
      String servicePathSuffixes = String.join("|", servicePaths);

      String servicePath = baseUri.replaceAll("/(?:" + servicePathSuffixes + ")/?$", "");
      log.info("SERVICE PATH: {}", servicePath);
      s = replace(s, servicePath, servicePathSuffixes, baseUrl);

      for (String extraSource : properties.getExtraSources()) {
        String normalizedSource = extraSource.replaceAll("/(?:" + servicePathSuffixes + ")/?$", "");
        log.info("EXTRA SOURCE: {}", normalizedSource);
        s = replace(s, normalizedSource, servicePathSuffixes, baseUrl);
      }

      requestExecutionResponseImpl1.setBody(s.getBytes(StandardCharsets.UTF_8));
    }
  }

  /** Replaces all quoted occurrences of {@code source} */
  private String replace(String content, String source, String servicePathSuffixes, String target) {
    Pattern pattern =
        Pattern.compile(
            "(?<=[\"'])"
                + Pattern.quote(source)
                + "(?:/(?:"
                + servicePathSuffixes
                + "))?(?=[?\"'\\s]|$)",
            Pattern.CASE_INSENSITIVE);
    String result = pattern.matcher(content).replaceAll(target);
    if (!result.equals(content)) {
      log.info("Replacement of {} by {} in GetCapabilities response", source, target);
    } else {
      log.warn("No replacements of {} by {} were done in GetCapabilities response", source, target);
    }
    return result;
  }
}
