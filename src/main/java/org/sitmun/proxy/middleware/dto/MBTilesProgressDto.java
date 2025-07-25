package org.sitmun.proxy.middleware.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MBTilesProgressDto {
    
    private long totalTiles;
    private long processedTiles;
}
