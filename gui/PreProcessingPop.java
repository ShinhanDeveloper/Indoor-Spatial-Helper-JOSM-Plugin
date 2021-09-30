package org.openstreetmap.josm.plugins.indoorSpatialHelper.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;

import org.openstreetmap.josm.plugins.indoorSpatialHelper.IndoorSpatialHelperController;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.CommonUtil;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

/**
 * 실내공간정보 구축을 위한 이미지 전처리 팝업 뷰 클래스
 */
public class PreProcessingPop extends JDialog {

	private static final long serialVersionUID = -7498190251735584132L;

	private static final int BUTTON_PANEL_HEIGHT = 35 + 25;
	private static final int DIALOG_CORRECTION_WIDTH = 11;
	private static final int DIALOG_CORRECTION_HEIGHT = 32;

	private static final int DIALOG_MARGIN = 200;

	private ArrayList<BufferedImage> commands = new ArrayList<BufferedImage>();
	//private BufferedImage image;

	private ImagePanel imagePanel;

	private ButtonGroup buttonGroup;
	private JButton btnUndo;
	private JButton btnRedo;

	JLabel statusLabel = new JLabel("x:0 y:0");

	private static final int BACKGROUND_DETECTING_RANGE = 20;
	private Color BACKGROUND_COLOR = Color.WHITE;

	private int index = 0;

	private boolean dialogResult = false;

	/**
	 * {@link PreProcessingPop} 생성자
	 * @param frame
	 * @param file
	 * @throws Exception
	 */
	public PreProcessingPop(JFrame frame, File file) throws Exception {
		super(frame, tr("실내 공간 정보 백터화"), true);

		try{
			commands.add(ImageIO.read(file));
		}catch(IOException e){
			throw e;
		}

		getBackgroundColor(commands.get(0));

		initComponents();

		this.addWindowListener(new WindowListener() {
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
//				int confirmResult = JOptionPane.showConfirmDialog(null,
//						"Would you like to reset your work and return to the previous step?", "Confirm",
//						JOptionPane.YES_NO_OPTION);
				int confirmResult = JOptionPane.showConfirmDialog(null,
						"작업을 취소하고 이전 단계로 돌아갈까요?", "확인",
						JOptionPane.YES_NO_OPTION);
				if(confirmResult == JOptionPane.YES_OPTION) {
					dialogResult = false;
					PreProcessingPop.this.setVisible(false);
				}
			}

			@Override
			public void windowClosed(WindowEvent e) {}

			@Override
			public void windowActivated(WindowEvent e) {}
		});
		this.setFocusable(true);
		this.requestFocus();
		this.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_R || e.getKeyCode() == KeyEvent.VK_L){
					Enumeration<AbstractButton> buttons = buttonGroup.getElements();
					while(buttons.hasMoreElements()){
						AbstractButton button = buttons.nextElement();
						if(button.getName().equals(String.valueOf(e.getKeyCode()))){
							button.setSelected(true);
							imagePanel.setMode(button.getName());
							break;
						}
					}
				}else if(e.getKeyCode() == KeyEvent.VK_DELETE){
					deleteCrop();
				}else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z){
					undo();
				}else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Y){
					redo();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {}
		});
	}

	/**
	 * 레이아웃 설정 및 컴포넌트를 초기화하는 함수
	 */
	private void initComponents() {
		JPanel toolbarPanel = makeToolBarPanel();
		JScrollPane scrollPane = makeImagePanel();
		JPanel buttonPanel = makeButtonPanel();

		this.add(toolbarPanel, BorderLayout.NORTH);
		this.add(scrollPane, BorderLayout.CENTER);
		this.add(buttonPanel, BorderLayout.SOUTH);

		int width = (int)scrollPane.getPreferredSize().getWidth() + DIALOG_CORRECTION_WIDTH;
		int height = (int) toolbarPanel.getPreferredSize().getHeight()
				+ (int) scrollPane.getPreferredSize().getHeight()
				+ (int) buttonPanel.getPreferredSize().getHeight()
				+ DIALOG_CORRECTION_HEIGHT;

		Dimension d = CommonUtil.getMaximumDim(width, height, DIALOG_MARGIN);

		this.setPreferredSize(d);
		this.setLocation(CommonUtil.getPosition(d));
		this.setResizable(false);
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.pack();

		setUndoRedoButton();
	}

	/**
	 * 툴바 패널의 레이아웃 설정 및 컴포넌트를 초기화하는 함수
	 * @return 툴바 패널 객체 반환
	 */
	private JPanel makeToolBarPanel() {
		JPanel toolbarPanel = new JPanel();
		JToolBar toolbar = new JToolBar();

		toolbarPanel.setLayout(new BorderLayout());

		JToggleButton btnRectMode = new JToggleButton();
		btnRectMode.setIcon(new ImageIcon(IndoorSpatialHelperController.class.getResource("/images/dialogs/rect.png")));
		btnRectMode.setToolTipText(tr("Rectangle Mode (R)"));
		btnRectMode.setName(String.valueOf(KeyEvent.VK_R));
		btnRectMode.setFocusable(false);
		btnRectMode.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(null != imagePanel){
					imagePanel.setMode(((AbstractButton)e.getSource()).getName());
				}
			}
		});

		JToggleButton btnLassoMode = new JToggleButton();
		btnLassoMode.setIcon(new ImageIcon(IndoorSpatialHelperController.class.getResource("/images/dialogs/lasso.png")));
		btnLassoMode.setToolTipText(tr("Lasso Mode (L)"));
		btnLassoMode.setName(String.valueOf(KeyEvent.VK_L));
		btnLassoMode.setFocusable(false);
		btnLassoMode.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(null != imagePanel){
					imagePanel.setMode(((AbstractButton)e.getSource()).getName());
				}
			}
		});

		buttonGroup = new ButtonGroup();
		buttonGroup.add(btnRectMode);
		buttonGroup.add(btnLassoMode);
		buttonGroup.setSelected(btnRectMode.getModel(), true);

		JButton btnErase = new JButton();
		btnErase.setIcon(new ImageIcon(IndoorSpatialHelperController.class.getResource("/images/dialogs/eraser.png")));
		btnErase.setToolTipText(tr("Eraser (Delete)"));
		btnErase.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				deleteCrop();
			}
		});

		btnUndo = new JButton();
		btnUndo.setIcon(new ImageIcon(IndoorSpatialHelperController.class.getResource("/images/dialogs/undo.png")));
		btnUndo.setToolTipText(tr("Un-Do (Ctrl+Z)"));
		btnUndo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				undo();
			}
		});

		btnRedo = new JButton();
		btnRedo.setIcon(new ImageIcon(IndoorSpatialHelperController.class.getResource("/images/dialogs/redo.png")));
		btnRedo.setToolTipText(tr("Re-Do (Ctrl+Y)"));
		btnRedo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				redo();
			}
		});
		toolbar.setFloatable(false);
		toolbar.add(btnRectMode);
		toolbar.add(btnLassoMode);
		toolbar.addSeparator();
		toolbar.add(btnErase);
		toolbar.addSeparator();
		toolbar.add(btnUndo);
		toolbar.add(btnRedo);
		toolbarPanel.add(toolbar);

		return toolbarPanel;
	}

	/**
	 * 이미지 패널의 레이아웃 설정 및 컴포넌트를 초기화하는 함수
	 * @return 이미지 패널이 포함된 스크롤 패인 객체 반환
	 */
	private JScrollPane makeImagePanel() {
		imagePanel = new ImagePanel(this, commands.get(0));
		JScrollPane scrollPane = new JScrollPane(imagePanel);

		imagePanel.setPreferredSize(new Dimension(commands.get(0).getWidth(), commands.get(0).getHeight()));
		scrollPane.setPreferredSize(imagePanel.getPreferredSize());

		imagePanel.setMode(getMode());

		return scrollPane;
	}

	/**
	 * 버튼 패널의 레이아웃 설정 및 컴포넌트를 초기화하는 함수
	 * @return 버튼 패널 객체 반환
	 */
	private JPanel makeButtonPanel() {
		JPanel buttonPanel = new JPanel(new GridLayout(1, 3));
		JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

		buttonPanel.setPreferredSize(new Dimension(commands.get(0).getWidth(), BUTTON_PANEL_HEIGHT));

		leftPanel.add(statusLabel);

		JButton btnOk = new JButton();
		btnOk.setFont(new Font("맑은고딕", 0, 30));
		btnOk.setText(tr("OK"));
		btnOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
//				int confirmResult = JOptionPane.showConfirmDialog(null,
//						"Would you like to go to next step with current condition?", "Confirm",
//						JOptionPane.YES_NO_OPTION);
				int confirmResult = JOptionPane.showConfirmDialog(null,
						"작업한 내용으로 다음 단계로 이동 하시겠습니까?", "확인",
						JOptionPane.YES_NO_OPTION);
				if(confirmResult == JOptionPane.YES_OPTION) {
					dialogResult = true;
					PreProcessingPop.this.setVisible(false);
				} else {
					PreProcessingPop.this.requestFocus();
				}
			}
		});
		centerPanel.add(btnOk);

		JButton btnCancel = new JButton();
		btnCancel.setFont(new Font("맑은고딕", 0, 30));
		btnCancel.setText(tr("Cancel"));
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
//				int confirmResult = JOptionPane.showConfirmDialog(null,
//						"Would you like to reset your work and return to the previous step?", "Confirm",
//						JOptionPane.YES_NO_OPTION);

				int confirmResult = JOptionPane.showConfirmDialog(null,
						"작업을 취소하고 이전 단계로 돌아갈까요?", "확인",
						JOptionPane.YES_NO_OPTION);
				if(confirmResult == JOptionPane.YES_OPTION) {
					dialogResult = false;
					PreProcessingPop.this.setVisible(false);
				} else {
					PreProcessingPop.this.requestFocus();
				}
			}
		});
		centerPanel.add(btnCancel);

		buttonPanel.add(leftPanel);
		buttonPanel.add(centerPanel);
		buttonPanel.add(new JPanel());
		return buttonPanel;
	}

	/**
	 * 이미지의 백그라운드 컬러를 추출하는 함수
	 * @param img
	 */
	private void getBackgroundColor(BufferedImage img) {
		int w = (img.getWidth() < BACKGROUND_DETECTING_RANGE ? img.getWidth() : BACKGROUND_DETECTING_RANGE);
		int h = (img.getHeight() < BACKGROUND_DETECTING_RANGE ? img.getHeight() : BACKGROUND_DETECTING_RANGE);

		ArrayList<Color> colorList = new ArrayList<Color>();
		for(int i=0; i<w; i++){
			for(int j=0; j<h; j++){
				colorList.add(new Color(img.getRGB(i, j)));
			}
		}
		//중복제거
		HashSet<Color> hs = new HashSet<Color>(colorList);
		colorList = new ArrayList<Color>(hs);

		//정렬
		Color[] colors = colorList.toArray(new Color[colorList.size()]);
		Arrays.sort(colors, new Comparator<Color>() {
			@Override
			public int compare(Color c1, Color c2) {
				return c2.getRGB() - c1.getRGB();
			}
		});

		if(null != colors && colors.length > 0) {
			BACKGROUND_COLOR = colors[0];
		}
	}

	/**
	 * 이미지의 선택된 영역을 배경색으로 변경하는 함수
	 */
	private void deleteCrop() {
		BufferedImage img = imagePanel.deleteCrop(BACKGROUND_COLOR);
		if(null != img){
			if(index + 1 < commands.size()) {
				for(int i=commands.size()-1; i>index; i--) {
					commands.remove(i);
				}
			}
			commands.add(++index, img);

			while(Constants.REDO_UNDO_COUNT < commands.size()){
				commands.remove(0);
				index--;
			}
			setUndoRedoButton();
		}
		setUndoRedoButton();
		this.requestFocus();
	}

	/**
	 * Redo를 실행하는 함수
	 */
	private void redo() {
		if(index < commands.size() - 1) {
			imagePanel.setImage(commands.get(++index));
		}
		setUndoRedoButton();
		this.requestFocus();
	}

	/**
	 * Undo를 실행하는 함수
	 */
	private void undo() {
		if(index > 0){
			imagePanel.setImage(commands.get(--index));
		}
		setUndoRedoButton();
		this.requestFocus();
	}

	/**
	 * Redo-Undo 버튼의 활성화 여부를 설정하는 함수
	 */
	private void setUndoRedoButton() {

		if(commands.size() > 0 && index > 0) {
			btnUndo.setEnabled(true);
		} else {
			btnUndo.setEnabled(false);
		}

		if(commands.size() > 0 && index < commands.size() - 1) {
			btnRedo.setEnabled(true);
		} else {
			btnRedo.setEnabled(false);
		}
	}

	/**
	 * 마우스 커서의 좌표 정보를 설정하는 함수
	 * @param x
	 * @param y
	 */
	public void setStatusLabel(int x, int y){
		statusLabel.setText("x:"+x+" y:"+y);
	}

	/**
	 * 이미지 전처리 팝업 결과 적용 여부를 반환하는 함수
	 * @return 결과 적용 여부를 반환
	 */
	public boolean getResult() {
		return dialogResult;
	}

	/**
	 * 전처리된 이미지를 반환하는 함수
	 * @return 전처리된 이미지 반환
	 */
	public BufferedImage getImage() {
		return imagePanel.getImage();
	}

	/**
	 * 선택 모드를 반환하는 함수
	 * @return 선택 모드 반환
	 */
	public String getMode() {
		Enumeration<AbstractButton> buttons = buttonGroup.getElements();
		while(buttons.hasMoreElements()){
			AbstractButton button = buttons.nextElement();
			if(button.isSelected()){
				return button.getName();
			}
		}
		return "";
	}

	/**
	 * 이미지 전처리를 처리하는  뷰 클래스
	 */
	class ImagePanel extends JPanel implements MouseListener, MouseMotionListener {

		private static final long serialVersionUID = 5529204949810599289L;

		private PreProcessingPop parent;
		private BufferedImage image;
		private String mode;
		private int x1, y1, x2, y2;
		private Polygon lasso;

		/**
		 * {@link ImagePanel} 생성자
		 * @param parent
		 * @param image
		 */
		public ImagePanel(PreProcessingPop parent, BufferedImage image) {
			this.parent = parent;
			this.image = imageCopy(image);
			x1 = y1 = x2 = y2 = 0;
			lasso = new Polygon();
			this.addMouseListener(this);
			this.addMouseMotionListener(this);
			this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		}

		/**
		 * 패널의 이미지를 설정하는 함수
		 * @param image
		 */
		public void setImage(BufferedImage image) {
			this.image = imageCopy(image);
			repaint();
		}

		/**
		 * 패널의 이미지를 반환하는 함수
		 * @return 패널의 이미지 반환
		 */
		public BufferedImage getImage() {
			return imageCopy(this.image);
		}

		/**
		 * 이미지의 선택된 영역을 해당 색으로 변경하는 함수
		 * @param color
		 * @return 선택 영역의 색이 변경된 이미지 반환
		 */
		public BufferedImage deleteCrop(Color color){
			if(mode.equals(String.valueOf(KeyEvent.VK_R))){
				return deleteRect(color);
			}else if(mode.equals(String.valueOf(KeyEvent.VK_L))){
				return deleteLasso(color);
			}
			return null;
		}


		/**
		 * 이미지의 선택된 사각형 영역을 해당 색으로 변경하는 함수
		 * @param color
		 * @return 선택 영역의 색이 변경된 이미지 반환
		 */
		private BufferedImage deleteRect(Color color) {
			int px = Math.min(x1, x2);
			int py = Math.min(y1, y2);
			int pw = Math.abs(x1 - x2);
			int ph = Math.abs(y1 - y2);

			if(pw > 0 && ph > 0){
				for(int i=px; i<=(px + pw); i++) {
					for(int j=py; j<=(py + ph); j++) {
						image.setRGB(i, j, color.getRGB());
					}
				}
				repaint();
				return imageCopy(this.image);
			}
			return null;
		}

		/**
		 * 이미지의 선택된 자유곡선 영역을 해당 색으로 변경하는 함수
		 * @param color
		 * @return 선택 영역의 색이 변경된 이미지 반환
		 */
		private BufferedImage deleteLasso(Color color) {
			if(lasso.getBounds().getWidth() > 0 && lasso.getBounds().getHeight() > 0){
				Graphics2D g = image.createGraphics();
				g.setColor(color);
				g.fillPolygon(lasso);
				repaint();
				return imageCopy(this.image);
			}
			return null;
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(image, 0, 0, this);
			g.setColor(Color.RED);
			if(mode.equals(String.valueOf(KeyEvent.VK_R))){
				drawRect(g);
			}else if(mode.equals(String.valueOf(KeyEvent.VK_L))){
				drawLasso(g);
			}
		}

		/**
		 * 사각형 선택 영역을 그리는 함수
		 * @param g
		 */
		private void drawRect(Graphics g) {
			int px = Math.min(x1, x2);
			int py = Math.min(y1, y2);
			int pw = Math.abs(x1 - x2);
			int ph = Math.abs(y1 - y2);
			if(pw == 0 || ph == 0) {
				pw = ph = 0;
			}
			g.drawRect(px, py, pw, ph);
		}

		/**
		 * 자유곡선 선택 영역을 그리는 함수
		 * @param g
		 */
		private void drawLasso(Graphics g) {
			g.drawPolygon(lasso);
		}

		/**
		 * 좌표 값의 유효성을 체크하는 함수
		 * @param e
		 * @return 유효성 체크된 좌표 값 반환
		 */
		private Point validCoord(MouseEvent e){
			int x = e.getX();
			int y = e.getY();

			if(x < 0) {
				x = 0;
			} else if(x > image.getWidth() - 1) {
				x = image.getWidth() - 1;
			}

			if(y < 0) {
				y = 0;
			} else if(y > image.getHeight() - 1) {
				y = image.getHeight() - 1;
			}
			return new Point(x, y);
		}

		/**
		 * 이미지 객체의 복사본을 생성하는 함수
		 * @param img
		 * @return 복사된 이미지 객체 반환
		 */
		private BufferedImage imageCopy(BufferedImage img) {
			ColorModel cm = img.getColorModel();
			boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
			WritableRaster wr = img.copyData(null);
			return new BufferedImage(cm, wr, isAlphaPremultiplied, null);
		}

		/**
		 * 선택 모드를 설정하는 함수
		 * @param mode
		 */
		public void setMode(String mode) {
			this.mode = mode;
			x1 = y1 = x2 = y2 = 0;
			lasso.reset();
			repaint();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			parent.setStatusLabel(e.getX(), e.getY());
			Point p = validCoord(e);
			this.x2 = (int)p.getX();
			this.y2 = (int)p.getY();
			lasso.addPoint(x2, y2);
			repaint();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			parent.setStatusLabel(e.getX(), e.getY());
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			x1 = y1 = x2 = y2 = 0;
			lasso.reset();
			repaint();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			Point p = validCoord(e);
			this.x1 = (int)p.getX();
			this.y1 = (int)p.getY();
			lasso.reset();
			lasso.addPoint(x1, y1);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			Point p = validCoord(e);
			this.x2 = (int)p.getX();
			this.y2 = (int)p.getY();
			lasso.addPoint(x2, y2);
			repaint();
		}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}
	}
}
