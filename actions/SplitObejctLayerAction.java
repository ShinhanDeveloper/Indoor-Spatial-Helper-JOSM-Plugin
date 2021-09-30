package org.openstreetmap.josm.plugins.indoorSpatialHelper.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSearchPrimitiveDialog.Action;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.LayerManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.AbstractIndoorObject;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.gui.SelectObjectLayerPop;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

/**
 * 오브젝트 레이어 분리를 처리하는 액션 클래스
 */
public class SplitObejctLayerAction extends AbstractAction implements DataSelectionListener {

	private SelectObjectLayerPop pop = null;

	/**
	 * {@link SplitObejctLayerAction} 생성자
	 */
	public SplitObejctLayerAction(){
		super(tr("분할"));
		putValue(Action.LARGE_ICON_KEY, Constants.SPLIT_ICON);
		putValue(Action.SHORT_DESCRIPTION, tr("선택한 오브젝트레이어 분할"));
		updateEnabledState();
		SelectionEventManager.getInstance().addSelectionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Layer layer = MainApplication.getLayerManager().getActiveLayer();
		if(layer instanceof VectorLayer && ((VectorLayer) layer).getDataSet().getSelectedWays().size() > 0){

			if(LayerManager.getInstance().getLayerCount() >= Constants.ALL_INDOOR_OBJECTS.length){
//				JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("There must be at least one indoor object layer that has not been created."));
				JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("적어도 한개의 오브젝트가 존재 해야 합니다."));
				return;
			}

			ActionListener listener = new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
					SplitObejctLayerAction.this.split(pop.getSelectedCls());
					pop.dispose();
				}
			};

			pop = new SelectObjectLayerPop(MainApplication.getMainFrame(),
					LayerManager.getInstance().getNotExistsLayer(), listener, true);
			pop.setVisible(true);
		}else{
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("백터레이어에서 실내공간 객체를 선택해 주세요."));
		}
	}

	/**
	 * 오브젝트 레이어를 분리하는 함수
	 * @param cls
	 */
	private void split(String cls){
		VectorLayer layer = (VectorLayer)MainApplication.getLayerManager().getActiveLayer();
		layer.syncDataSet();

		DataSet ds = layer.getRealDataSet();
		Collection<Way> ways = layer.getDataSet().getSelectedWays();
		for(Way way : ways){
			//Map<String, String> tag = way.getKeys();
			//tag.put(Constants.INDOOR_OBJECT_KEY, cls);
			OsmPrimitive primitive = ds.getPrimitiveById(way);
			//primitive.setKeys(tag);
			primitive.setKeys(AbstractIndoorObject.getTagMap(cls, way.getKeys()));
		}
		layer.drawLayer();
	}

	/**
	 * 컴포넌트의 실행 가능 여부를 처리하는 함수
	 */
	public void updateEnabledState(){
		Layer layer = MainApplication.getLayerManager().getActiveLayer();
		if (layer instanceof VectorLayer && ((VectorLayer) layer).getDataSet().getSelectedWays().size() > 0
				&& (LayerManager.getInstance().getLayerCount() < Constants.ALL_INDOOR_OBJECTS.length)) {
			setEnabled(true);
		} else {
			setEnabled(false);
		}
	}

	@Override
	public void selectionChanged(SelectionChangeEvent event){
		updateEnabledState();
	}
}
