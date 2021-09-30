package org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object;

import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

public class Stair extends AbstractIndoorObject {

	protected static final String[] ATTRIBUTES = {"stair_id", "auto_yn", 
			"indoorgml_class", "indoorgml_function", "indoorgml_usage"};

	public Stair(int cnt) {
		super(Constants.STAIR_CLASS_CODE[0], Constants.STAIR_CLASS_CODE[1] + " layer", cnt);
	}

	@Override
	public String[] getObjAttributes() {
		return ATTRIBUTES;
	}
}
