package org.sitmun.proxy.middleware.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sitmun.proxy.middleware.decorator.Context;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PayloadDto implements Context {

  private List<String> vary;

  @Override
  public String describe() {
    return "PayloadDto{" +
      "vary=" + vary +
      "}";
  }
}
