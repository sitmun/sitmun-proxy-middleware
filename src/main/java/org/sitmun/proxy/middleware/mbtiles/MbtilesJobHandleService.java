package org.sitmun.proxy.middleware.mbtiles;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Opaque integrity-protected MBTiles job handles.
 *
 * <p>Format: {@code base64url(payload).base64url(hmac-sha256)}. Payload is {@code
 * principal|appId|terId|jobId|expEpochSeconds}. Rotating {@code sitmun.mbtiles.job-handle-secret}
 * without multi-key support invalidates active jobs.
 */
@Service
public class MbtilesJobHandleService {

  private static final String HMAC_ALG = "HmacSHA256";
  private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder B64_DEC = Base64.getUrlDecoder();

  private final byte[] secret;
  private final Clock clock;
  private final long ttlSeconds;

  public MbtilesJobHandleService(MbtilesProperties properties, Clock clock) {
    this.secret = properties.jobHandleSecret().getBytes(StandardCharsets.UTF_8);
    this.clock = clock;
    this.ttlSeconds = Math.max(1, properties.jobHandleTtl().toSeconds());
  }

  public String mint(String principal, int appId, int terId, long jobId) {
    if (!StringUtils.hasText(principal)) {
      throw new IllegalArgumentException("principal is required");
    }
    long exp = clock.instant().getEpochSecond() + ttlSeconds;
    String payload = encodePayload(principal, appId, terId, jobId, exp);
    return B64.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + sign(payload);
  }

  public Optional<JobHandleClaims> verify(String handle, String principal, int appId, int terId) {
    if (!StringUtils.hasText(handle) || !StringUtils.hasText(principal)) {
      return Optional.empty();
    }
    int dot = handle.indexOf('.');
    if (dot <= 0 || dot == handle.length() - 1) {
      return Optional.empty();
    }
    String payloadB64 = handle.substring(0, dot);
    String signature = handle.substring(dot + 1);
    String payload;
    try {
      payload = new String(B64_DEC.decode(payloadB64), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
    if (!MessageDigest.isEqual(
        sign(payload).getBytes(StandardCharsets.UTF_8),
        signature.getBytes(StandardCharsets.UTF_8))) {
      return Optional.empty();
    }
    String[] parts = payload.split("\\|", -1);
    if (parts.length != 5) {
      return Optional.empty();
    }
    try {
      String claimPrincipal = parts[0];
      int claimAppId = Integer.parseInt(parts[1]);
      int claimTerId = Integer.parseInt(parts[2]);
      long jobId = Long.parseLong(parts[3]);
      long exp = Long.parseLong(parts[4]);
      if (!claimPrincipal.equals(principal)
          || claimAppId != appId
          || claimTerId != terId
          || clock.instant().getEpochSecond() > exp
          || jobId <= 0) {
        return Optional.empty();
      }
      return Optional.of(new JobHandleClaims(claimPrincipal, claimAppId, claimTerId, jobId, exp));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }

  private static String encodePayload(
      String principal, int appId, int terId, long jobId, long exp) {
    return principal + "|" + appId + "|" + terId + "|" + jobId + "|" + exp;
  }

  private String sign(String payload) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALG);
      mac.init(new SecretKeySpec(secret, HMAC_ALG));
      return B64.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("HMAC unavailable", e);
    }
  }

  public record JobHandleClaims(String principal, int appId, int terId, long jobId, long exp) {}
}
