package org.openstreetmap.josm.plugins.indoorSpatialHelper.modules;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.ImageLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.IndoorCommand;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.LayerManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.Simplifier;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.AbstractIndoorObject;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;
import org.openstreetmap.josm.tools.I18n;
import org.xml.sax.SAXException;

import com.kitfox.svg.Group;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.ShapeElement;

/**
 * 벡터라이징 모듈 실행을 통해 생성된 SVG파일을 MapFrame에 추가하는 쓰레드 클래스
 */
public class SVGImportTask extends PleaseWaitRunnable {

	private String svgFilePath;

	private int buildingLevel;
	private VectorLayer vectorLayer;

	private LinkedList<Way> ways = new LinkedList<>();
	private Way way;

	private final double epsilon = 1.0E-6;

	private EastNorth center;
	private double radian;
	private double scale;
	private double lastX;
	private double lastY;
	private Rectangle2D bbox;

	private static final double CURVE_STEPS = 4;

	private boolean canceled;

	/**
	 * {@link SVGImportTask} 생성자
	 * @param svgFilePath
	 * @param imageLayer
	 * @param scale
	 * @param radian
	 */
	public SVGImportTask(String svgFilePath, ImageLayer imageLayer, double scale, double radian){
		super(I18n.tr("Import Vector Layer..."), false);
		this.svgFilePath = svgFilePath;
		this.center = imageLayer.getCenter();
		//this.scale = (1 / imageLayer.getInitPixelPerEastNorth()) * imageLayer.getAffineTransform().getScaleX();
		this.scale = (1 / imageLayer.getInitPixelPerEastNorth()) * imageLayer.getCorrection() * scale;
		this.radian = radian;
		this.buildingLevel = imageLayer.getSelectedLevel();
	}

	@Override
	protected void cancel() {
		this.canceled = true;
	}

	@Override
	protected void realRun() throws SAXException, IOException, OsmTransferException {
		try {
			SVGUniverse universe = new SVGUniverse();
			universe.setVerbose(false);

			File svgFile = new File(svgFilePath);
			if(!svgFile.exists()){
				return;
			}
			SVGDiagram diagram = universe.getDiagram(svgFile.toURI());
			ShapeElement root = diagram.getRoot();
			if (root == null) {
				throw new IOException("Can't find root SVG element");
			}
			bbox = root.getBoundingBox();
			this.center = this.center.add(-bbox.getCenterX() * scale, bbox.getCenterY() * scale);

			AffineTransform at = new AffineTransform();
			at.rotate(radian, bbox.getCenterX(), bbox.getCenterY());

			processElement(root, at);

			if(canceled){
				return;
			}

		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}

		if(canceled){
			return;
		}

		vectorLayer = LayerManager.getInstance().addVectorLayer();
		DataSet ds = vectorLayer.getDataSet();
		Collection<Command> cmds = new LinkedList<Command>();
		for (Way way : ways) {
			List<Node> nodes = way.getNodes();
			for (Node node : nodes) {
				cmds.add(new IndoorCommand(ds, node));
			}
			way.setKeys(AbstractIndoorObject.getTagMap(Constants.ETC_CLASS_CODE[0]));
			cmds.add(new IndoorCommand(ds, way));
		}

		UndoRedoHandler.getInstance().add(new SequenceCommand("Vector Import", cmds));
		vectorLayer.initDataSet();
		//LayerManager.removeImageLayer();
	}

	@Override
	protected void finish() {
		File svgFile = new File(svgFilePath);
		if(svgFile.exists()){
			svgFile.delete();
		}
	}

	/**
	 * SVG 파일을 기반으로 Way 객체 리스트를 생성하는 함수
	 * @param el
	 * @param transform
	 * @throws IOException
	 */
	private void processElement(SVGElement el, AffineTransform transform) throws IOException {
		if (el instanceof Group) {
			AffineTransform oldTransform = transform;
			AffineTransform xform = ((Group) el).getXForm();
			if (transform == null) {
				transform = xform;
			} else if (xform != null) {
				transform = new AffineTransform(transform);
				transform.concatenate(xform);
			}
			for (Object child : ((Group) el).getChildren(null)) {
				if(canceled){
					return;
				}
				processElement((SVGElement) child, transform);
			}
			transform = oldTransform;
		} else if (el instanceof ShapeElement) {
			Shape shape = ((ShapeElement) el).getShape();

			if (transform != null) {
				shape = transform.createTransformedShape(shape);
			}

			PathIterator it = shape.getPathIterator(null);
			while (!it.isDone()) {
				if(canceled){
					return;
				}

				double[] coords = new double[6];
				switch (it.currentSegment(coords)) {
				case PathIterator.SEG_MOVETO:
					way = new Way(++Constants.TEMP_OSM_ID, Constants.TEMP_OSM_VERSION);
					addNode(coords[0], coords[1]);
					break;
				case PathIterator.SEG_LINETO:
					addNode(coords[0], coords[1]);
					break;
				case PathIterator.SEG_CLOSE:
					Simplifier simplifier = new Simplifier();
					List<Node> nodes = way.getNodes();
					if(nodes.size() > 4){
						if(way.firstNode().getCoor().distance(way.lastNode().getCoor()) == 0) {
							nodes = simplifier.simplify(nodes, epsilon, true);
						}else{
							nodes = simplifier.simplify(nodes, epsilon);
						}
						way.setNodes(nodes);
					}
					ways.add(way);
					break;
				case PathIterator.SEG_QUADTO:
					for (int i=1; i<CURVE_STEPS; i++) {
						addNode(lastX, lastY, coords[0], coords[1], coords[2], coords[3], i/CURVE_STEPS);
					}
					addNode(coords[2], coords[3]);
					break;
				case PathIterator.SEG_CUBICTO:
					for (int i=1; i<CURVE_STEPS; i++) {
						addNode(lastX, lastY, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5], i/CURVE_STEPS);
					}
					addNode(coords[4], coords[5]);
					break;
				}
				it.next();
			}
		}
	}

	/**
	 * 실제 좌표 값이 적용된 Node 객체를 추가하는 함수
	 * @param x
	 * @param y
	 * @throws IOException
	 */
	private void addNode(double x, double y) throws IOException {
		Node node = new Node(center.add(x * scale, -y * scale));
		way.addNode(node);
		//nodes.add(node);
		lastX = x;
		lastY = y;
	}

	/**
	 * Quadratic Interpolation
	 * @param ax
	 * @param ay
	 * @param bx
	 * @param by
	 * @param cx
	 * @param cy
	 * @param t
	 * @throws IOException
	 */
	private void addNode(double ax, double ay, double bx, double by, double cx, double cy, double t) throws IOException {
		double x = (Math.pow((1 - t), 2) * ax) + (2 * (1 - t) * t * bx) + (Math.pow(t, 2) * cx);
		double y = (Math.pow((1 - t), 2) * ay) + (2 * (1 - t) * t * by) + (Math.pow(t, 2) * cy);
		addNode(x, y);
	}

	/**
	 * Cubic Interpolation
	 * @param ax
	 * @param ay
	 * @param bx
	 * @param by
	 * @param cx
	 * @param cy
	 * @param dx
	 * @param dy
	 * @param t
	 * @throws IOException
	 */
	private void addNode(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy, double t) throws IOException {
		double x = Math.pow((1 - t), 3) * ax + 3 * Math.pow((1 - t), 2) * t * bx + 3 * (1 - t) * t * t * cx + t * t * t * dx;
		double y = Math.pow((1 - t), 2) * ay + 3 * Math.pow((1 - t), 2) * t * by + 3 * (1 - t) * t * t * cy + t * t * t * dy;
		addNode(x, y);
	}
}
