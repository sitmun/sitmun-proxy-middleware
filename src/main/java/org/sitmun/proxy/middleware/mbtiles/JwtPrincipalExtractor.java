package org.sitmun.proxy.middleware.mbtiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Decodes JWT payload claims without verifying the signature (backend verifies). */
@Component
public class JwtPrincipalExtractor {

  private final ObjectMapper objectMapper;

  public JwtPrincipalExtractor(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Optional<String> extractSub(String token) {
    if (!StringUtils.hasText(token)) {
      return Optional.empty();
    }
    String[] parts = token.split("\\.");
    if (parts.length < 2) {
      return Optional.empty();
    }
    try {
      byte[] decoded = Base64.getUrlDecoder().decode(pad(parts[1]));
      JsonNode payload = objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
      String sub = payload.path("sub").asText(null);
      return StringUtils.hasText(sub) ? Optional.of(sub) : Optional.empty();
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  private static String pad(String value) {
    int mod = value.length() % 4;
    if (mod == 0) {
      return value;
    }
    return value + "====".substring(mod);
  }
}
