package org.openstreetmap.josm.plugins.indoorSpatialHelper.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.CommonUtil;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

/**
 * 선택 레이어 위치로 줌 및 이동을 처리하는 액션 클래스
 */
public class ZoomToLayerAction extends AbstractAction {

	private VectorLayer layer;

	/**
	 * {@link ZoomToLayerAction} 생성자
	 * @param layer
	 */
	public ZoomToLayerAction(VectorLayer layer){
		super(tr("레이어로 확대"), Constants.ZOOM_ICON);
		putValue(Action.SHORT_DESCRIPTION, tr("레이어로 확대"));
		this.layer = layer;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Collection<Node> nodes =  layer.getRealDataSet().getNodes();
		Bounds box = CommonUtil.getBoundary((Collection)nodes);
		if(null != box){
			MainApplication.getMap().mapView.zoomTo(box);
		}
	}
}
