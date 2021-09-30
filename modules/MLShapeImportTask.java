package org.openstreetmap.josm.plugins.indoorSpatialHelper.modules;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileException;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.shapefile.shp.ShapefileReader.Record;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.ImageLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.IndoorCommand;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.LayerManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.MLRunner;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.MLRunnerAbstract;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.TransformInfo;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.AbstractIndoorObject;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.Door;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.Lift;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.Space;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.Stair;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.Subspace;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.Wall;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.Window;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.CommonUtil;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;
import org.openstreetmap.josm.tools.I18n;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * ML 모듈 실행을 통해 생성된 Shp파일을 MapFrame에 추가하는 쓰레드 클래스
 * shape file 속성은 Dbf의 내용 모두 Import하므로 2020년 ML Module 적용 시 변경 부분 없음.
 */
public class MLShapeImportTask extends PleaseWaitRunnable {

	private VectorLayer vectorLayer;

	private TransformInfo transform;

	private double imageX;
	private double imageY;
	private EastNorth center;
	private double radian;
	private double scale;

	private boolean canceled;
	private String shpFilePath;
	private boolean ml2020 = false;

	ArrayList<Geometry> geometries = new ArrayList<>();
	ArrayList<Map<String, String>> entries = new ArrayList<>();

	private String[][] shpFiles = {{"stair_w_label.shp", Constants.STAIR_CLASS_CODE[0]},
			{"lift_w_label.shp", Constants.LIFT_CLASS_CODE[0]},
			{"space_w_label.shp", Constants.SPACE_CLASS_CODE[0]},
			{"door.shp", Constants.DOOR_CLASS_CODE[0]},
			{"newDoor.shp", Constants.DOOR_CLASS_CODE[0]},
			{"line.shp", Constants.WALL_CLASS_CODE[0]},
			{"window.shp", Constants.WINDOW_CLASS_CODE[0]}};

	private String[][] shpFiles2020 = {{"stair_polygons.shp", Constants.STAIR_CLASS_CODE[0]},
			{"lift_polygons.shp", Constants.LIFT_CLASS_CODE[0]},
			{"new_polys.shp", Constants.SPACE_CLASS_CODE[0]},
			{"door_line.shp", Constants.DOOR_CLASS_CODE[0]},
			{"wall.shp", Constants.WALL_CLASS_CODE[0]},
//			{"new_wall.shp", Constants.WALL_CLASS_CODE[0]},
			{"window_line.shp", Constants.WINDOW_CLASS_CODE[0]}};
	/**
	 * {@link MLShapeImportTask} 생성자
	 * @param runner
	 * @param imageLayer
	 * @param scale
	 * @param radian
	 */
	public MLShapeImportTask(MLRunnerAbstract runner, ImageLayer imageLayer, double scale, double radian, boolean ml2020) {
		super(I18n.tr("Import Shape file..."), false);
		//this.transform = new TransformInfo(imageLayer.getImage().getWidth(), imageLayer.getImage().getHeight(), 1024, 724);
		this.shpFilePath = runner.getShpFilePath();
		this.transform = runner.getTransformInfo();

		this.imageX = ((imageLayer.getImage().getWidth() * imageLayer.getCorrection())/2)/imageLayer.getInitPixelPerEastNorth();
		this.imageY = ((imageLayer.getImage().getHeight() * imageLayer.getCorrection())/2)/imageLayer.getInitPixelPerEastNorth();

		this.scale = (1 / imageLayer.getInitPixelPerEastNorth()) * imageLayer.getCorrection() * scale;
		this.radian = radian;
		this.center = imageLayer.getCenter().add(-imageX * scale, imageY * scale);
		this.ml2020 = ml2020;
	}

	@Override
	protected void cancel() {
		this.canceled = true;
	}

	@Override
	protected void realRun() throws SAXException, IOException, OsmTransferException {

		//ArrayList<Geometry> geometries = new ArrayList<>();
		//ArrayList<Map<String, String>> entries = new ArrayList<>();

		try {
			File dir = new File(shpFilePath);

			String[][] shpFilesTarget = null;
			
			if(ml2020)
				shpFilesTarget = shpFiles2020;
			else
				shpFilesTarget = shpFiles;

			if(dir.exists() && dir.isDirectory()){
				for(File f : dir.listFiles()){
					String name = f.getName().toLowerCase();
					if(f.isFile() && name.endsWith("shp")){
						String cls = "99";
						for(String[] shp : shpFilesTarget){
							if(name.endsWith(shp[0])){
								cls = shp[1];
								break;
							}
						}
						getShapeData(f.getCanonicalPath(), cls, true);
						//geometries.addAll(getGeometries(f.getCanonicalPath()));
						//entries.addAll(getEntries(f.getCanonicalPath(), cls));
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			return;
		} catch(Throwable t){
			t.printStackTrace();
		}
		
		if(geometries.size() <= 0) {
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("There is no shp file."));
			return;
		}

		if(geometries.size() != entries.size()) {
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Shp file and Dbf file are not matching"));
			return;
		}

		//if(transform.isNeedTransForm()) {
		if(true) {
			try {
				geometries = GeometriesToOrginal(geometries);
			} catch(Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Fail to transform coordinates of Shp file"));
				return;
			}
		}
		
		AffineTransform affineTransform = AffineTransform.getRotateInstance(radian, transform.getOriginalW()/2, transform.getOriginalH()/2);
		MathTransform mathTransform = new AffineTransform2D(affineTransform);

		if(canceled){
			return;
		}

		vectorLayer = LayerManager.getInstance().addVectorLayer();
		DataSet ds = vectorLayer.getDataSet();
		List<Command> cmds = new ArrayList<Command>();

		for(int i=0; i<geometries.size(); i++) {
			Geometry geometry = geometries.get(i);
			if(null != geometry){
				for(int j=0; j<geometry.getNumGeometries(); j++){
					List<Way> ways = new ArrayList<>();

					Geometry g;
					try {
//						Geometry gt = geometry.getGeometryN(j);
//						g = JTS.transform(gt, mathTransform);
						g = JTS.transform(geometry.getGeometryN(j), mathTransform);
					} catch (MismatchedDimensionException | TransformException e) {
						e.printStackTrace();
						JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Fail to transform coordinates of Shp file"));
						return;
					}

					if(g instanceof Polygon){
						Way way = new Way(++Constants.TEMP_OSM_ID, Constants.TEMP_OSM_VERSION);

						Polygon polygon = (Polygon)g;
						Coordinate[] coords = polygon.getExteriorRing().getCoordinates();
						for(int k=0; k<coords.length; k++) {
							way.addNode(getNode(coords[k].x, coords[k].y));
						}
						ways.add(way);

						for(int k=0; k<polygon.getNumInteriorRing(); k++){
							way = new Way(++Constants.TEMP_OSM_ID, Constants.TEMP_OSM_VERSION);

							coords = polygon.getInteriorRingN(k).getCoordinates();
							for(int l=0; l<coords.length; l++){
								way.addNode(getNode(coords[l].x, coords[l].y));
							}
							ways.add(way);
						}

					}else if(g instanceof MultiPolygon){
						MultiPolygon multiPolygon = (MultiPolygon)g;
						for(int k=0; k<multiPolygon.getNumGeometries(); k++) {
							Way way = new Way(++Constants.TEMP_OSM_ID, Constants.TEMP_OSM_VERSION);
							Polygon polygon = (Polygon)multiPolygon.getGeometryN(k);

							Coordinate[] coords = polygon.getExteriorRing().getCoordinates();
							for(int l=0; l<coords.length; l++){
								way.addNode(getNode(coords[l].x, coords[l].y));
							}
							ways.add(way);

							for(int l=0; l<polygon.getNumInteriorRing(); l++){
								way = new Way(++Constants.TEMP_OSM_ID, Constants.TEMP_OSM_VERSION);

								coords = polygon.getInteriorRingN(l).getCoordinates();
								for(int m=0; m<coords.length; m++){
									way.addNode(getNode(coords[m].x, coords[m].y));
								}
								ways.add(way);
							}
						}
					}else{
						Way way = new Way(++Constants.TEMP_OSM_ID, Constants.TEMP_OSM_VERSION);

						Coordinate[] coords = g.getCoordinates();
						for(int k=0; k<coords.length; k++) {
							way.addNode(getNode(coords[k].x, coords[k].y));
						}
						ways.add(way);
					}

					for(Way way : ways){
						List<Node> nodes = way.getNodes();
						for (Node node : nodes) {
							cmds.add(new IndoorCommand(ds, node));
						}
						way.setKeys(entries.get(i));
						cmds.add(new IndoorCommand(ds, way));
					}
				}

			}
		}
		UndoRedoHandler.getInstance().add(new SequenceCommand("Vector Import", cmds));
		vectorLayer.initDataSet();
		//removeImageLayer();
	}

	@Override
	protected void finish() {}

	/**
	 * Shp 및 Dbf 파일을 읽어 오는 함수
	 * @param FilePath
	 * @param cls
	 * @param deduplicate ML모듈 실행시 중복 데이터가 다수 생성 되어 true로 처리하여 중복을 제거할 수 있음
	 * @throws Exception
	 */
	private void getShapeData(String FilePath, String cls, boolean deduplicate) throws Exception {

		ArrayList<Geometry> geoms = new ArrayList<Geometry>();
		ArrayList<Map<String, String>> entry = new ArrayList<Map<String, String>>();

		ShapefileReader sReader = null;
		DbaseFileReader dReader = null;
		try{
			ShpFiles shpFile = new ShpFiles(FilePath);

			GeometryFactory geometryFactory = new GeometryFactory();
			sReader = new ShapefileReader(shpFile, true, false, geometryFactory);
			//dReader = new DbaseFileReader(shpFile, false, Charset.defaultCharset());
			dReader = new DbaseFileReader(shpFile, false, Charset.forName("euc-kr"));

			DbaseFileHeader header = dReader.getHeader();

			String[] attrs = AbstractIndoorObject.getAttributes(cls);
			
			while(sReader.hasNext()){
				try{
					Record record = sReader.nextRecord();
					Object[] values = dReader.readEntry();

					Geometry geom = (Geometry)record.shape();
					boolean isDup = false;
					if(deduplicate){
						for(Geometry g : geoms){
							if(g.equals(geom)){
								isDup = true;
								break;
							}
						}
						if(isDup){
							continue;
						}
					}
					geoms.add(geom);

					Map<String, String> map = new HashMap<String, String>();
					map.put(Constants.INDOOR_OBJECT_KEY, cls);
					for(String attr : attrs){
						String value = "";
						for(int i=0; i<header.getNumFields(); i++) {
							if(attr.indexOf(header.getFieldName(i).toLowerCase()) == 0){
								if(null != values[i]){
									value = String.valueOf(values[i]);
								}
								break;
							}
						}
						map.put(attr, value);
					}

					// Set IndoorGML Attribute Value
					if(Constants.STAIR_CLASS_CODE[0].equals(cls)){
						map.put("indoorgml_class", "1010");
						map.put("indoorgml_function", "1120");
						map.put("indoorgml_usage", "1120");
					}else if(Constants.LIFT_CLASS_CODE[0].equals(cls)){
						map.put("indoorgml_class", "1010");
						map.put("indoorgml_function", "1110");
						map.put("indoorgml_usage", "1110");
					}

					entry.add(map);

				}catch(NullPointerException e){
					e.printStackTrace();
				}
			}
		}catch(MalformedURLException e){
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Failed to find Shapefile"));
			throw e;
		}catch(ShapefileException e){
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Shapefile contains ivalid records"));
			throw e;
		}catch(IOException e){
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Failed to read Shapefile"));
			throw e;
		}catch(Throwable t){
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr(t.getMessage()));
			throw t;
		}finally{
			if(null != sReader){try{sReader.close();}catch(IOException e){}}
			if(null != dReader){try{dReader.close();}catch(IOException e){}}
		}
		geometries.addAll(geoms);
		entries.addAll(entry);
	}

	/**
	 * Shp 파일을 읽어 geometry를 반환하는 함수
	 * @param FilePath
	 * @return geometry 리스트 반환
	 * @throws Exception
	 */
	@Deprecated
	private ArrayList<Geometry> getGeometries(String FilePath) throws Exception {

		ArrayList<Geometry> geometies = new ArrayList<Geometry>();

		ShapefileReader reader = null;
		try {
			ShpFiles shpFile = new ShpFiles(FilePath);

			GeometryFactory geometryFactory = new GeometryFactory();
			reader = new ShapefileReader(shpFile, true, false, geometryFactory);

			while (reader.hasNext()) {
				Record record = reader.nextRecord();
				try{
					Geometry geom = (Geometry)record.shape();
					geometies.add(geom);
				}catch(NullPointerException e){
					e.printStackTrace();
					geometies.add(null);
				}
			}
		} catch (MalformedURLException e) {
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Failed to find Shapefile"));
			throw e;
		} catch (ShapefileException e) {
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Shapefile contains ivalid records"));
			throw e;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Failed to read Shapefile"));
			throw e;
		} catch(Throwable t){
			t.printStackTrace();
			throw t;
		} finally {
			if(null != reader){
				try { reader.close(); }catch(IOException e){}
			}
		}
		return geometies;
	}

	/**
	 * Dbf 파일을 읽어 속성정보를 반환하는 함수
	 * @param FilePath
	 * @param cls
	 * @return 속성정보 리스트 반환
	 * @throws Exception
	 */
	@Deprecated
	private ArrayList<Map<String, String>> getEntries(String FilePath, String cls) throws Exception {

		ArrayList<Map<String, String>> entries = new ArrayList<Map<String, String>>();

		DbaseFileReader reader = null;
		try {
			ShpFiles shpFile = new ShpFiles(FilePath);
			reader = new DbaseFileReader(shpFile, false, Charset.defaultCharset());
			DbaseFileHeader header = reader.getHeader();

			String[] attrs = AbstractIndoorObject.getAttributes(cls);

			while (reader.hasNext()) {
				Object[] values = reader.readEntry();

				Map<String, String> map = new HashMap<String, String>();
				map.put(Constants.INDOOR_OBJECT_KEY, cls);
				for(String attr : attrs){
					String value = "";
					for(int i=0; i<header.getNumFields(); i++) {
						if(attr.indexOf(header.getFieldName(i).toLowerCase()) == 0){
							value = String.valueOf(values[i]);
							break;
						}
					}
					map.put(attr, value);
				}
				entries.add(map);
			}
		} catch (MalformedURLException e) {
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Failed to find Shapefile"));
			throw e;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Failed to read Dbffile"));
			throw e;
		} finally {
			if(null != reader){
				try { reader.close(); }catch(IOException e){}
			}
		}
		return entries;
	}

	/**
	 * ML 모듈 실행시 강제 변환된 사이즈를 기존 이미지 사이즈 및 좌표로 되돌리는 함수
	 * @param geometries
	 * @return 원본 이미지 해상도로 좌표가 변환된 geometry 리스트 반환
	 * @throws Exception
	 */
	private ArrayList<Geometry> GeometriesToOrginal(ArrayList<Geometry> geometries) throws Exception {
		ArrayList<Geometry> result = new ArrayList<Geometry>();
		for(int i=0; i<geometries.size(); i++) {
			result.add(GeometryToOrginal(geometries.get(i)));
		}
		return result;
	}

	/**
	 * ML 모듈 실행시 강제 변환된 사이즈를 기존 이미지 사이즈 및 좌표로 되돌리는 함수
	 * @param geometry
	 * @return 원본 이미지 해상도로 좌표가 변환된 geometry 반환
	 * @throws Exception
	 */
	private Geometry GeometryToOrginal(Geometry geometry) throws Exception {
		GeometryFactory gf = new GeometryFactory();
		Geometry result = null;

		if (geometry instanceof Point) {

			Point point = (Point)geometry;
			Coordinate coord = point.getCoordinate();
			result = gf.createPoint(coordToOriginal(coord));

		} else if (geometry instanceof LineString) {

			LineString lineString = (LineString)geometry;
			Coordinate[] coords = lineString.getCoordinates();
			for(int i=0; i<coords.length; i++) {
				coords[i] = coordToOriginal(coords[i]);
			}
			result = gf.createLineString(coords);

		} else if (geometry instanceof Polygon) {

			Polygon polygon = (Polygon)geometry;
			LinearRing shell = null;
			List<LinearRing> holes = new ArrayList<>();
			Coordinate[] coords = polygon.getExteriorRing().getCoordinates();
			for(int j=0; j<coords.length; j++){
				coords[j] = coordToOriginal(coords[j]);
			}
			shell = gf.createLinearRing(coords);
			for(int j=0; j<polygon.getNumInteriorRing(); j++){
				coords = polygon.getInteriorRingN(j).getCoordinates();
				for(int k=0; k<coords.length; k++){
					coords[k] = coordToOriginal(coords[k]);
				}
				holes.add(gf.createLinearRing(coords));
			}
			result = gf.createPolygon(shell, holes.toArray(new LinearRing[0]));

		} else if (geometry instanceof MultiPoint) {

			MultiPoint multiPoint = (MultiPoint)geometry;
			Point[] points = new Point[multiPoint.getNumGeometries()];
			for(int i=0; i<multiPoint.getNumGeometries(); i++) {
				Point point = (Point)multiPoint.getGeometryN(i);
				Coordinate coord = point.getCoordinate();
				points[i] = gf.createPoint(coordToOriginal(coord));
			}
			result = gf.createMultiPoint(points);

		} else if (geometry instanceof MultiLineString) {

			MultiLineString multiLineString = (MultiLineString)geometry;
			LineString[] lineStrings = new LineString[multiLineString.getNumGeometries()];
			for(int i=0; i<multiLineString.getNumGeometries(); i++) {
				LineString lineString = (LineString)multiLineString.getGeometryN(i);
				Coordinate[] coords = lineString.getCoordinates();
				for(int j=0; j<coords.length; j++){
					coords[j] = coordToOriginal(coords[j]);
				}
				lineStrings[i] = gf.createLineString(coords);
			}
			result = gf.createMultiLineString(lineStrings);

		} else if (geometry instanceof MultiPolygon) {
			MultiPolygon multiPolygon = (MultiPolygon)geometry;
			Polygon[] polygons = new Polygon[multiPolygon.getNumGeometries()];
			System.out.println("##### MultiPolygon ");
			for(int i=0; i<multiPolygon.getNumGeometries(); i++) {
				Polygon polygon = (Polygon)multiPolygon.getGeometryN(i);
				LinearRing shell = null;
				List<LinearRing> holes = new ArrayList<>();
				Coordinate[] coords = polygon.getExteriorRing().getCoordinates();
				for(int j=0; j<coords.length; j++){
					coords[j] = coordToOriginal(coords[j]);
				}
				shell = gf.createLinearRing(coords);
				for(int j=0; j<polygon.getNumInteriorRing(); j++){
					coords = polygon.getInteriorRingN(j).getCoordinates();
					for(int k=0; k<coords.length; k++){
						coords[k] = coordToOriginal(coords[k]);
					}
					holes.add(gf.createLinearRing(coords));
				}
				polygons[i] = gf.createPolygon(shell, holes.toArray(new LinearRing[0]));
			}
			result = gf.createMultiPolygon(polygons);
		}
		return result;
	}

	/**
	 * ML 모듈 실행시 강제 변환된 좌표를 기존 이미지 좌표로 되돌리는 함수
	 * @param c
	 * @return 기존 이미지 좌표로 변환된 좌표 반환
	 * @throws Exception
	 */
	private Coordinate coordToOriginal(Coordinate c) throws Exception {
		return new Coordinate((c.x + 2) * transform.getWidthRatio(), (transform.getTransformH() - (c.y - 2)) * transform.getHeightRatio());
		//return new Coordinate(c.x * transform.getWidthRatio(), ((transform.getTransformW()+transform.getTransformH())/2 - c.y) * transform.getHeightRatio());
	}

	/**
	 * 픽셀 좌표를 실제 좌표로 변환하는 함수
	 * @param x
	 * @param y
	 * @return 변환된 실제 좌표
	 * @throws IOException
	 */
	private Node getNode(double x, double y) throws IOException {
		return new Node(center.add(x * scale, -y * scale));
	}
}
