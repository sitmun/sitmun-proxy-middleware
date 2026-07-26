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
      var parameters = wmsPayloadDto.getParameters();
      if (parameters == null) {
        return false;
      }
      return parameters.getOrDefault("REQUEST", "").equalsIgnoreCase("GetCapabilities");
    }
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void addBehavior(Object response, Context context) {
    if (response instanceof RequestExecutorResponseImpl<?> result
        && result.getStatusCode() >= 200
        && result.getStatusCode() < 300
        && result.getBody() instanceof byte[]
        && context instanceof WmsPayloadDto wmsPayloadDto) {
      RequestExecutorResponseImpl<byte[]> requestExecutionResponseImpl1 =
          (RequestExecutorResponseImpl<byte[]>) response;
      String s = new String(requestExecutionResponseImpl1.getBody(), StandardCharsets.UTF_8);
      String fullUri = wmsPayloadDto.getUri();
      String baseUri = fullUri.split("\\?")[0];

      String baseUrl = requestExecutionResponseImpl1.getBaseUrl();
      List<String> servicePaths = properties.getServicePaths();
      String servicePathSuffixes = String.join("|", servicePaths);
      log.debug(
          "Decorating WMS capabilities response: servicePathCount={} extraSourceCount={}",
          servicePaths.size(),
          properties.getExtraSources().size());

      String servicePath = baseUri.replaceAll("/(?:" + servicePathSuffixes + ")/?$", "");
      s = replace(s, servicePath, servicePathSuffixes, baseUrl, "primary");

      for (String extraSource : properties.getExtraSources()) {
        String normalizedSource = extraSource.replaceAll("/(?:" + servicePathSuffixes + ")/?$", "");
        s = replace(s, normalizedSource, servicePathSuffixes, baseUrl, "extra");
      }

      requestExecutionResponseImpl1.setBody(s.getBytes(StandardCharsets.UTF_8));
    }
  }

  /** Replaces all quoted occurrences of {@code source} */
  private String replace(
      String content, String source, String servicePathSuffixes, String target, String sourceKind) {
    Pattern pattern =
        Pattern.compile(
            "(?<=[\"'])"
                + Pattern.quote(source)
                + "(?:/(?:"
                + servicePathSuffixes
                + "))?(?=[?\"'\\s]|$)",
            Pattern.CASE_INSENSITIVE);
    long replacementCount = pattern.matcher(content).results().count();
    log.debug(
        "WMS capabilities URL replacement: sourceKind={} replacementCount={}",
        sourceKind,
        replacementCount);
    return pattern.matcher(content).replaceAll(target);
  }
}
