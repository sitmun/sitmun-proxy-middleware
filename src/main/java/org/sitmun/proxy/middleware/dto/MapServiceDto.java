package org.sitmun.proxy.middleware.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MapServiceDto {

    private String url;

    private List<String> layers;

    private String type;

}
