package org.openstreetmap.josm.plugins.indoorSpatialHelper.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSearchPrimitiveDialog.Action;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.IndoorSpatialHelperController;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.BuildingInfo;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.CommonUtil;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

/**
 * 도면 이미지 정보 팝업 조회를 처리하는 액션 클래스
 */
public class ShowFloorPlanAction extends AbstractAction {

	private static List<ShowFloorPlanAction> actionList = new ArrayList<>();

	private VectorLayer layer;
	private JFrame frame;
	private BufferedImage image;

	/**
	 * {@link ShowFloorPlanAction} 생성자
	 * @param layer
	 */
	public ShowFloorPlanAction(VectorLayer layer) {
		super(tr("평면도 보기"), Constants.IMAGE_LAYER_ICON);
		putValue(Action.SHORT_DESCRIPTION, tr("선택한 레이어의 평면도 보기"));
		this.layer = layer;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JFrame f = getFrameFromList(layer.getOsmPrimitiveId(), layer.getFloor());
		if(null == f){
			BuildingInfo buildingInfo = IndoorSpatialHelperController.getBuildingManager().getBuildingInfo(layer.getOsmPrimitiveId());
			image = buildingInfo.getFloorPlanImage(layer.getFloor());
			ShowFloorPlan(buildingInfo.getNm(), layer.getFloor());
			actionList.add(this);
		}else{
			f.toFront();
		}
	}

	/**
	 * 도면 이미지 정보 팝업을 조회하는 함수
	 * @param buildingNm
	 * @param floor
	 */
	private void ShowFloorPlan(String buildingNm, int floor) {

		if(null == image){
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("평면도가 존재 하지 않습니다."));
			return;
		}

		String title = tr("실내 공간 정보 백터화 - {0}({1})", buildingNm, CommonUtil.getFloorFormat(floor));
		frame = new JFrame(title);
		JPanel p = new JPanel(){
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(image, 0, 0, this);
			}
		};
		p.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));

		JScrollPane pane = new JScrollPane(p);
		pane.setPreferredSize(p.getPreferredSize());
		frame.add(pane, BorderLayout.CENTER);

		Dimension d = CommonUtil.getMaximumDim(image.getWidth(), image.getHeight(), 250);
		frame.setPreferredSize(d);
		frame.setLocation(CommonUtil.getPosition(d));
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.pack();
		frame.addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {}

			@Override
			public void windowIconified(WindowEvent e) {}

			@Override
			public void windowDeiconified(WindowEvent e) {}

			@Override
			public void windowDeactivated(WindowEvent e) {}

			@Override
			public void windowClosing(WindowEvent e) {
				actionList.remove(ShowFloorPlanAction.this);
				frame.setVisible(false);
				frame.dispose();
				frame = null;
			}

			@Override
			public void windowClosed(WindowEvent e) {}

			@Override
			public void windowActivated(WindowEvent e) {}
		});

		frame.setVisible(true);
	}

	/**
	 * osmId 및 층에 해당하는 도면 이미지 정보 팝업을 반환하는 함수
	 * @param osmId
	 * @param floor
	 * @return 도면 이미지 정보 팝업 프레임 객체를 반환
	 */
	public JFrame getFrame(Long osmId, int floor){
		if(layer.getOsmPrimitiveId() == osmId && layer.getFloor() == floor){
			return this.frame;
		}
		return null;
	}

	/**
	 * osmId 및 층에 해당하는 도면 이미지 정보 팝업을 반환하는 함수
	 * @param osmId
	 * @param floor
	 * @return 도면 이미지 정보 팝업 프레임 객체를 반환
	 */
	public static JFrame getFrameFromList(Long osmId, int floor){
		for(ShowFloorPlanAction action : actionList){
			JFrame f = action.getFrame(osmId, floor);
			if(null != f){
				return f;
			}
		}
		return null;
	}
}
