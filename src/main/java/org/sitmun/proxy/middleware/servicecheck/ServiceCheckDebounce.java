package org.sitmun.proxy.middleware.servicecheck;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory debounce for one proxy process. Restart drops it. */
public final class ServiceCheckDebounce {

  private final int countLimit;
  private final Duration window;
  private final Map<Long, Slot> slots = new HashMap<>();

  public ServiceCheckDebounce(int countLimit, Duration window) {
    this.countLimit = countLimit;
    this.window = window;
  }

  public synchronized boolean observe(long serviceId, Optional<String> status, Instant now) {
    if (status.isEmpty()) {
      return false;
    }
    String code = status.get();
    Slot slot = slots.getOrDefault(serviceId, Slot.empty());
    if (code.equals(slot.reported)) {
      slots.put(serviceId, new Slot(slot.reported, null));
      return false;
    }
    Pending pending =
        slot.pending != null && code.equals(slot.pending.status)
            ? new Pending(code, slot.pending.count + 1, slot.pending.since)
            : new Pending(code, 1, now);
    if (pending.count >= countLimit || !now.isBefore(pending.since.plus(window))) {
      slots.put(serviceId, new Slot(code, null));
      return true;
    }
    slots.put(serviceId, new Slot(slot.reported, pending));
    return false;
  }

  private record Slot(String reported, Pending pending) {
    static Slot empty() {
      return new Slot(null, null);
    }
  }

  private record Pending(String status, int count, Instant since) {}
}
