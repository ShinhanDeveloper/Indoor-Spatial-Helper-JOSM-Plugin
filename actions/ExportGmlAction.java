package org.openstreetmap.josm.plugins.indoorSpatialHelper.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSearchPrimitiveDialog.Action;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.gui.widgets.FileChooserManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.modules.ExportGMLTask;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

/**
 * GML 파일 내보내기를 처리하는 액션 클래스
 */
public class ExportGmlAction extends AbstractAction {

	private VectorLayer layer;

	/**
	 * {@link ExportGmlAction} 생성자
	 * @param layer
	 */
	public ExportGmlAction(VectorLayer layer){
		super(tr("GML 추출"), Constants.GML_ICON);
		putValue(Action.SHORT_DESCRIPTION, tr("Export current data to GML file"));
		this.layer = layer;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		FileChooserManager fcm = new FileChooserManager(false);
		fcm.createFileChooser(false, null, Arrays.asList(Constants.GML_FILE_FILTER), Constants.GML_FILE_FILTER, JFileChooser.FILES_ONLY);
		AbstractFileChooser fc = fcm.openFileChooser();
		if(fc != null) {
			try {
				layer.syncDataSet();
				MainApplication.worker.submit(new ExportGMLTask(fc.getSelectedFile(), layer));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
