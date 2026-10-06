package org.sitmun.proxy.middleware.usage;

import org.sitmun.proxy.contract.ServiceUsageReport;

public interface ServiceUsagePoster {

  void post(ServiceUsageReport report);
}
