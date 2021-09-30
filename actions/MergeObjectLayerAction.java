package org.openstreetmap.josm.plugins.indoorSpatialHelper.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSearchPrimitiveDialog.Action;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.LayerManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.LayerManager.ObjectLayerChangeEvent;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.LayerManager.ObjectLayerChangeListener;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.AbstractIndoorObject;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.gui.SelectObjectLayerPop;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

/**
 * 오브젝트 레이어의 병합을 처리하는 액션 클래스
 */
public class MergeObjectLayerAction extends AbstractAction implements ObjectLayerChangeListener {

	private SelectObjectLayerPop pop = null;

	/**
	 * {@link MergeObjectLayerAction} 생성자
	 */
	public MergeObjectLayerAction(){
		super(tr("병합"), Constants.MERGE_ICON_16);
		putValue(Action.LARGE_ICON_KEY, Constants.MERGE_ICON);
		putValue(Action.SHORT_DESCRIPTION, tr("Merge this layer into another indoor object layer"));
		updateEnabledState();
		LayerManager.getInstance().addObjectLayerChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		TreePath path = LayerManager.getInstance().getSelectionPath();
		if(path == null){
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Please select indoor object layer to merge."));
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

			ActionListener listener = new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
					MergeObjectLayerAction.this.merge(cls, pop.getSelectedCls());
					pop.dispose();
				}
			};

			pop = new SelectObjectLayerPop(MainApplication.getMainFrame(),
					LayerManager.getInstance().getExistsLayer(cls), listener, false);
			pop.setVisible(true);
		}else{
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Please select indoor object layer to merge."));
		}
	}

	/**
	 * 오브젝트 레이어를 병합하는 함수
	 * @param src
	 * @param trg
	 */
	private void merge(String src, String trg){
		VectorLayer layer = (VectorLayer)MainApplication.getLayerManager().getActiveLayer();
		layer.syncDataSet();

		Collection<Way> ways = layer.getRealDataSet().getWays();
		for(Way way : ways){
			if(AbstractIndoorObject.getIndoorObjectCode(way.get(Constants.INDOOR_OBJECT_KEY)).equals(src)){
				//Map<String, String> tag = way.getKeys();
				//tag.put(Constants.INDOOR_OBJECT_KEY, trg);
				//w.setKeys(tag);
				way.setKeys(AbstractIndoorObject.getTagMap(trg, way.getKeys()));
			}
		}
		layer.drawLayer();
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
