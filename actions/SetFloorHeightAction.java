package org.openstreetmap.josm.plugins.indoorSpatialHelper.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSearchPrimitiveDialog.Action;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.CommonUtil;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.JTextFieldLimit;

/**
 * 층 및 오브젝트의 높이 값을 설정하는 액션 클래스
 */
public class SetFloorHeightAction extends AbstractAction {

	private VectorLayer layer;
	private String cls;

	/**
	 * {@link SetFloorHeightAction} 생성자
	 * @param layer
	 */
	public SetFloorHeightAction(VectorLayer layer){
		super(tr("층고 높이 설정"), Constants.FLOOR_HEIGHT_ICON);
		putValue(Action.SHORT_DESCRIPTION, tr("층고 높이 설정"));
		this.layer = layer;
		this.cls = null;
	}

	/**
	 * {@link SetFloorHeightAction} 생성자
	 * @param layer
	 * @param cls
	 */
	public SetFloorHeightAction(VectorLayer layer, String cls){
		super(tr("층고 높이 설정"), Constants.FLOOR_HEIGHT_ICON);
		putValue(Action.SHORT_DESCRIPTION, tr("층고 높이 설정"));
		this.layer = layer;
		this.cls = cls;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String target = null == cls ? "Floor" : "Object";

		JLabel label = new JLabel(tr("높이(mm) : ", target));
		label.setPreferredSize(new Dimension(120, 24));

		JTextField tf = new JTextField();
		tf.setDocument(new JTextFieldLimit(100));
		tf.setPreferredSize(new Dimension(120, 24));
		tf.setDocument(new JTextFieldLimit(10));

		boolean isContinue = true;
		while(isContinue){
			Object[] options = {label, tf, tr("확인")};
			int result = JOptionPane.showOptionDialog(MainApplication.getMainFrame(),
					tr("원하는 높이 값을 mm단위로 입력하세요.", target.toLowerCase()),
					tr("높이 설정", target.toLowerCase()),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[2]);
			UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
			if(result == 2){
				if(isContinue = !CommonUtil.isPositiveNumber(tf.getText())){
//					String msg = tr("Please check the {0} height format.\nIt needs to positive real number.", target.toLowerCase());
					String msg = tr("높이 값 {0}을 확인해 주세요.\n양수 값으로 설정해 주세요.", target.toLowerCase());
					JOptionPane.showMessageDialog(MainApplication.getMainFrame(), msg);
				}else if(isContinue = !(Double.parseDouble(tf.getText()) > 0 )){
					String msg = tr("높이 값 {0}을 확인해 주세요.\n0보다 큰값으로 설정해 주세요.", target.toLowerCase());
					JOptionPane.showMessageDialog(MainApplication.getMainFrame(), msg);
				}
			}else{
				return;
			}
		}
		setFloorHeight(tf.getText());
	}

	/**
	 * 오브젝트의 높이값을 설정하는 함수
	 * @param height
	 */
	public void setFloorHeight(String height){
		layer.syncDataSet();

		Collection<Way> ways = layer.getRealDataSet().getWays();
		for(Way w : ways){
			Map<String, String> tag = w.getKeys();
			if(null != cls && !tag.get(Constants.INDOOR_OBJECT_KEY).equals(cls)){
				continue;
			}
			tag.put("height", height);
			w.setKeys(tag);
		}
		layer.drawLayer();
	}
}
