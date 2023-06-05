package org.sitmun.proxy.middleware.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

public class LoggerUtils {
  public static void logAsPrettyJson(Logger log, String msg, Object object) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
      log.info(msg, json);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
