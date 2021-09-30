package org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object;

import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

public class Window extends AbstractIndoorObject {

	protected static final String[] ATTRIBUTES = {"window_id", "wall_id", "window_type", 
			"window_start_point", "window_end_point", "auto_yn", 
			"indoorgml_class", "indoorgml_function", "indoorgml_usage"};

	public Window(int cnt) {
		super(Constants.WINDOW_CLASS_CODE[0], Constants.WINDOW_CLASS_CODE[1] + " layer", cnt);
	}

	@Override
	public String[] getObjAttributes() {
		return ATTRIBUTES;
	}
}
