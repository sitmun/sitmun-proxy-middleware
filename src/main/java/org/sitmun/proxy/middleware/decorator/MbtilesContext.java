package org.sitmun.proxy.middleware.decorator;

import org.sitmun.proxy.middleware.dto.TileServiceDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MbtilesContext implements Context {
    
    private TileServiceDto service;
    private String outputPath;

    @Override
    public String describe() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'describe'");
    }
}
