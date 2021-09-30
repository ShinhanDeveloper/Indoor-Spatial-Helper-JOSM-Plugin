package org.openstreetmap.josm.plugins.indoorSpatialHelper.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSearchPrimitiveDialog.Action;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.LayerManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.LayerManager.ObjectLayerChangeEvent;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.LayerManager.ObjectLayerChangeListener;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.AbstractIndoorObject;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

/**
 * 오브젝트 레이어 삭제를 처리하는 액션 클래스
 */
public class DeleteObjectLayerAction extends AbstractAction implements ObjectLayerChangeListener {

	/**
	 * {@link DeleteObjectLayerAction} 생성자
	 */
	public DeleteObjectLayerAction(){
		super(tr("삭제"), Constants.DELETE_ICON_16);
		putValue(Action.LARGE_ICON_KEY, Constants.DELETE_ICON);
		putValue(Action.SHORT_DESCRIPTION, tr("Delete the selected indoor object layer"));
		updateEnabledState();
		LayerManager.getInstance().addObjectLayerChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		TreePath path = LayerManager.getInstance().getSelectionPath();
		if (path == null){
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Please select indoor object layer to delete."));
			return;
		}
		if(LayerManager.getInstance().getLayerCount() < 2){
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("There must be at least two indoor object layers."));
			return;
		}
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
		if(node.getUserObject() instanceof AbstractIndoorObject){
			AbstractIndoorObject obj = (AbstractIndoorObject)node.getUserObject();
			String cls = obj.getCls();

			VectorLayer layer = LayerManager.getInstance().getLayer();
			layer.syncDataSet();
			DataSet ds = LayerManager.getInstance().getLayer().getRealDataSet();
			for(Way w : ds.getWays()){
				String c = AbstractIndoorObject.getIndoorObjectCode(w.get(Constants.INDOOR_OBJECT_KEY));
				if(cls.equals(c)){
					for(Node n : w.getNodes()) {
						OsmPrimitive primitive = ds.getPrimitiveById(n);
						if(null != primitive && !((Node)primitive).isReferredByWays(2)) {
							ds.removePrimitive(primitive);
						}
					}
					OsmPrimitive way = ds.getPrimitiveById(w);
					if(null != way){
						ds.removePrimitive(way);
					}
				}
			}
			layer.drawLayer();
		}else{
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Please select indoor object layer to delete."));
		}
	}

	/**
	 * 컴포넌트의 실행 가능 여부를 처리하는 함수
	 */
	public void updateEnabledState(){
		if(LayerManager.getInstance().getLayerCount() < 2){
			setEnabled(false);
		}else{
			setEnabled(true);
		}
	}

	@Override
	public void objectLayerChangeEvent(ObjectLayerChangeEvent e) {
		updateEnabledState();
	}
}
