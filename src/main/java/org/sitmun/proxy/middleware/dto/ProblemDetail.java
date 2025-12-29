package org.sitmun.proxy.middleware.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * RFC 9457 Problem Details for HTTP APIs (Proxy Middleware).
 *
 * <p>This is a copy of the main ProblemDetail class for use in the proxy middleware module.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9457.html">RFC 9457</a>
 */
@Builder
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetail {

  /** A URI reference that identifies the problem type. */
  private String type;

  /** The HTTP status code for this occurrence of the problem. */
  private Integer status;

  /** A short, human-readable summary of the problem type. */
  private String title;

  /** A human-readable explanation specific to this occurrence. */
  private String detail;

  /** A URI reference identifying the specific occurrence. */
  private String instance;

  /** Extension members for additional context. */
  @Builder.Default private Map<String, Object> properties = new HashMap<>();

  /**
   * Add an extension property.
   *
   * @param key property name
   * @param value property value
   * @return this instance for chaining
   */
  public ProblemDetail addProperty(String key, Object value) {
    this.properties.put(key, value);
    return this;
  }
}

