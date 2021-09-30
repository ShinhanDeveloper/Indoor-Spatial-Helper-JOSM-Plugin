package org.openstreetmap.josm.plugins.indoorSpatialHelper.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSearchPrimitiveDialog.Action;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.gui.widgets.FileChooserManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.modules.ExportIndoorGMLTask2;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.GuiUtil;

/**
 * IndoorGML 내보내기를 처리하는 액션 클래스
 */
public class ExportIndoorGmlAction extends AbstractAction {

	private VectorLayer layer;

	/**
	 * {@link ExportIndoorGmlAction} 생성자
	 * @param layer
	 */
	public ExportIndoorGmlAction(VectorLayer layer){
		super(tr("IndoorGML 추출"), Constants.GML_ICON);
		putValue(Action.SHORT_DESCRIPTION, tr("Export current data to IndoorGML file"));
		this.layer = layer;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		((GridBagLayout) panel.getLayout()).columnWidths = new int[] {0, 0, 0};
		((GridBagLayout) panel.getLayout()).rowHeights = new int[] {0, 0, 0};
		((GridBagLayout) panel.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
		((GridBagLayout) panel.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};

		JRadioButton radio1 = new JRadioButton(tr("모든 층 내보내기"));
		radio1.setActionCommand("0");
		radio1.setSelected(true);
		JRadioButton radio2 = new JRadioButton(tr("선택한 층만 내보내기"));
		radio2.setActionCommand("1");
		JCheckBox check = new JCheckBox(tr("For Inviewer-Desktop"));
		check.setSelected(true);

		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(radio1);
		buttonGroup.add(radio2);

		panel.add(radio1, GuiUtil.getGridBagConst(0, 0, 1, 1));
		panel.add(radio2, GuiUtil.getGridBagConst(1, 0, 1, 1));
		if(!Constants.IS_CERTIFICATE_VERSION){
			panel.add(check, GuiUtil.getGridBagConst(0, 1, 2, 1));
		}

		int result = JOptionPane.showOptionDialog(MainApplication.getMainFrame(),
				tr("내보내기 옵션을 선택해 주세요."),
				tr("IndoorGML 내보내기"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				new Object[]{panel, tr("확인")},
				null);
		if(result != 1){
			return;
		}
		
		FileChooserManager fcm = new FileChooserManager(false);
		fcm.createFileChooser(false, null, Arrays.asList(Constants.GML_FILE_FILTER), Constants.GML_FILE_FILTER, JFileChooser.FILES_ONLY);
		AbstractFileChooser fc = fcm.openFileChooser();
		if(fc != null) {
			try {
				if(radio1.getActionCommand().equals(buttonGroup.getSelection().getActionCommand())){

					List<VectorLayer> layers = new ArrayList<>();
					for(Layer l : MainApplication.getLayerManager().getLayers()){
						if(l instanceof VectorLayer){
							if(layer.getOsmPrimitiveId() == ((VectorLayer)l).getOsmPrimitiveId()){
								((VectorLayer)l).syncDataSet();
								layers.add((VectorLayer)l);
							}
						}
					}
					MainApplication.worker.submit(new ExportIndoorGMLTask2(fc.getSelectedFile(), layers.toArray(new VectorLayer[0]), check.isSelected()));
				}else if(radio2.getActionCommand().equals(buttonGroup.getSelection().getActionCommand())){
					layer.syncDataSet();
					MainApplication.worker.submit(new ExportIndoorGMLTask2(fc.getSelectedFile(), layer, check.isSelected()));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
