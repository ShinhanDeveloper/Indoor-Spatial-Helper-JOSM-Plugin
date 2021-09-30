package org.openstreetmap.josm.plugins.indoorSpatialHelper.data;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import javax.swing.Icon;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * 벡터 레이어의 Redo-Undo를 관리하는 클래스
 */
public class IndoorCommand extends Command {

	private final OsmPrimitive osm;

	/**
	 * {@link IndoorCommand} 생성자
	 * @param data
	 * @param osm
	 */
	public IndoorCommand(DataSet data, OsmPrimitive osm) {
		super(data);
		this.osm = Objects.requireNonNull(osm, "osm");
	}

	protected static final void checkNodeStyles(OsmPrimitive osm) {
		if (osm instanceof Way) {
			((Way) osm).clearCachedNodeStyles();
		}
	}

	@Override
	public boolean executeCommand() {
		getAffectedDataSet().addPrimitive(osm);
		//osm.setModified(true);
		checkNodeStyles(osm);
		return true;
	}

	@Override
	public void undoCommand() {
		getAffectedDataSet().removePrimitive(osm);
		checkNodeStyles(osm);
	}

	@Override
	public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		added.add(osm);
	}

	@Override
	public String getDescriptionText() {
		String msg;
		switch(OsmPrimitiveType.from(osm)) {
		case NODE: msg = marktr("Add node {0}"); break;
		case WAY: msg = marktr("Add way {0}"); break;
		case RELATION: msg = marktr("Add relation {0}"); break;
		default: /* should not happen */msg = ""; break;
		}
		return tr(msg, osm.getDisplayName(DefaultNameFormatter.getInstance()));
	}

	@Override
	public Icon getDescriptionIcon() {
		return ImageProvider.get(osm.getDisplayType());
	}

	@Override
	public Collection<OsmPrimitive> getParticipatingPrimitives() {
		return Collections.singleton(osm);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), osm);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		if (!super.equals(obj)) return false;
		IndoorCommand that = (IndoorCommand) obj;
		return Objects.equals(osm, that.osm);
	}
}
