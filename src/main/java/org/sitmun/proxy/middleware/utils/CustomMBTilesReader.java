package org.sitmun.proxy.middleware.utils;

import java.io.File;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ch.poole.geo.mbtiles4j.MBTilesReadException;
import ch.poole.geo.mbtiles4j.MBTilesReader;
import ch.poole.geo.mbtiles4j.Tile;

public class CustomMBTilesReader extends MBTilesReader {

    public CustomMBTilesReader(File f) throws MBTilesReadException {
        super(f);
    }

    @Override
    public Tile getTile(int zoom, int column, int row) throws MBTilesReadException {
        String sql = String.format("SELECT tile_data FROM tiles WHERE zoom_level = %d AND tile_column = %d AND tile_row = %d", zoom, column, row);
    	
    	try {
            Statement stm = getConnection().createStatement();
            ResultSet resultSet = stm.executeQuery(sql);
			InputStream tileDataInputStream = null;
			tileDataInputStream = resultSet.getBinaryStream("tile_data");
            resultSet.close();
            stm.close();
            if (tileDataInputStream == null) {
                return null; // No tile found
            }
            return new Tile(zoom, column, row, tileDataInputStream);
		} catch (SQLException e) {
			throw new MBTilesReadException(String.format("Could not get Tile for z:%d, column:%d, row:%d", zoom, column, row), e);
		}
    }
    
}
