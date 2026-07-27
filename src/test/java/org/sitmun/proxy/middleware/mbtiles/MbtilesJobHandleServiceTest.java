package org.sitmun.proxy.middleware.mbtiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MBTiles opaque job handle")
class MbtilesJobHandleServiceTest {

  private Clock clock;
  private MbtilesJobHandleService service;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    service =
        new MbtilesJobHandleService(
            new MbtilesProperties(
                "http://mbtiles.test/mbtiles",
                "unit-test-mbtiles-job-handle-secret-key",
                Duration.ofHours(1),
                65536,
                12,
                List.of("EPSG:4326"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5)),
            clock);
  }

  @Test
  @DisplayName("Minted handle verifies for matching principal/app/ter")
  void mintAndVerify() {
    String handle = service.mint("alice", 1, 2, 42L);
    assertThat(handle).contains(".").doesNotContain("42");
    assertThat(service.verify(handle, "alice", 1, 2))
        .isPresent()
        .get()
        .satisfies(
            claims -> {
              assertThat(claims.jobId()).isEqualTo(42L);
              assertThat(claims.principal()).isEqualTo("alice");
              assertThat(claims.exp()).isEqualTo(clock.instant().getEpochSecond() + 3600);
            });
  }

  @Test
  @DisplayName("Rejects tampered signature, wrong principal, wrong app/ter, and expired handles")
  void rejectsInvalidHandles() {
    String handle = service.mint("alice", 1, 2, 42L);
    assertThat(service.verify(handle + "x", "alice", 1, 2)).isEmpty();
    assertThat(service.verify(handle, "bob", 1, 2)).isEmpty();
    assertThat(service.verify(handle, "alice", 9, 2)).isEmpty();
    assertThat(service.verify(handle, "alice", 1, 9)).isEmpty();

    Clock expiredClock = Clock.fixed(Instant.parse("2026-01-01T02:00:00Z"), ZoneOffset.UTC);
    MbtilesJobHandleService expiredService =
        new MbtilesJobHandleService(
            new MbtilesProperties(
                "http://mbtiles.test/mbtiles",
                "unit-test-mbtiles-job-handle-secret-key",
                Duration.ofHours(1),
                65536,
                12,
                List.of("EPSG:4326"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5)),
            expiredClock);
    assertThat(expiredService.verify(handle, "alice", 1, 2)).isEmpty();
  }

  @Test
  @DisplayName("Secret rotation invalidates previously minted handles")
  void secretRotationInvalidatesHandles() {
    String handle = service.mint("alice", 1, 2, 42L);
    MbtilesJobHandleService rotated =
        new MbtilesJobHandleService(
            new MbtilesProperties(
                "http://mbtiles.test/mbtiles",
                "rotated-mbtiles-job-handle-secret-key-xx",
                Duration.ofHours(1),
                65536,
                12,
                List.of("EPSG:4326"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5)),
            clock);
    assertThat(rotated.verify(handle, "alice", 1, 2)).isEmpty();
  }

  @Test
  @DisplayName("Blank principal is rejected at mint time")
  void rejectsBlankPrincipal() {
    assertThatThrownBy(() -> service.mint(" ", 1, 2, 1L))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
