package org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object;

import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

public class Door extends AbstractIndoorObject {

	protected static final String[] ATTRIBUTES = {"door_id", "wall_id", "door_type", 
			"door_start_point", "door_end_point", "auto_yn", 
			"indoorgml_class", "indoorgml_function", "indoorgml_usage"};

	public Door(int cnt) {
		super(Constants.DOOR_CLASS_CODE[0], Constants.DOOR_CLASS_CODE[1] + " layer", cnt);
	}

	@Override
	public String[] getObjAttributes() {
		return ATTRIBUTES;
	}
}
