package org.sitmun.proxy.middleware.request;

import java.util.List;

import org.sitmun.proxy.middleware.dto.MapServiceDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TileRequestDto {
    private List<MapServiceDto> mapServices;
    private double minLat;
    private double minLon;
    private double maxLat;
    private double maxLon;
    private int minZoom;
    private int maxZoom;
    private String srs;
}
