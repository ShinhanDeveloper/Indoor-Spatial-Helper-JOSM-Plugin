package org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object;

import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

public class Lift extends AbstractIndoorObject {

	protected static final String[] ATTRIBUTES = {"lift_id", "auto_yn", 
			"indoorgml_class", "indoorgml_function", "indoorgml_usage"};

	public Lift(int cnt) {
		super(Constants.LIFT_CLASS_CODE[0], Constants.LIFT_CLASS_CODE[1] + " layer", cnt);
	}

	@Override
	public String[] getObjAttributes() {
		return ATTRIBUTES;
	}
}
