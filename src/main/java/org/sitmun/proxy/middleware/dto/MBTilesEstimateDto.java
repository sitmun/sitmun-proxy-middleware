package org.sitmun.proxy.middleware.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MBTilesEstimateDto {
    private int tileCount;
    private double estimatedTileSizeKb;
    private double estimatedMbtilesSizeMb;
}
