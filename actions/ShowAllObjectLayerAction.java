package org.openstreetmap.josm.plugins.indoorSpatialHelper.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.LayerManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.AbstractIndoorObject;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

public class ShowAllObjectLayerAction extends AbstractAction {

	/**
	 * {@link ShowAllObjectLayerAction} 생성자
	 */
	public ShowAllObjectLayerAction(){
		super(tr("모두 보기"), Constants.SHOWONLY_ICON);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int count = LayerManager.getInstance().getLayerCount();
		
		for(int i=0; i<count; i++) {
			TreePath path = LayerManager.getInstance().getPathForRow(i);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			if(node.getUserObject() instanceof AbstractIndoorObject){
				AbstractIndoorObject obj = (AbstractIndoorObject)node.getUserObject();
				VectorLayer layer = LayerManager.getInstance().getLayer();
				layer.setVisibleLayer(obj.getCls(), true);
			}
		}
	}
}