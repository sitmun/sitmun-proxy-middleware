package org.sitmun.proxy.middleware.servicecheck;

import org.sitmun.proxy.contract.ServiceCheckReport;

public interface ServiceCheckPoster {
  void post(ServiceCheckReport report);
}
