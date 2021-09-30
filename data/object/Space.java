package org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object;

import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

public class Space extends AbstractIndoorObject {

	protected static final String[] ATTRIBUTES = {"space_id", "space_f", "floor_num", "auto_yn", 
			"indoorgml_class", "indoorgml_function", "indoorgml_usage"};

	public Space(int cnt) {
		super(Constants.SPACE_CLASS_CODE[0], Constants.SPACE_CLASS_CODE[1] + " layer", cnt);
	}

	@Override
	public String[] getObjAttributes() {
		return ATTRIBUTES;
	}
}
