package org.openstreetmap.josm.plugins.indoorSpatialHelper.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.IndoorSpatialHelperController;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.BuildingInfo;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.CommonUtil;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.GuiUtil;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.JTextFieldLimit;

/**
 * 실내공간정보 구축을 위한 건물 층수 입력 뷰 클래스
 */
public class BuildingInfoPop extends JDialog {

	private static final long serialVersionUID = 10677107865849230L;

	private OsmPrimitive osmPrimitive;
	private BuildingInfo buildingInfo;

	private JTextField nmTf;
	private JTextField addrTf;
	private JSpinner spTopFloor;
	private JSpinner spLowestFloor;
	private JTextField lonTf;
	private JTextField latTf;

	private JButton okButton;
	private JButton cancelButton;

	/**
	 * 뷰 컴포넌트를 초기화하는 {@link BuildingInfoPop} 생성자
	 * @param frame
	 * @param osmPrimitive
	 */
	public BuildingInfoPop(JFrame frame, OsmPrimitive osmPrimitive) {
		super(frame, tr("Building Information"), true);
		this.osmPrimitive = osmPrimitive;
		this.buildingInfo = IndoorSpatialHelperController.getBuildingManager().getBuildingInfoObj(osmPrimitive);
		createPopUI();

		nmTf.setText(buildingInfo.getNm());
		addrTf.setText(buildingInfo.getAddr());
		spTopFloor.setValue(buildingInfo.getTopFloor());
		spLowestFloor.setValue(buildingInfo.getLowestFloor());
		lonTf.setText(String.valueOf(buildingInfo.getLon()));
		latTf.setText(String.valueOf(buildingInfo.getLat()));

		this.setFocusable(true);
		this.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {}
		});
	}

	/**
	 * 레이아웃 설정 및 컴포넌트를 초기화하는 함수
	 */
	private void createPopUI() {
		JPanel mainPanel = new JPanel();
		JPanel contentPanel = new JPanel();
		JPanel buttonPanel = new JPanel();

		nmTf = new JTextField();
		nmTf.setDocument(new JTextFieldLimit(100));
		addrTf = new JTextField();
		addrTf.setDocument(new JTextFieldLimit(500));
		spTopFloor = new JSpinner(new SpinnerNumberModel(1, 1, 200, 1));
		spLowestFloor = new JSpinner(new SpinnerNumberModel(1, -20, 1, 1));
		lonTf = new JTextField();
		lonTf.setDocument(new JTextFieldLimit(22));
		latTf = new JTextField();
		latTf.setDocument(new JTextFieldLimit(22));

		okButton = new JButton();
		cancelButton = new JButton();

		//======== this ========
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		//======== mainPanel ========
		mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

		//======== contentPanel ========
		contentPanel.setLayout(new GridBagLayout());
		((GridBagLayout) contentPanel.getLayout()).columnWidths = new int[] {50, 50, 100, 50, 100, 0};
		((GridBagLayout) contentPanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0};
		((GridBagLayout) contentPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
		((GridBagLayout) contentPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

		contentPanel.add(new JLabel(tr("Name")), GuiUtil.getGridBagConst(0, 0, 2, 1));
		contentPanel.add(new JLabel(tr("Address")), GuiUtil.getGridBagConst(0, 1, 2, 1));
		contentPanel.add(new JLabel(tr("층정보")), GuiUtil.getGridBagConst(0, 2, 1, 1));
//		contentPanel.add(new JLabel(tr("Top")), GuiUtil.getGridBagConst(1, 2, 1, 1));
//		contentPanel.add(new JLabel(tr("Lowest")), GuiUtil.getGridBagConst(3, 2, 1, 1));

		contentPanel.add(new JLabel(tr("아래층")), GuiUtil.getGridBagConst(1, 2, 1, 1));
		contentPanel.add(new JLabel(tr("  위층")), GuiUtil.getGridBagConst2(3, 2, 2, 1));

		contentPanel.add(new JLabel(tr("Coordinates")), GuiUtil.getGridBagConst(0, 3, 1, 2));
		contentPanel.add(new JLabel(tr("Longitude")), GuiUtil.getGridBagConst(1, 3, 1, 1));
		contentPanel.add(new JLabel(tr("Latitude")), GuiUtil.getGridBagConst(1, 4, 1, 1));

		contentPanel.add(nmTf, GuiUtil.getGridBagConst(2, 0, 3, 1));
		contentPanel.add(addrTf, GuiUtil.getGridBagConst(2, 1, 3, 1));

//		contentPanel.add(spTopFloor, GuiUtil.getGridBagConst(2, 2, 1, 1));
//		contentPanel.add(spLowestFloor, GuiUtil.getGridBagConst(4, 2, 1, 1));
		contentPanel.add(spLowestFloor, GuiUtil.getGridBagConst(2, 2, 1, 1));
		contentPanel.add(spTopFloor, GuiUtil.getGridBagConst(4, 2, 1, 1));

		contentPanel.add(lonTf, GuiUtil.getGridBagConst(2, 3, 3, 1));
		contentPanel.add(latTf, GuiUtil.getGridBagConst(2, 4, 3, 1));

		mainPanel.add(contentPanel);

		//======== buttonPanel ========
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		okButton.setText(tr("OK"));
		buttonPanel.add(okButton);

		cancelButton.setText(tr("Cancel"));
		buttonPanel.add(cancelButton);

		mainPanel.add(buttonPanel);

		contentPane.add(mainPanel, BorderLayout.NORTH);
		pack();
		setLocationRelativeTo(getOwner());
	}

	/**
	 * 건물의 최고 층수 반환하는 함수
	 * @return 최고 층수 반환
	 */
	public int getTopFloor() {
		try{
			return Integer.parseInt(String.valueOf(spTopFloor.getValue()));
		}catch(NumberFormatException e){
			return 1;
		}
	}

	/**
	 * 건물의 최저 층수 반환하는 함수
	 * @return 최저 층수 반환
	 */
	public int getLowestFloor() {
		try{
			return Integer.parseInt(String.valueOf(spLowestFloor.getValue()));
		}catch(NumberFormatException e){
			return 1;
		}
	}

	/**
	 * 입력된 건물 정보의 유효성을 체크하는 함수
	 * @return 유효성 체크 결과 반환
	 */
	public boolean validation() {

		if("".equals(nmTf.getText().trim())){
			JOptionPane.showMessageDialog(this, tr("Please enter the name of the building."));
			nmTf.requestFocus();
			return false;
		}

		for(Layer l : MainApplication.getLayerManager().getLayers()){
			if(l instanceof VectorLayer){
				VectorLayer layer = (VectorLayer)l;
				if(layer.getOsmPrimitiveId() == osmPrimitive.getId()){
					if(layer.getFloor() < Integer.parseInt(String.valueOf(spLowestFloor.getValue()))){
						JOptionPane.showMessageDialog(this, tr("There is a lower floor than the floor you set in the layer list.\nPlease delete the layer and try again."));
						spLowestFloor.requestFocus();
						return false;
					}
					if(layer.getFloor() > Integer.parseInt(String.valueOf(spTopFloor.getValue()))){
						JOptionPane.showMessageDialog(this, tr("There is a higher floor than the floor you set in the layer list.\nPlease delete the layer and try again."));
						spTopFloor.requestFocus();
						return false;
					}
				}
			}
		}

		if(!CommonUtil.isRealNumber(lonTf.getText())){
			JOptionPane.showMessageDialog(this, tr("Please check the coordinate format.\nIt needs to real number."));
			lonTf.requestFocus();
			return false;
		}
		if(-180 > Double.parseDouble(lonTf.getText()) || Double.parseDouble(lonTf.getText()) > 180){
			JOptionPane.showMessageDialog(this, tr("Please check the range of longitude."));
			lonTf.requestFocus();
			return false;
		}
		if(!CommonUtil.isRealNumber(latTf.getText())){
			JOptionPane.showMessageDialog(this, tr("Please check the coordinate format.\nIt needs to real number."));
			latTf.requestFocus();
			return false;
		}
		if(-90 > Double.parseDouble(latTf.getText()) || Double.parseDouble(latTf.getText()) > 90){
			JOptionPane.showMessageDialog(this, tr("Please check the range of latitude."));
			latTf.requestFocus();
			return false;
		}
		return true;
	}

	/**
	 * 건물 정보 객체를 반환하는 함수
	 * @return 건물 정보 객체를 반환
	 */
	public BuildingInfo getBuildingInfo() {
		buildingInfo.setNm(nmTf.getText());
		buildingInfo.setAddr(addrTf.getText());
		buildingInfo.setTopFloor(Integer.parseInt(String.valueOf(spTopFloor.getValue())));
		buildingInfo.setLowestFloor(Integer.parseInt(String.valueOf(spLowestFloor.getValue())));
		buildingInfo.setLon(Double.parseDouble(lonTf.getText()));
		buildingInfo.setLat(Double.parseDouble(latTf.getText()));

		return buildingInfo;

	}

	/**
	 * {@link BuildingInfoPop}.okButton 컴포넌트 액션 리스너를 등록하는 함수
	 * @param listener
	 */
	public void setOkButtonListener(ActionListener listener) {
		this.okButton.addActionListener(listener);
	}

	/**
	 * {@link BuildingInfoPop}.cancelButton 컴포넌트 액션 리스너를 등록하는 함수
	 * @param listener
	 */
	public void setCancelButtonListener(ActionListener listener) {
		this.cancelButton.addActionListener(listener);
	}

	/**
	 * 건물 정보 입력 팝업 윈도우 리스너를 등록하는 함수
	 * @param listener
	 */
	public void setBuildingInfoPopWindowListener(WindowListener listener) {
		this.addWindowListener(listener);
	}
}
