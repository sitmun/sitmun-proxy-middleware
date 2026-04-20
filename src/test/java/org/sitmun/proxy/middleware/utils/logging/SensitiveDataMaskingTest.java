package org.sitmun.proxy.middleware.utils.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Per-header masking matrix lives in backend-core to avoid duplicating the same header names in two
 * modules. This class only covers multi-entry map behavior for the proxy helper.
 */
@DisplayName("SensitiveDataMasking (proxy)")
class SensitiveDataMaskingTest {

  @Test
  @DisplayName("maskSortedMap sorts case-insensitively and masks only sensitive keys")
  void maskSortedMapSortsAndMasksSubset() {
    Map<String, String> in = new LinkedHashMap<>();
    in.put("zebra", "z");
    in.put("Alpha", "a");
    in.put("Custom-Api-Key-Client", "hunter2");
    Map<String, String> out = SensitiveDataMasking.maskSortedMap(in);
    assertThat(out.keySet()).containsExactly("Alpha", "Custom-Api-Key-Client", "zebra");
    assertThat(out.get("Alpha")).isEqualTo("a");
    assertThat(out.get("zebra")).isEqualTo("z");
    assertThat(out.get("Custom-Api-Key-Client")).isEqualTo(SensitiveDataMasking.REDACTED);
  }
}
