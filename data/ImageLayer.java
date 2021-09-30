package org.openstreetmap.josm.plugins.indoorSpatialHelper.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.BuildingInfo.BuildingTransform;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

/**
 * 도면 이미지 레이어를 관리하는 클래스
 */
public class ImageLayer extends Layer {

	private static double INIT_IMG_SCALE = 1.0;
	private static double INIT_IMG_OPACITY = 0.8;

	private long osmPrimitiveId;
	private int floor;

	private Projection projection;

	private File imageFile;
	private BufferedImage image = null;
	private String tooltipText;

	private EastNorth initCenter;
	private EastNorth center;
	private AffineTransform affineTransform;

	private BBox bbox;

	private double initPixPerEn;

	private double correction;
	private int width;
	private int height;

	/**
	 * {@link ImageLayer} 생성자
	 */
	public ImageLayer() {
		super("IndoorSpatialImage");
	}

	/**
	 * {@link ImageLayer} 생성자
	 * @param file
	 * @param floor
	 * @param osmPrimitive
	 */
	public ImageLayer(File file, int floor, OsmPrimitive osmPrimitive) {
		this();
		this.osmPrimitiveId = osmPrimitive.getId();
		this.floor = floor;
		this.imageFile = file;
		this.initCenter = MainApplication.getMap().mapView.getCenter();
		this.center = this.initCenter;
		this.bbox = osmPrimitive.getBBox();

		projection = MainApplication.getMap().mapView.getProjection();
		tooltipText = imageFile.getAbsolutePath();
		setName(imageFile.getName());
		this.setOpacity(INIT_IMG_OPACITY);
	}

	/**
	 * {@link ImageLayer} 생성자
	 * @param image
	 * @param file
	 * @param floor
	 * @param osmPrimitive
	 */
	public ImageLayer(BufferedImage image, File file, int floor, OsmPrimitive osmPrimitive) {
		this(file, floor, osmPrimitive);
		this.image = image;
	}

	/**
	 * 이미지 레이어를 초기화하는 함수
	 * @param transform
	 * @throws IOException
	 */
	public void initialize(BuildingTransform transform) throws IOException {
		if (MainApplication.getMap() != null && MainApplication.getMap().mapView != null) {
			affineTransform = new AffineTransform();
			INIT_IMG_SCALE = MainApplication.getMap().mapView.getDist100Pixel();
		} else {
			throw new IOException(tr("Could not find the map."));
		}

		if (image == null) {
			image = createImage();
		}
		if (image == null) {
			throw new IOException(tr("Failed to import the image."));
		}
		
		int iW = image.getWidth(null);
		int iH = image.getHeight(null);
		
		initPixPerEn = getPixelPerEastNorth();

		Point min = MainApplication.getMap().mapView.getPoint(new LatLon(bbox.getBottomRightLat(), bbox.getTopLeftLon()));
		Point max = MainApplication.getMap().mapView.getPoint(new LatLon(bbox.getTopLeftLat(), bbox.getBottomRightLon()));

		double correctionW = Math.abs(min.getX() - max.getX()) / iW;//image.getWidth(null);
		double correctionH = Math.abs(min.getY() - max.getY()) / iH;//image.getHeight(null);

		correction = Math.min(correctionW, correctionH);

		width = (int)(image.getWidth(null) * correction);
		height = (int)(image.getHeight(null) * correction);

		if(null != transform){
			this.setCenter(transform.getCenter());
			this.resize(transform.getScale());
			this.rotate(transform.getRadian());
		}
	}

	/**
	 * 이미지 객체를 생성하는 함수
	 * @return 이미지 객체
	 * @throws IOException
	 */
	protected BufferedImage createImage() throws IOException {
		return ImageIO.read(imageFile);
	}

	@Override
	public void paint(Graphics2D g2, MapView mv, Bounds bounds) {
		if (image != null) {

			AffineTransform at = getCurrentAffineTransform();

			Graphics2D g = (Graphics2D) g2.create();

			g.translate(at.getTranslateX(), at.getTranslateY());
			g.scale(at.getScaleX(), at.getScaleY());


			g.transform(affineTransform);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

			/*
			// Draw picture
			int width = image.getWidth(null);
			int height = image.getHeight(null);
			try {
				g.drawImage(image, -width/2, -height/2, null);
			} catch (RuntimeException e) {
				Logging.error(e);
			}

			if (mv.getLayerManager().getActiveLayer() == this) {
				g.setColor(Color.RED);
				g.drawRect(-width/2, -height/2, width, height);
			}
			 */

			try {
				g.drawImage(image, -width/2, -height/2, width, height, null);
			} catch (RuntimeException e) {
				Logging.error(e);
			}

			if (mv.getLayerManager().getActiveLayer() == this) {
				g.setColor(Color.RED);
				g.drawRect(-width/2, -height/2, width, height);
			}


		}
	}

	/**
	 * 이미지 레이어의 AffineTransform 객체를 반환하는 함수
	 * @return AffineTransform 객체를 반환
	 */
	public AffineTransform getAffineTransform() {
		return this.affineTransform;
	}

	/**
	 * 이미지 레이어의 AffineTransform 객체를 반환하는 함수
	 * @return AffineTransform 객체를 반환
	 */
	public AffineTransform getCurrentAffineTransform() {

		EastNorth leftop = MainApplication.getMap().mapView.getEastNorth(0, 0);

		double pixPerEn = getPixelPerEastNorth();

		EastNorth imageCenter = this.center;

		double pic_offset_x = ((imageCenter.east() - leftop.east()) * pixPerEn);
		double pic_offset_y = ((leftop.north() - imageCenter.north()) * pixPerEn);

		AffineTransform at = AffineTransform.getTranslateInstance(pic_offset_x, pic_offset_y);

		double scalex = INIT_IMG_SCALE * pixPerEn / getMetersPerEasting(imageCenter) / 100;
		double scaley = INIT_IMG_SCALE * pixPerEn / getMetersPerNorthing(imageCenter) / 100;
		at.scale(scalex, scaley);

		return at;
	}

	/**
	 * 이미지 레이어의 Osm Primitive 데이터의 키를 반환하는 함수
	 * @return Osm Primitive 데이터의 키 반환
	 */
	public long getOsmPrimitiveId() {
		return osmPrimitiveId;
	}

	/**
	 * 이미지 레이어의 층 정보를 반환하는 함수
	 * @return 층 정보 반환
	 */
	public int getFloor() {
		return floor;
	}

	/**
	 * 실제 좌표 값 대비 화면의 픽셀 좌표 값의 비율 반환하는 함수
	 * @return 실제 좌표 값 대비 화면의 픽셀 좌표 값의 비율  반환
	 */
	private double getPixelPerEastNorth() {
		EastNorth center = MainApplication.getMap().mapView.getCenter();
		EastNorth leftop = MainApplication.getMap().mapView.getEastNorth(0, 0);
		return (MainApplication.getMap().mapView.getWidth()/2.0) / (center.east() - leftop.east());
	}

	/**
	 * 초기 이미지 레이어의 실제 좌표 값 대비 화면의 픽셀 좌표 값의 비율 반환하는 함수
	 * @return 초기 이미지 레이어의 실제 좌표 값 대비 화면의 픽셀 좌표 값의 비율 반환
	 */
	public double getInitPixelPerEastNorth() {
		return this.initPixPerEn;
	}

	/**
	 * 이미지의 픽셀 좌표 값 대비 실제 좌표 값의 비율을 반환하는 함수
	 * @return 이미지의 픽셀 좌표 값 대비 실제 좌표 값의 비율을 반환
	 */
	public double getCorrection() {
		return this.correction;
	}

	/**
	 * AffineTransform 정보가 적용된 Point 정보를 반환하는 함수
	 * @param p
	 * @return
	 */
	public Point2D transformPoint(Point p) {

		AffineTransform at = getCurrentAffineTransform();
		at.concatenate(affineTransform);

		Point2D result = null;
		try {
			result = at.inverseTransform(p, null);
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public Icon getIcon() {
		return Constants.IMAGE_LAYER_ICON;
	}

	@Override
	public String getToolTipText() {
		return tooltipText;
	}

	@Override
	public void mergeFrom(Layer from) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isMergable(Layer other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void visitBoundingBox(BoundingXYVisitor v) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getInfoComponent() {
		final JPanel p = new JPanel(new GridBagLayout());
		try{
			String filePath = tr("File path : {0}", imageFile.getCanonicalPath());
			String fileSize = tr("File size : {0} bytes", imageFile.length());
			String resolution = tr("Resolution : {0}*{0}", image.getWidth(), image.getHeight());

			p.add(new JLabel(tr("Image file information", getName())), GBC.eol());
			p.add(new JLabel(filePath, null, JLabel.HORIZONTAL), GBC.eop().insets(15, 0, 0, 0));
			p.add(new JLabel(fileSize, null, JLabel.HORIZONTAL), GBC.eop().insets(15, 0, 0, 0));
			p.add(new JLabel(resolution, null, JLabel.HORIZONTAL), GBC.eop().insets(15, 0, 0, 0));
		}catch(IOException e){
			e.printStackTrace();
		}

		return p;
	}

	@Override
	public Action[] getMenuEntries() {
		List<Action> actions = new ArrayList<>();
		actions.add(LayerListDialog.getInstance().createActivateLayerAction(this));
		actions.add(LayerListDialog.getInstance().createShowHideLayerAction());
		actions.add(LayerListDialog.getInstance().createDeleteLayerAction());
		actions.add(SeparatorLayerAction.INSTANCE);
		actions.add(new RenameLayerAction(getAssociatedFile(), this));
		actions.add(SeparatorLayerAction.INSTANCE);
		actions.add(new LayerListPopup.InfoAction(this));
		return actions.toArray(new Action[0]);
	}

	/**
	 * 이미지 레이어의 센터 정보를 반환하는 함수
	 * @return 센터 정보 반환
	 */
	public EastNorth getCenter() {
		return center;
	}

	/**
	 * 이미지 레이어의 센터 정보를 설정하는 함수
	 * @param center
	 */
	public void setCenter(EastNorth center) {
		this.center = center;
	}

	/**
	 * 이미지 레이어의 초기 센터 정보를 반환하는 함수
	 * @return
	 */
	public EastNorth getInitCenter() {
		return initCenter;
	}

	/**
	 * 이미지 레이어의 도면 이미지 스케일을 변경하는 함수
	 * @param scale
	 */
	public void resize(double scale) {
		Point2D trans = transformPoint(MainApplication.getMap().mapView.getPoint(center));
		concatenate(AffineTransform.getScaleInstance(scale, scale), trans);
	}

	/**
	 * 이미지 레이어의 도면 이미지를 회전시키는 함수
	 * @param radian
	 */
	public void rotate(double radian) {
		Point2D trans = transformPoint(MainApplication.getMap().mapView.getPoint(center));
		concatenate(AffineTransform.getRotateInstance(radian), trans);
	}

	/**
	 * 이미지 레이어의 AffineTransform 객체 정보를 설정하는 함수
	 * @param at
	 * @param point
	 */
	public void concatenate(AffineTransform at, Point2D point) {

		if (point != null) {
			AffineTransform centered = AffineTransform.getTranslateInstance(point.getX(), point.getY());
			centered.concatenate(at);
			centered.translate(-point.getX(), -point.getY());
			affineTransform.concatenate(centered);
		} else {
			affineTransform.concatenate(at);
		}
	}

	/**
	 * Returns the distance in meter, that corresponds to one unit in east north space.
	 * For normal projections, it is about 1 (but usually changing with latitude).
	 * For EPSG:4326, it is the distance from one meridian of full degree to the next (a couple of kilometers).
	 * @param en east/north
	 * @return the distance in meter, that corresponds to one unit in east north space
	 */
	protected double getMetersPerEasting(EastNorth en) {
		/* Natural scale in east/north units per pixel.
		 * This means, the projection should be able to handle
		 * a shift of that size in east north space without
		 * going out of bounds.
		 *
		 * Also, this should get us somewhere in the range of meters,
		 * so we get the result at the point 'en' and not some average.
		 */
		double naturalScale = projection.getDefaultZoomInPPD() * 0.01;

		LatLon ll1 = projection.eastNorth2latlon(new EastNorth(en.east() - naturalScale, en.north()));
		LatLon ll2 = projection.eastNorth2latlon(new EastNorth(en.east() + naturalScale, en.north()));

		double dist = ll1.greatCircleDistance(ll2) / naturalScale / 2;
		return dist;
	}

	/* see getMetersPerEasting */
	private double getMetersPerNorthing(EastNorth en) {
		double naturalScale = projection.getDefaultZoomInPPD() * 0.01;

		LatLon ll1 = projection.eastNorth2latlon(new EastNorth(en.east(), en.north()- naturalScale));
		LatLon ll2 = projection.eastNorth2latlon(new EastNorth(en.east(), en.north() + naturalScale));

		double dist = ll1.greatCircleDistance(ll2) / naturalScale / 2;
		return dist;
	}

	/**
	 * 이미지 레이어의 이미지 객체를 반환하는 함수
	 * @return 이미지 객체 반환
	 */
	public BufferedImage getImage() {
		return image;
	}

	/**
	 * 이미지 레이어의 이미지 파일 객체를 반환하는 함수
	 * @return 이미지 파일 객체 반환
	 */
	public File getImageFile() {
		return imageFile;
	}

	public int getSelectedLevel() {
		//return this.buildingInfo.getSelectedLevel();
		return 0;
	}
}
