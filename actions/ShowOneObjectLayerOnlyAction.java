package org.openstreetmap.josm.plugins.indoorSpatialHelper.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.LayerManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.AbstractIndoorObject;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

/**
 * 선택한 오브젝트 레이어만 가시화 처리하는 액션 클래스
 */
public class ShowOneObjectLayerOnlyAction extends AbstractAction {

	/**
	 * {@link ShowOneObjectLayerOnlyAction} 생성자
	 */
	public ShowOneObjectLayerOnlyAction(){
		super(tr("선택한 레이어만 보기"), Constants.SHOWONLY_ICON);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		TreePath path = LayerManager.getInstance().getSelectionPath();
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();

		if(node.getUserObject() instanceof AbstractIndoorObject){
			AbstractIndoorObject obj = (AbstractIndoorObject)node.getUserObject();
			VectorLayer layer = LayerManager.getInstance().getLayer();
			TreeModel model = LayerManager.getInstance().getModel();
			obj.setVisible(true);

			for(int i=0; i<model.getChildCount(model.getRoot()); i++){
				DefaultMutableTreeNode n = (DefaultMutableTreeNode)model.getChild(model.getRoot(), i);
				if(n.getUserObject() instanceof AbstractIndoorObject && !n.equals(node)){
					((AbstractIndoorObject)n.getUserObject()).setVisible(false);
				}
			}

			layer.setVisibleOneLayer(obj.getCls());
		}
	}
}
