package org.openstreetmap.josm.plugins.indoorSpatialHelper.modules;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.tools.I18n;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * GML 파일 생성을 처리하는 쓰레드 클래스
 */
public class ExportGMLTask extends PleaseWaitRunnable {

	private static final String OGR_FEATURE_COLLECTION = "ogr:FeatureCollection";

	private static final String[][] xmlSchema = {
			{"xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"},
			{"xsi:schemaLocation","http://ogr.maptools.org/gml_sample.xsd"},
			{"xmlns:ogr","http://ogr.maptools.org/"},
			{"xmlns:gml","http://www.opengis.net/gml"}
	};

	private static final String GML_BOUNDED_BY = "gml:boundedBy";
	private static final String GML_BOX = "gml:Box";
	private static final String GML_COORD = "gml:coord";
	private static final String GML_X = "gml:X";
	private static final String GML_Y = "gml:Y";

	private static final String GML_FEATURE_MEMBER = "gml:featureMember";
	private static final String OGR_GEOMETRY_PROPERTY = "ogr:geometryProperty";
	private static final String GML_MULTI_POLYGON = "gml:MultiPolygon";
	private static final String GML_POLYGON_MEMBER = "gml:polygonMember";
	private static final String GML_POLYGON = "gml:Polygon";
	private static final String GML_OUTER_BOUNDARY_IS = "gml:outerBoundaryIs";
	private static final String GML_LINEAR_RING = "gml:LinearRing";
	private static final String GML_COORDINATES = "gml:coordinates";

	private Document doc;

	//private String srsName = "EPSG:3857";
	private String srsName = MainApplication.getMap().mapView.getProjection().toCode();
	private Projection projection = Projections.getProjectionByCode(srsName);

	private File file;
	private VectorLayer layer;

	private double xmin, xmax, ymin, ymax;
	private Set<String> tagKeys = new HashSet<String>();

	private boolean canceled;

	/**
	 * {@link ExportGMLTask} 생성자
	 * @param gmlFile
	 * @param vectorLayer
	 */
	public ExportGMLTask(File gmlFile, VectorLayer vectorLayer) {
		super(I18n.tr("Export Vector Layer..."), false);
		this.file = gmlFile;
		this.layer = vectorLayer;
	}

	@Override
	protected void cancel() {
		this.canceled = true;
	}

	@Override
	protected void realRun() throws SAXException, IOException, OsmTransferException {

		FileOutputStream fos = null;

		try{
			getDataSetInfo();

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			doc = docBuilder.newDocument();
			Element featureCollectionEl = doc.createElement(OGR_FEATURE_COLLECTION);
			for(int i=0; i<xmlSchema.length; i++){
				featureCollectionEl.setAttribute(xmlSchema[i][0], xmlSchema[i][1]);
			}
			doc.appendChild(featureCollectionEl);

			this.setBoundedBy(featureCollectionEl);

			int seq = 0;
			Iterator<Way> ways = layer.getRealDataSet().getWays().iterator();
			while(ways.hasNext()) {
				if(canceled){
					return;
				}
				this.setFeaturMember(featureCollectionEl, ways.next(), seq++);
			}

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
			//transformer.setOutputProperty(OutputKeys.METHOD, "html");
			DOMSource source = new DOMSource(doc);

			String fileName = file.getCanonicalPath();
			if(fileName.toLowerCase().indexOf(".gml") < 0){
				fileName = fileName + ".gml";
			}

			if(canceled){
				return;
			}
			fos = new FileOutputStream(new File(fileName));
			StreamResult result = new StreamResult(fos);
			//StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);
		}catch (ParserConfigurationException e){
			e.printStackTrace();
		}catch (TransformerConfigurationException e){
			e.printStackTrace();
		}catch (TransformerException e){
			e.printStackTrace();
		}finally{
			if(null != fos){fos.close();}
		}
	}

	/**
	 * DataSet Way 객체들의 정보를 조회하는 함수
	 */
	private void getDataSetInfo() {

		boolean isFirst = true;
		Iterator<Way> ways = layer.getDataSet().getWays().iterator();
		while(ways.hasNext()) {
			Way way = ways.next();

			BBox bbox = way.getBBox();
			LatLon ll1 = bbox.getTopLeft();
			LatLon ll2 = bbox.getBottomRight();
			if(isFirst) {
				xmin = ll1.getX();
				xmax = ll2.getX();
				ymin = ll2.getY();
				ymax = ll1.getY();
				isFirst = !isFirst;
			}
			if(xmin > ll1.getX()) xmin = ll1.getX();
			if(xmax < ll2.getX()) xmax = ll2.getX();
			if(ymin > ll2.getY()) ymin = ll2.getY();
			if(ymax < ll1.getY()) ymax = ll1.getY();

			Map<String, String> tags = way.getKeys();
			Iterator<String> itr = tags.keySet().iterator();
			while(itr.hasNext()) {
				String key = itr.next();
				if(!tagKeys.contains(key)) {
					tagKeys.add(key);
				}
			}
		}
	}

	@Override
	protected void finish() {
		String msg = tr("File export finished.");
		JOptionPane.showMessageDialog(MainApplication.getMainFrame(), msg);
	}

	/**
	 * featureMember 태그를 생성하는 함수
	 * @param root
	 * @param way
	 * @param seq
	 */
	private void setFeaturMember(Element root, Way way, int seq) {

		String name = file.getName();
		if(name.indexOf(".") > 0){
			name = name.substring(0, name.lastIndexOf("."));
		}

		Element featureMemberEl = doc.createElement(GML_FEATURE_MEMBER);
		root.appendChild(featureMemberEl);

		Element fidEl = doc.createElement("ogr:" + name);
		fidEl.setAttribute("fid", name + "." + seq);
		featureMemberEl.appendChild(fidEl);

		/*
		if(seq == 0){
			BBox bbox = way.getBBox();
			LatLon ll1 = bbox.getTopLeft();
			LatLon ll2 = bbox.getBottomRight();

			xmin = ll1.getX();
			xmax = ll2.getX();
			ymin = ll2.getY();
			ymax = ll1.getY();
		}
		 */

		this.setGeometryProperty(fidEl, way.getNodes());

		Map<String, String> tags = way.getKeys();
		Iterator<String> keys = tagKeys.iterator();
		while(keys.hasNext()){
			String key = keys.next();
			Element propertyEl = doc.createElement("ogr:" + key);
			propertyEl.appendChild(doc.createTextNode(tags.getOrDefault(key, " ")));
			fidEl.appendChild(propertyEl);
		}
	}

	/**
	 * featureMember의 geometryProperty 태그를 생성하는 함수
	 * @param root
	 * @param nodes
	 */
	private void setGeometryProperty(Element root, List<Node> nodes) {
		String coordinates = "";
		for(Node node : nodes){
			LatLon ll = projection.eastNorth2latlon(node.getEastNorth());
			coordinates += " " + ll.getX() + "," + ll.getY();
			/*
			if(xmin > ll.getX()) {
				xmin = ll.getX();
			}
			if(xmax < ll.getX()) {
				xmax = ll.getX();
			}
			if(ymin > ll.getY()) {
				ymin = ll.getY();
			}
			if(ymax < ll.getY()) {
				ymax = ll.getY();
			}
			 */
		}

		coordinates = coordinates.length() > 0 ? coordinates.substring(1) : "";

		Element geometryPropertyEl = doc.createElement(OGR_GEOMETRY_PROPERTY);
		root.appendChild(geometryPropertyEl);

		Element multiPolygonEl = doc.createElement(GML_MULTI_POLYGON);
		multiPolygonEl.setAttribute("srsName", srsName);
		geometryPropertyEl.appendChild(multiPolygonEl);

		Element polygonMemberEl = doc.createElement(GML_POLYGON_MEMBER);
		multiPolygonEl.appendChild(polygonMemberEl);

		Element polygonEl = doc.createElement(GML_POLYGON);
		polygonMemberEl.appendChild(polygonEl);

		Element outerBoundaryIsEl = doc.createElement(GML_OUTER_BOUNDARY_IS);
		polygonEl.appendChild(outerBoundaryIsEl);

		Element linearRingEl = doc.createElement(GML_LINEAR_RING);
		outerBoundaryIsEl.appendChild(linearRingEl);

		Element coordinatesEl = doc.createElement(GML_COORDINATES);
		coordinatesEl.appendChild(doc.createTextNode(coordinates));
		linearRingEl.appendChild(coordinatesEl);
	}

	/**
	 * boundedBy 태그를 생성하는 함수
	 * @param root
	 */
	private void setBoundedBy(Element root) {

		Element boundedByEl = doc.createElement(GML_BOUNDED_BY);
		root.appendChild(boundedByEl);

		Element boxEl = doc.createElement(GML_BOX);
		boundedByEl.appendChild(boxEl);

		Element minCoordEl = doc.createElement(GML_COORD);
		boxEl.appendChild(minCoordEl);

		Element minXEl = doc.createElement(GML_X);
		minXEl.appendChild(doc.createTextNode(String.valueOf(xmin)));
		minCoordEl.appendChild(minXEl);

		Element minYEl = doc.createElement(GML_Y);
		minYEl.appendChild(doc.createTextNode(String.valueOf(ymin)));
		minCoordEl.appendChild(minYEl);

		Element maxCoordEl = doc.createElement(GML_COORD);
		boxEl.appendChild(maxCoordEl);

		Element maxXEl = doc.createElement(GML_X);
		maxXEl.appendChild(doc.createTextNode(String.valueOf(xmax)));
		maxCoordEl.appendChild(maxXEl);

		Element maxYEl = doc.createElement(GML_Y);
		maxYEl.appendChild(doc.createTextNode(String.valueOf(ymax)));
		maxCoordEl.appendChild(maxYEl);
	}
}
