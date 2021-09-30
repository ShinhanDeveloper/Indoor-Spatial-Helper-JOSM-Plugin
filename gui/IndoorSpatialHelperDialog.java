package org.openstreetmap.josm.plugins.indoorSpatialHelper.gui;

import static org.openstreetmap.josm.tools.I18n.tr;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.IndoorSpatialHelperController;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.DeleteObjectLayerAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.MergeObjectLayerAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.SplitObejctLayerAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.BuildingInfo;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.LayerManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.ComboItem;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.CommonUtil;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.GuiUtil;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.JTextFieldLimit;
import org.openstreetmap.josm.gui.layer.Layer;

/**
 *
 * 실내공간정보 구축을 위한 플러그인 다이어로그 뷰 클래스
 */
public class IndoorSpatialHelperDialog extends ToggleDialog {

	private static final long serialVersionUID = 2187078710036414075L;

	private JPanel mainPanel;

	private JPanel layerInfoPanel;
	private JPanel buildingInfoPanel;
	private JPanel floorPlanInfoPanel;
	private JPanel Open3DViewerInfoPanel;

	private JLabel buildingInfoLabel;
	private JLabel floorPlanInfoLabel;
	private JLabel Open3DViewerInfoLabel;

	private JButton btnSetBuldInfo;
	private JTextField buldNmTf;
	private JTextField buldAdresTf;
	private JTextField buldCrdntTf;
	private JTextField buldFloorTf;
	private JTextField setWallHeihtTF;

	private JComboBox<ComboItem> floorSelect;
	private JButton btnSetImage;
	private ButtonGroup buttonGroup;
	private JToggleButton btnRotate;
	private JToggleButton btnResize;
	private JToggleButton btnMove;
	private JButton btnRunVectorizing;
	private JButton btnCancel;
	private JButton btnOpen3DViewer;
	private JComboBox<ComboItem> floorHeightSelect;
	JButton btnSetHeight;

	/**
	 * {@link IndoorSpatialHelperDialog} 생성자
	 */
	public IndoorSpatialHelperDialog() {
		super(tr("실내 공간 정보 백터화"), "indoorspatialhelper", 
				tr("Toolbox for indoor spatial data building assistance"), null, 500, true);
		createPluginUI();
		
	}

	/**
	 * 레이아웃 설정 및 컴포넌트를 초기화하는 함수
	 */
	private void createPluginUI() {
		createLayerInfoPanel();
		createBuildingInfoPanel();
		createFloorPlanInfoPanel();
		create3DViewPanel();

		mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());
		((GridBagLayout) mainPanel.getLayout()).columnWidths = new int[] {0, 0};
		((GridBagLayout) mainPanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0};
		((GridBagLayout) mainPanel.getLayout()).columnWeights = new double[] {0.0, 1.0E-4};
		((GridBagLayout) mainPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

		buildingInfoLabel = new JLabel(tr("건물정보 설정 -----------------------------------"));
		buildingInfoLabel.setIcon(Constants.OPEN_ARROW_ICON);
		buildingInfoLabel.addMouseListener(new BuildingInfoLabelMouseListener());

		floorPlanInfoLabel = new JLabel(tr("평면도 설정 ------------------------------------"));
		floorPlanInfoLabel.setIcon(Constants.OPEN_ARROW_ICON);
		floorPlanInfoLabel.addMouseListener(new FloorPlanInfoLabelMouseListener());
		
		
		Open3DViewerInfoLabel = new JLabel(tr("건물 3D 설정 -----------------------------------"));
		Open3DViewerInfoLabel.setIcon(Constants.OPEN_ARROW_ICON);
		Open3DViewerInfoLabel.addMouseListener(new Open3DViewerInfoLabelMouseListener());

		mainPanel.add(layerInfoPanel, GuiUtil.getGridBagConst(0, 0, 1, 1));
		mainPanel.add(buildingInfoLabel, GuiUtil.getGridBagConst(0, 1, 1, 1));
		mainPanel.add(buildingInfoPanel, GuiUtil.getGridBagConst(0, 2, 1, 1));
		mainPanel.add(floorPlanInfoLabel, GuiUtil.getGridBagConst(0, 3, 1, 1));
		mainPanel.add(floorPlanInfoPanel, GuiUtil.getGridBagConst(0, 4, 1, 1));
		mainPanel.add(Open3DViewerInfoLabel, GuiUtil.getGridBagConst(0, 5, 1, 1));
		mainPanel.add(Open3DViewerInfoPanel, GuiUtil.getGridBagConst(0, 6, 1, 1));

		JPanel pluginPanel = new JPanel();
		pluginPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
		pluginPanel.setLayout(new BorderLayout());
		pluginPanel.add(mainPanel, BorderLayout.NORTH);
		this.createLayout(pluginPanel, true, null);

		setFloorPlanInfoPanelVisible(false);
		seOpen3DViewertInfoPanelVisible(false);
	}

	/**
	 * 오브젝트 레이어 패널 레이아웃 설정 및 컴포넌트를 초기화하는 함수
	 */
	private void createLayerInfoPanel() {
		layerInfoPanel = new JPanel();
		layerInfoPanel.setLayout(new BorderLayout());
		JScrollPane pane = new JScrollPane(LayerManager.getInstance());
		pane.setPreferredSize(new Dimension(300, 100));
		pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		layerInfoPanel.add(pane, BorderLayout.CENTER);

		JButton btnMergeLayer = new JButton(new MergeObjectLayerAction());
		JButton btnSplitLayer = new JButton(new SplitObejctLayerAction());
		JButton btnDeleteLayer = new JButton(new DeleteObjectLayerAction());

		JPanel btnPanel = new JPanel(new GridLayout(1, 3));
		btnPanel.add(btnMergeLayer);
		btnPanel.add(btnSplitLayer);
		btnPanel.add(btnDeleteLayer);

		layerInfoPanel.add(btnPanel, BorderLayout.SOUTH);
	}

	/**
	 * 건물 정보 패널 레이아웃 설정 및 컴포넌트를 초기화하는 함수
	 */
	private void createBuildingInfoPanel() {
		buildingInfoPanel = new JPanel();

		btnSetBuldInfo = new JButton();
		buldNmTf = new JTextField();
		buldAdresTf = new JTextField();
		buldCrdntTf = new JTextField();
		buldFloorTf = new JTextField();

		buildingInfoPanel.setLayout(new GridBagLayout());
		((GridBagLayout) buildingInfoPanel.getLayout()).columnWidths = new int[] {80, 70, 180, 0};
		((GridBagLayout) buildingInfoPanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
		((GridBagLayout) buildingInfoPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
		((GridBagLayout) buildingInfoPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

		btnSetBuldInfo.setText(tr("건물 설정"));
		btnSetBuldInfo.setToolTipText(tr("건물 정보 설정"));
		btnSetBuldInfo.setIcon(Constants.BUILDING_ICON);
		buildingInfoPanel.add(btnSetBuldInfo, GuiUtil.getGridBagConst(0, 0, 3, 1));

		buildingInfoPanel.add(new JLabel(tr("Name")), GuiUtil.getGridBagConst(0, 1, 1, 1));
		buildingInfoPanel.add(new JLabel(tr("Address")), GuiUtil.getGridBagConst(0, 2, 1, 1));
		buildingInfoPanel.add(new JLabel(tr("Coordinates")), GuiUtil.getGridBagConst(0, 3, 1, 1));
		buildingInfoPanel.add(new JLabel(tr("층 정보")), GuiUtil.getGridBagConst(0, 4, 1, 1));

		buldNmTf.setEditable(false);
		buldAdresTf.setEditable(false);
		buldCrdntTf.setEditable(false);
		buldFloorTf.setEditable(false);
		buildingInfoPanel.add(buldNmTf, GuiUtil.getGridBagConst(1, 1, 2, 1));
		buildingInfoPanel.add(buldAdresTf, GuiUtil.getGridBagConst(1, 2, 2, 1));
		buildingInfoPanel.add(buldCrdntTf, GuiUtil.getGridBagConst(1, 3, 2, 1));
		buildingInfoPanel.add(buldFloorTf, GuiUtil.getGridBagConst(1, 4, 2, 1));
	}
	private void manItemInCombo() {

    }
	/**
	 * 도면 정보 패널 레이아웃 설정 및 컴포넌트를 초기화하는 함수
	 */
	private void createFloorPlanInfoPanel() {
		floorPlanInfoPanel = new JPanel();

		floorSelect = new JComboBox<>();
		
		btnSetImage = new JButton();
		buttonGroup = new ButtonGroup();
		btnRotate = new JToggleButton();
		btnResize = new JToggleButton();
		btnMove = new JToggleButton();
		btnRunVectorizing = new JButton();
		btnCancel = new JButton();

		floorPlanInfoPanel.setLayout(new GridBagLayout());
		((GridBagLayout) floorPlanInfoPanel.getLayout()).columnWidths = new int[] {80, 50, 50, 0}; //new int[] {50, 50, 50, 50, 50, 50, 0};
		((GridBagLayout) floorPlanInfoPanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
		((GridBagLayout) floorPlanInfoPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
		((GridBagLayout) floorPlanInfoPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

		floorSelect.addItem(new ComboItem("0", tr("층 설정")));
		
		floorSelect.setRenderer(new DefaultListCellRenderer(){
			private static final long serialVersionUID = -3735675665832641281L;

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				ComboItem combo = (ComboItem) value;
				int floor = 0;
				try{
					floor = Integer.parseInt(combo.getKey());
				}catch(NumberFormatException e){
					floor = 0;
				}
				this.setText(combo.toString());
				this.setIcon(floor > 0 ? Constants.UPSTAIRS_ICON : floor < 0 ? Constants.DOWNSTAIRS_ICON : null);
				return this;
			}
		});
		
		floorPlanInfoPanel.add(floorSelect, GuiUtil.getGridBagConst(0, 0, 3, 1));

		btnSetImage.setText(tr("평면도 설정"));
//		btnSetImage.setToolTipText(tr("Sets file of the floor Plan."));
		btnSetImage.setToolTipText(tr("평면도 설정을 위한 파일 선택"));
		btnSetImage.setIcon(Constants.UPLOAD_IMAGE_ICON);
		floorPlanInfoPanel.add(btnSetImage, GuiUtil.getGridBagConst(3, 0, 3, 1));

		btnRotate.setText(tr("회전"));
		btnRotate.setToolTipText(tr("평면도 회전 설정"));
		buttonGroup.add(btnRotate);
		floorPlanInfoPanel.add(btnRotate, GuiUtil.getGridBagConst(0, 1, 2, 1));

		btnResize.setText(tr("크기 조절"));
		btnResize.setToolTipText(tr("평면도 크기 설정"));
		buttonGroup.add(btnResize);
		floorPlanInfoPanel.add(btnResize, GuiUtil.getGridBagConst(2, 1, 2, 1));

		btnMove.setText(tr("이동"));
		btnMove.setToolTipText(tr("평면도 위치 설정"));
		buttonGroup.add(btnMove);
		floorPlanInfoPanel.add(btnMove, GuiUtil.getGridBagConst(4, 1, 2, 1));

		btnRunVectorizing.setText(tr("백터변환"));
		btnRunVectorizing.setToolTipText(tr("백터 변환 실행"));
		btnRunVectorizing.setIcon(Constants.VECTOR_ICON);
		floorPlanInfoPanel.add(btnRunVectorizing, GuiUtil.getGridBagConst(0, 2, 3, 1));

		btnCancel.setText(tr("취소"));
		floorPlanInfoPanel.add(btnCancel, GuiUtil.getGridBagConst(3, 2, 3, 1));
		
	}
	
	private void create3DViewPanel() {
		Open3DViewerInfoPanel = new JPanel();
		btnOpen3DViewer = new JButton();
		floorHeightSelect = new JComboBox<>() ;

		Open3DViewerInfoPanel.setLayout(new GridBagLayout());
//		((GridBagLayout) Open3DViewerInfoPanel.getLayout()).columnWidths = new int[] {230, 50, 50, 0};
		((GridBagLayout) Open3DViewerInfoPanel.getLayout()).columnWidths = new int[] {80, 50, 50, 0};
		((GridBagLayout) Open3DViewerInfoPanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
		((GridBagLayout) Open3DViewerInfoPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
		((GridBagLayout) Open3DViewerInfoPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

		// 3D Viewer Button
		btnOpen3DViewer.setText(tr("3DViewer로 보기"));
		btnOpen3DViewer.setToolTipText(tr("3DViewer 열기"));
		btnOpen3DViewer.setIcon(Constants.SHOWONLY_ICON);
		Open3DViewerInfoPanel.add(btnOpen3DViewer, GuiUtil.getGridBagConst(0, 2, 6, 1));
		
		// 건물 유형에 따른 층고 선택 드롭 박스 
		floorHeightSelect.addItem(new ComboItem("0", tr("건물 유형에 따른 층고 선택(기본 2500mm)"), 2500));
        floorHeightSelect.addItem(new ComboItem("1", tr("단독주택(3122mm)"), 3122));
        floorHeightSelect.addItem(new ComboItem("2", tr("다가구주택(2817mm)"), 2817));
        floorHeightSelect.addItem(new ComboItem("3", tr("아파트(2855mm)"), 2855));
        floorHeightSelect.addItem(new ComboItem("4", tr("연립주택(2731mm)"), 2731));
        floorHeightSelect.addItem(new ComboItem("5", tr("다세대주택(2726mm)"), 2726));

        floorHeightSelect.addItem(new ComboItem("6", tr("기숙사(3105mm)"), 3105));
        floorHeightSelect.addItem(new ComboItem("7", tr("제1종근린(3341mm)"), 3341));
        floorHeightSelect.addItem(new ComboItem("8", tr("근린공공시설(1000m^2미만)(3554mm)"), 3554));
        floorHeightSelect.addItem(new ComboItem("9", tr("마을공동시설(3789mm)"), 3789));
        floorHeightSelect.addItem(new ComboItem("10", tr("기타그린공공시설(3170mm)"), 3170));

        floorHeightSelect.addItem(new ComboItem("11", tr("제2종근린(3438mm)"), 3438));
        floorHeightSelect.addItem(new ComboItem("12", tr("문화집회(3704mm)"), 3704));
        floorHeightSelect.addItem(new ComboItem("13", tr("업무(3069mm)"), 3069));
        floorHeightSelect.addItem(new ComboItem("14", tr("제조(2360mm)"), 2360));
        floorHeightSelect.addItem(new ComboItem("15", tr("오락(3519mm)"), 3519));

        floorHeightSelect.addItem(new ComboItem("16", tr("공연장(근생재외)(4555mm)"), 4555));
        floorHeightSelect.addItem(new ComboItem("17", tr("집회장(근생재외)(4688mm)"), 4648));
        floorHeightSelect.addItem(new ComboItem("18", tr("관람장(관람석면적1000m^2이상)(4907mm)"), 4907));
        floorHeightSelect.addItem(new ComboItem("19", tr("전시장(4452mm)"), 4452));
        floorHeightSelect.addItem(new ComboItem("20", tr("종교집회장(4157mm)"), 4157));

        floorHeightSelect.addItem(new ComboItem("21", tr("도매시장(시자내근생포함)(5097mm)"), 5097));
        floorHeightSelect.addItem(new ComboItem("22", tr("소매시장(건축물내근생포함)(4677mm)"), 4677));
        floorHeightSelect.addItem(new ComboItem("23", tr("상점(건축물근생포함)(4386mm)"), 4386));
        floorHeightSelect.addItem(new ComboItem("24", tr("철도시설(3747mm)"), 3747));
        floorHeightSelect.addItem(new ComboItem("25", tr("병원(4163mm)"), 4163));

        floorHeightSelect.addItem(new ComboItem("26", tr("학교(3916mm)"), 3916 ));
        floorHeightSelect.addItem(new ComboItem("27", tr("교육원(4481mm)"), 4481));
        floorHeightSelect.addItem(new ComboItem("28", tr("직업훈련소(3375mm)"), 3375));
        floorHeightSelect.addItem(new ComboItem("29", tr("학원(자동차, 무도학원 제외)(4101mm)"), 4101));
        floorHeightSelect.addItem(new ComboItem("30", tr("연구소(대학부설연구소 제외)(4273mm)"), 4273));

        floorHeightSelect.addItem(new ComboItem("31", tr("도서관(1000m^2미만 공공도서관 제외)(4214mm)"), 4214));
        floorHeightSelect.addItem(new ComboItem("32", tr("아동관련시설(3512mm)"), 3512));
        floorHeightSelect.addItem(new ComboItem("33", tr("노인복지시설(3532mm)"), 3532));
        floorHeightSelect.addItem(new ComboItem("34", tr("기타복시시설(3843mm)"), 3843));
        floorHeightSelect.addItem(new ComboItem("35", tr("생활권수련시설(4056mm)"), 4056));

        floorHeightSelect.addItem(new ComboItem("36", tr("자연권수련시설(4050mm)"), 4050));
        floorHeightSelect.addItem(new ComboItem("37", tr("유스호스텔(3500mm)"), 3500));
        floorHeightSelect.addItem(new ComboItem("38", tr("운동시설(4198mm)"), 4198));
        floorHeightSelect.addItem(new ComboItem("39", tr("체육관(5292mm)"), 5292));
        floorHeightSelect.addItem(new ComboItem("40", tr("운동장(관람석면적1000m^2미만)(4828mm)"), 4828));

        floorHeightSelect.addItem(new ComboItem("41", tr("일반업무시설(4432mm)"), 4432));
        floorHeightSelect.addItem(new ComboItem("42", tr("일반숙박시설(3715mm)"), 3715));
        floorHeightSelect.addItem(new ComboItem("43", tr("관광숙박시설(3840mm)"), 3840));
        floorHeightSelect.addItem(new ComboItem("44", tr("공장(4528mm)"), 4528));
        floorHeightSelect.addItem(new ComboItem("45", tr("창고(5027mm)"), 5027));

        floorHeightSelect.addItem(new ComboItem("46", tr("주유소, 석유판매소(5322mm)"), 5322));
        floorHeightSelect.addItem(new ComboItem("47", tr("액화석유가스충전소, 판매소, 저장소(3400mm)"), 3400));
        floorHeightSelect.addItem(new ComboItem("48", tr("위험물제조소, 저장소, 취급소(5058mm)"), 5058));
        floorHeightSelect.addItem(new ComboItem("49", tr("주차장(3358mm)"), 3358));
        floorHeightSelect.addItem(new ComboItem("50", tr("세차장(1937mm)"), 1937));

        floorHeightSelect.addItem(new ComboItem("51", tr("차고(5000mm)"), 5000));
        floorHeightSelect.addItem(new ComboItem("52", tr("축사(3000mm)"), 3000));
        floorHeightSelect.addItem(new ComboItem("53", tr("쓰레기처리시설(5406mm)"), 5406));
        floorHeightSelect.addItem(new ComboItem("54", tr("폐기물처리시설(4750mm)"), 4750));
        floorHeightSelect.addItem(new ComboItem("55", tr("하수종말처리장(7967mm)"), 7967));

        floorHeightSelect.addItem(new ComboItem("56", tr("화장시설(5750mm)"), 5750));
        floorHeightSelect.addItem(new ComboItem("57", tr("봉안당(5417mm)"), 5417));
        floorHeightSelect.addItem(new ComboItem("58", tr("휴게소(3650mm)"), 3650));
        floorHeightSelect.addItem(new ComboItem("59", tr("공원/유원지/관광지 부속시설(4728mm)"), 4728));
        floorHeightSelect.addItem(new ComboItem("60", tr("주상복합시설(4980mm)"), 4980));

        floorHeightSelect.addItem(new ComboItem("61", tr("기타복합시설(4652mm)"), 4652));
        floorHeightSelect.addItem(new ComboItem("62", tr("경비동(5117mm)"), 5117));
		

		floorHeightSelect.addActionListener (new ActionListener () {
		    public void actionPerformed(ActionEvent e) {
				String height = Integer.toString(getHeight());
				setFloorHeight(height);
				setWallHeihtTF.setText(height);
		    }
		});

		((JLabel)floorHeightSelect.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
		Open3DViewerInfoPanel.add(floorHeightSelect, GuiUtil.getGridBagConst(0, 0, 6, 1));
		
		setWallHeihtTF = new JTextField();
		setWallHeihtTF.setDocument(new JTextFieldLimit(100));
		setWallHeihtTF.setPreferredSize(new Dimension(203, 24));
		setWallHeihtTF.setDocument(new JTextFieldLimit(10));
		Open3DViewerInfoPanel.add(setWallHeihtTF, GuiUtil.getGridBagConst(0, 1, 1, 1));
		
		btnSetHeight = new JButton();
		btnSetHeight.setText(tr("높이 설정(mm)"));
		Open3DViewerInfoPanel.add(btnSetHeight, GuiUtil.getGridBagConst(1, 1, 2, 1));
		btnSetHeight.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!CommonUtil.isPositiveNumber(setWallHeihtTF.getText())){
					String msg = tr("양수 값으로 설정해 주세요.");
					JOptionPane.showMessageDialog(MainApplication.getMainFrame(), msg);
				}else if(!(Double.parseDouble(setWallHeihtTF.getText()) > 0 )){
					String msg = tr("0보다 큰값으로 설정해 주세요.");
					JOptionPane.showMessageDialog(MainApplication.getMainFrame(), msg);
				} else {
					setFloorHeight(setWallHeihtTF.getText());
				}
			}

		});
	}

	/**
	 * 오브젝트의 높이값을 설정하는 함수
	 * @param height
	 */
	public void setFloorHeight(String height)
	{
		Layer layer = MainApplication.getLayerManager().getActiveLayer();
		if(!(layer instanceof VectorLayer)){
//					JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Please select building layer from layer list"));
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("건물을 선택 해 주세요."));
			return;
		}
		
		VectorLayer vLayer = (VectorLayer)layer;
		vLayer.setHeight(Integer.parseInt(height));
		vLayer.syncDataSet();
		String cls = Constants.WALL_CLASS_CODE[0];

		Collection<Way> ways = vLayer.getRealDataSet().getWays();
		
		for(Way w : ways){
			Map<String, String> tag = w.getKeys();
			if(tag == null || !tag.containsKey(Constants.INDOOR_OBJECT_KEY)) {
				continue;
			}
			if(null != cls && !tag.get(Constants.INDOOR_OBJECT_KEY).equals(cls)){
				continue;
			}
			tag.put("height", height);
			w.setKeys(tag);
		}
		vLayer.drawLayer();
	}


	/**
	 * 건물 정보 패널 가시화 여부를 설정하는 함수
	 * @param visible
	 */
	public void setBuildingInfoPanelVisible(Boolean visible) {
		if(!visible){
			buildingInfoLabel.setIcon(Constants.CLOSE_ARROW_ICON);
			buildingInfoLabel.setName("C");
			buildingInfoPanel.setVisible(false);
		}else{
			buildingInfoLabel.setIcon(Constants.OPEN_ARROW_ICON);
			buildingInfoLabel.setName("O");
			buildingInfoPanel.setVisible(true);
		}
	}

	/**
	 * 도면 정보 패널 가시화 여부를 설정하는 함수
	 * @param visible
	 */
	public void setFloorPlanInfoPanelVisible(Boolean visible) {
		if(!visible){
			floorPlanInfoLabel.setIcon(Constants.CLOSE_ARROW_ICON);
			floorPlanInfoLabel.setName("C");
			floorPlanInfoPanel.setVisible(false);
		}else{
			floorPlanInfoLabel.setIcon(Constants.OPEN_ARROW_ICON);
			floorPlanInfoLabel.setName("O");
			floorPlanInfoPanel.setVisible(true);
		}
	}
	
	/**
	 * 도면 정보 패널 가시화 여부를 설정하는 함수
	 * @param visible
	 */
	public void seOpen3DViewertInfoPanelVisible(Boolean visible) {
		if(!visible){
			Open3DViewerInfoLabel.setIcon(Constants.CLOSE_ARROW_ICON);
			Open3DViewerInfoLabel.setName("C");
			Open3DViewerInfoPanel.setVisible(false);
		}else{
			Open3DViewerInfoLabel.setIcon(Constants.OPEN_ARROW_ICON);
			Open3DViewerInfoLabel.setName("O");
			Open3DViewerInfoPanel.setVisible(true);
		}
	}

	/**
	 * 건물 정보 패널에 건물 정보를 설정하는 함수
	 * @param info
	 */
	public void setBuildingInfo(BuildingInfo info) {
		if(info == null){
			buldNmTf.setText("");
			buldAdresTf.setText("");
			buldCrdntTf.setText("");
			buldFloorTf.setText("");
			drawFloorSelect(null);
		}else{
			buldNmTf.setText(info.getNm());
			buldAdresTf.setText(info.getAddr());
			buldCrdntTf.setText(info.getLonLatText());
			buldFloorTf.setText(info.getFloorInfoText());
			drawFloorSelect(info);
		}
	}

	/**
	 * 편집 층 선택 콤보박스를 설정하는 함수
	 * @param info
	 */
	public void drawFloorSelect(BuildingInfo info) {
		floorSelect.removeAllItems();
		floorSelect.addItem(new ComboItem("0", tr("층 설정")));

		if(null == info){
			return;
		}
		for(int i=info.getLowestFloor(); i<=info.getTopFloor(); i++){
			if(i != 0){
				long osmId = IndoorSpatialHelperController.getBuildingManager().getOsmIdByBuildingInfo(info);
				String label = CommonUtil.getFloorFormat(i)
						+ (LayerManager.getInstance().containsVectorLayer(osmId, i) ? " - exists" : "");
				floorSelect.addItem(new ComboItem(String.valueOf(i), label));
			}
		}
	}
	
	public int getHeight() {
		return ((ComboItem)floorHeightSelect.getSelectedItem()).height;
	}

	/**
	 * 편집 층 콤보박스의 선택된 층을 반환하는 함수
	 * @return 선택된 층 반환
	 */
	public int getFloorSelect() {
		return (floorSelect.getSelectedItem() != null ? Integer.parseInt(((ComboItem)floorSelect.getSelectedItem()).getKey()) : 0);
	}

	/**
	 * 편집 층 콤보박스의 선택된 층을 반환하는 함수
	 * @return 선택된 층 반환
	 */
	public String getFloorSelectText() {
		return (floorSelect.getSelectedItem() != null ? floorSelect.getSelectedItem().toString() : "");
	}

	/**
	 * 편집 층 콤보박스의 선택된 층 정보를 초기화하는 함수
	 */
	public void resetFloorSelect() {
		if(floorSelect.getItemCount() > 0){
			floorSelect.setSelectedIndex(0);
		}
	}

	/**
	 * 이미지 레이어 매칭 액션 버튼 선택 초기화 함수
	 */
	public void clearToggle() {
		buttonGroup.clearSelection();
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnSetBuldInfo 컴포넌트 액션 리스너를 등록하는 함수
	 * @param listener
	 */
	public void setBtnSetBuildingInfoListener(ActionListener listener) {
		this.btnSetBuldInfo.addActionListener(listener);
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnSetImage 컴포넌트 액션 리스너를 등록하는 함수
	 * @param listener
	 */
	public void setBtnSetImageListener(ActionListener listener) {
		this.btnSetImage.addActionListener(listener);
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnRotate 컴포넌트 아이템 리스너를 등록하는 함수
	 * @param listener
	 */
	public void setBtnRotateItemListener(ItemListener listener) {
		this.btnRotate.addItemListener(listener);
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnResize 컴포넌트 아이템 리스너를 등록하는 함수
	 * @param listener
	 */
	public void setBtnResizeItemListener(ItemListener listener) {
		this.btnResize.addItemListener(listener);
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnMove 컴포넌트 아이템 리스너를 등록하는 함수
	 * @param listener
	 */
	public void setBtnMoveItemListener(ItemListener listener) {
		this.btnMove.addItemListener(listener);
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnRotate 컴포넌트 액션 리스너를 등록하는 함수
	 * @param listener
	 */
	public void setBtnRotateActionListener(ActionListener listener) {
		this.btnRotate.addActionListener(listener);
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnResize 컴포넌트 액션 리스너를 등록하는 함수
	 * @param listener
	 */
	public void setBtnResizeActionListener(ActionListener listener) {
		this.btnResize.addActionListener(listener);
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnMove 컴포넌트 액션 리스너를 등록하는 함수
	 * @param listener
	 */
	public void setBtnMoveActionListener(ActionListener listener) {
		this.btnMove.addActionListener(listener);
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnRunVectorizing 컴포넌트 액션 리스너를 등록하는 함수
	 * @param listener
	 */
	public void setBtnRunVectorizingListener(ActionListener listener) {
		this.btnRunVectorizing.addActionListener(listener);
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnCancel 컴포넌트 액션 리스너를 등록하는 함수
	 * @param listener
	 */
	public void setBtnCancelListener(ActionListener listener) {
		this.btnCancel.addActionListener(listener);
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnOpen3DViewer 컴포넌트 액션 리스너를 등록하는 함수
	 * @param listener
	 */
	public void setBtnOpen3DViewerListener(ActionListener listener) {
		this.btnOpen3DViewer.addActionListener(listener);
	}
	/**
	 * {@link IndoorSpatialHelperDialog}.buildingInfoLabel 컴포넌트 마우스 리스너 클래스
	 */
	class BuildingInfoLabelMouseListener implements MouseListener {

		@Override
		public void mouseReleased(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseClicked(MouseEvent e) {
			setBuildingInfoPanelVisible(!"O".equals(buildingInfoLabel.getName()));
		}
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.floorPlanInfoLabel 컴포넌트 마우스 리스너 클래스
	 */
	class FloorPlanInfoLabelMouseListener implements MouseListener {

		@Override
		public void mouseReleased(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseClicked(MouseEvent e) {
			setFloorPlanInfoPanelVisible(!"O".equals(floorPlanInfoLabel.getName()));
		}
	}
	
	/**
	 * {@link IndoorSpatialHelperDialog}.floorPlanInfoLabel 컴포넌트 마우스 리스너 클래스
	 */
	class Open3DViewerInfoLabelMouseListener implements MouseListener {

		@Override
		public void mouseReleased(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseClicked(MouseEvent e) {
			seOpen3DViewertInfoPanelVisible(!"O".equals(Open3DViewerInfoLabel.getName()));
		}
	}
}
