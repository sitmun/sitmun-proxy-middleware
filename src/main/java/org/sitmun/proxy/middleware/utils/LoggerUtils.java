package org.sitmun.proxy.middleware.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;

public class LoggerUtils {
  @SneakyThrows(JsonProcessingException.class)
  public static void logAsPrettyJson(Logger log, String msg, Object object) {
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    log.info(msg, json);
  }
}
