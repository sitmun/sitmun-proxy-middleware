package org.sitmun.proxy.middleware.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TileCoordinate {
    private final int x;
    private final int y;
    private final String z;
}
