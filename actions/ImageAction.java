package org.openstreetmap.josm.plugins.indoorSpatialHelper.actions;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.ImageLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

/**
 *
 * 실내공간정보 구축을 위한 이미지 레이어 컨트롤 액션 클래스
 */
public class ImageAction extends JosmAction implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = -3242005325187490527L;

	private ImageLayer imageLayer;

	private final Cursor cursor;
	private String name;

	private boolean isFocus = false;
	private EastNorth oldEN;

	private double scale = 1;
	private double radian = 0;

	/**
	 * {@link ImageAction} 생서자
	 * @see JosmAction#JosmAction()
	 */
	public ImageAction(String name, Cursor cursor) {
		super();
		putValue("active", Boolean.FALSE);
		this.name = name;
		this.cursor = cursor;
	}

	/**
	 * 이미지 컨트롤 액션 활성화
	 */
	public void enterMode() {
		putValue("active", Boolean.TRUE);
		MainApplication.getMap().mapView.setNewCursor(cursor, this);
		MainApplication.getMap().mapView.addMouseListener(this);
		MainApplication.getMap().mapView.addMouseMotionListener(this);
		setStatusLine();
	}

	/**
	 * 이미지 컨트롤 액션 비활성화
	 */
	public void exitMode() {
		putValue("active", Boolean.FALSE);
		MainApplication.getMap().mapView.resetCursor(this);
		MainApplication.getMap().mapView.removeMouseListener(this);
		MainApplication.getMap().mapView.removeMouseMotionListener(this);
		MainApplication.getMap().statusLine.setHelpText("");
	}

	/**
	 * 상태 도움말 세팅
	 */
	protected void setStatusLine() {
		MapFrame map = MainApplication.getMap();
		if (map != null && map.statusLine != null) {
			map.statusLine.setHelpText(getActionHelpText());
			map.statusLine.repaint();
		}
	}

	/**
	 * 액션 종류별 상태 도움말 반환
	 * @return 액션별 상태 도움말 반환
	 */
	private String getActionHelpText() {
		String helpText = "";
		switch(name){
		case "MoveAction" :
			helpText = "You can move the image by dragging the mouse.";
			break;
		case "ResizeAction" :
			helpText = "You can change the image size by dragging the mouse.";
			break;
		case "RotateAction" :
			helpText = "You can rotate the image by dragging the mouse.";
			break;
		}
		return helpText;
	}

	/**
	 * 이미지 레이어 이동 컨트롤
	 * @param e
	 */
	public void move(MouseEvent e) {
		EastNorth newEN = MainApplication.getMap().mapView.getEastNorth(e.getX(), e.getY());
		double moveX = newEN.east() - oldEN.east();
		double moveY = newEN.north() - oldEN.north();

		imageLayer.setCenter(imageLayer.getCenter().add(moveX, moveY));

		this.oldEN = newEN;
		imageLayer.invalidate();
	}

	/**
	 * 이미지 레이어 리사이즈 컨트롤
	 * @param e
	 */
	public void resize(MouseEvent e) {
		//double centerX = imageLayer.getCenter().getX();
		//double centerY = imageLayer.getCenter().getY();
		EastNorth newEN = MainApplication.getMap().mapView.getEastNorth(e.getX(), e.getY());

		//double oldDistance = Math.max(oldEN.distance(centerX, centerY), 10);
		//double newDistance = newEN.distance(centerX, centerY);
		//double scale = Math.max(newDistance / oldDistance, 0.9);

		Point pCenter = MainApplication.getMap().mapView.getPoint(imageLayer.getCenter());
		double oldDistance = Math.max(MainApplication.getMap().mapView.getPoint(oldEN).distance(pCenter.getX(), pCenter.getY()), 10);
		double newDistance = MainApplication.getMap().mapView.getPoint(newEN).distance(pCenter.getX(), pCenter.getY());
		double scale = Math.max(newDistance / oldDistance, 0.9);

		imageLayer.resize(scale);
		this.scale *= scale;
		this.oldEN = newEN;
		imageLayer.invalidate();
	}

	/**
	 * 이미지 레이어 회전 컨트롤
	 * @param e
	 */
	public void rotate(MouseEvent e) {
		double centerX = imageLayer.getCenter().getX();
		double centerY = imageLayer.getCenter().getY();
		EastNorth newEN = MainApplication.getMap().mapView.getEastNorth(e.getX(), e.getY());

		double oldAlpha = Math.atan2(oldEN.getY() - centerY, oldEN.getX() - centerX);
		double newAlpha = Math.atan2(newEN.getY() - centerY, newEN.getX() - centerX);
		double radian = oldAlpha - newAlpha;

		imageLayer.rotate(radian);

		this.radian += radian;
		this.oldEN = newEN;
		imageLayer.invalidate();
	}

	@Override
	public void actionPerformed(ActionEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) {
		if(isFocus){
			switch(name){
			case "MoveAction" :
				move(e);
				break;
			case "ResizeAction" :
				resize(e);
				break;
			case "RotateAction" :
				rotate(e);
				break;
			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		if (MainApplication.getLayerManager().getActiveLayer() instanceof ImageLayer) {
			imageLayer = (ImageLayer) MainApplication.getLayerManager().getActiveLayer();

			if (e.getButton() == MouseEvent.BUTTON1) {
				isFocus = true;
				if (isEnabled()) {
					MainApplication.getMap().mapView.requestFocus();
				}
				oldEN = MainApplication.getMap().mapView.getEastNorth(e.getX(), e.getY());
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		isFocus = false;
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	/**
	 * The function returns mouse cursor to move
	 * @return Returns mouse cursor to move
	 */
	public static Cursor getMoveCursor(){
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		return toolkit.createCustomCursor(Constants.MOVE_CURSOR, new Point(16,16), "move");
	}

	/**
	 * The function returns resize mouse cursor
	 * @return Returns resize mouse cursor
	 */
	public static Cursor getResizeCursor(){
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		return toolkit.createCustomCursor(Constants.RESIZE_CURSOR, new Point(16,16), "resize");
	}

	/**
	 * The function returns rotating mouse cursor
	 * @return Returns rotating mouse cursor
	 */
	public static Cursor getRotateCursor(){
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		return toolkit.createCustomCursor(Constants.ROTATE_CURSOR, new Point(16,16), "rotate");
	}

	/**
	 * 리사이즈에 의한 스케일 값을 설정하는 함수
	 * @param scale
	 */
	public void setScale(double scale) {
		this.scale = scale;
	}

	/**
	 * 리사이즈에 의한 스케일 값을 반환하는 함수
	 * @return 스케일 값을 반환
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * 회전에 의한 라디안 값을 설정하는 함수
	 * @param radian
	 */
	public void setRadian(double radian) {
		this.radian = radian;
	}

	/**
	 * 회전에 의한 라디안 값을 반환하는 함수
	 * @return 라디안 값을 반환
	 */
	public double getRadian() {
		return radian;
	}
}
