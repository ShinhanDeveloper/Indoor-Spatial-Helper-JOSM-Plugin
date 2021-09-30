package org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object;

import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

public class Subspace extends AbstractIndoorObject {

	protected static final String[] ATTRIBUTES = {"subspace_id", "subspace_f", "space_id", "floor_num", "auto_yn", 
			"indoorgml_class", "indoorgml_function", "indoorgml_usage"};

	public Subspace(int cnt) {
		super(Constants.SUBSPACE_CLASS_CODE[0], Constants.SUBSPACE_CLASS_CODE[1] + " layer", cnt);
	}

	@Override
	public String[] getObjAttributes() {
		return ATTRIBUTES;
	}
}
