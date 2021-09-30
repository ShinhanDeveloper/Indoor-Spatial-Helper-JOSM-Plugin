package org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object;

import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

public class Etc extends AbstractIndoorObject {

	protected static final String[] ATTRIBUTES = {};

	public Etc(int cnt) {
		super(Constants.ETC_CLASS_CODE[0], Constants.ETC_CLASS_CODE[1] + " layer", cnt);
	}

	@Override
	public String[] getObjAttributes() {
		return ATTRIBUTES;
	}
}
