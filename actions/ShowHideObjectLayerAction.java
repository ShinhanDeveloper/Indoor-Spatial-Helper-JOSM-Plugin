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

/**
 * 오브젝트 레이어 on/off를 처리하는 액션 클래스
 */
public class ShowHideObjectLayerAction extends AbstractAction {

	/**
	 * {@link ShowHideObjectLayerAction} 생성자
	 */
	public ShowHideObjectLayerAction(){
		super(tr("보기/숨기기"), Constants.SHOWHIDE_ICON);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		TreePath path = LayerManager.getInstance().getSelectionPath();
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
		if(node.getUserObject() instanceof AbstractIndoorObject){
			AbstractIndoorObject obj = (AbstractIndoorObject)node.getUserObject();
			VectorLayer layer = LayerManager.getInstance().getLayer();
			boolean visible = obj.setVisible();

			layer.setVisibleLayer(obj.getCls(), visible);
		}
	}
}
