package org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object;

import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

public class Wall extends AbstractIndoorObject {

	protected static final String[] ATTRIBUTES = {"wall_id", "floor_num", "auto_yn", 
			"indoorgml_class", "indoorgml_function", "indoorgml_usage"};

	public Wall(int cnt) {
		super(Constants.WALL_CLASS_CODE[0], Constants.WALL_CLASS_CODE[1] + " layers", cnt);
	}

	@Override
	public String[] getObjAttributes() {
		return ATTRIBUTES;
	}
}
