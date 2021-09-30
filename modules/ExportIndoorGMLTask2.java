package org.openstreetmap.josm.plugins.indoorSpatialHelper.modules;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
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

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.AbstractIndoorObject;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;
import org.openstreetmap.josm.tools.I18n;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * IndoorGML 파일 생성을 처리하는 쓰레드 클래스
 */
public class ExportIndoorGMLTask2 extends PleaseWaitRunnable {

	public ExportIndoorGMLTask2(Component parent, String title, boolean ignoreException) {
		super(parent, title, ignoreException);
		// TODO Auto-generated constructor stub
	}

	static class Tags {
		static final String ID = "gml:id";
		static final String INDOOR_FEATURES = "core:IndoorFeatures";
		static final String[][] INDOOR_FEATURES_ATTR = {
				{"xmlns:gml", "http://www.opengis.net/gml/3.2"},
				{"xmlns:xlink", "http://www.w3.org/1999/xlink"},
				{"xmlns:core", "http://www.opengis.net/indoorgml/1.0/core"},
				{"xmlns:navi", "http://www.opengis.net/indoorgml/1.0/navigation"},
				{"xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"},
				{ID, "a59aa57f-36ab-86fa-8950-d7fa844ab96b"},
				{"xsi:schemaLocation", "http://www.opengis.net/indoorgml/1.0/core "
						+ "http://schemas.opengis.net/indoorgml/1.0/indoorgmlcore.xsd "
						+ "http://www.opengis.net/indoorgml/1.0/navigation "
						+ "http://schemas.opengis.net/indoorgml/1.0/indoorgmlnavi.xsd"}};

		static final String BOUNDED_BY = "gml:boundedBy";
		static final String[][] BOUNDED_BY_ATTR = {{"xsi:nil", "true"}};

		static final String PRIMAL_SPACE = "core:primalSpaceFeatures";
		static final String PRIMAL_SPACE_FEATURES = "core:PrimalSpaceFeatures";
		static final String[][] PRIMAL_SPACE_FEATURES_ATTR = {{"gml:id", "a59aa57f-36ab-86fa-8950-d7fa844ab96b"}};

		static final String MULTI_LAYERED = "core:multiLayeredGraph";
		static final String MULTI_LAYERED_GRAPH = "core:MultiLayeredGraph";
		static final String[][] MULTI_LAYERED_GRAPH_ATTR = {{"gml:id", "a59aa57f-36ab-86fa-8950-d7fa844ab96b"}};

		static final String CELL_SPACE_MEMBER = "core:cellSpaceMember";
		static final String GENERAL_SPACE = "navi:GeneralSpace";
		static final String TRANSITION_SPACE = "navi:TransitionSpace";
		static final String DESCRIPTION = "gml:description";
		static final String NAME = "gml:name";
		static final String CELL_SPACE_GEOMETRY = "core:cellSpaceGeometry";
		static final String GEOMETRY3D = "core:Geometry3D";
		static final String SOLID = "gml:Solid";
		static final String EXTERIOR = "gml:exterior";
		static final String SHELL = "gml:Shell";
		static final String SURFACE_MEMBER = "gml:surfaceMember";
		static final String POLYGON = "gml:Polygon";
		static final String LINEARRING = "gml:LinearRing";

		static final String CELL_SPACE_BOUNDARY_MEMBER = "core:cellSpaceBoundaryMember";
		static final String CONNECTION_BOUNDARY = "navi:ConnectionBoundary";
		static final String DUALITY = "core:duality";
		static final String CELL_SPACE_BOUNDARY_GEOMETRY = "core:cellSpaceBoundaryGeometry";

		static final String SPACE_LAYERS = "core:spaceLayers";
		static final String[][] SPACE_LAYERS_ATTR = {{"gml:id", "a59aa57f-36ab-86fa-8950-d7fa844ab96b"}};
		static final String SPACE_LAYER_MEMBER = "core:spaceLayerMember";
		static final String SPACE_LAYER = "core:SpaceLayer";
		static final String NODES = "core:nodes";
		static final String[][] NODES_ATTR = {{"gml:id", "a59aa57f-36ab-86fa-8950-d7fa844ab96b"}};
		static final String EDGES = "core:edges";
		static final String[][] EDGES_ATTR = {{"gml:id", "a59aa57f-36ab-86fa-8950-d7fa844ab96b"}};

		static final String POS = "gml:pos";
		static final String[][] POS_ATTR = {{"srsDimension", "3"}};

		static final String HREF = "xlink:href";
	}

	private int cellSpace_id = 1;
	private int cellSpaceBoundary_id = 1;
	private int transition_id = 1;
	private int space_id = 1;

	private Document doc;

	private String srsName = MainApplication.getMap().mapView.getProjection().toCode();
	private Projection projection = Projections.getProjectionByCode(srsName);

	private File file;
	private VectorLayer[] layers;
	private Map<Integer, Double> heightMap = new HashMap<>();

	private boolean canceled;
	private boolean isForInviewer;

	/**
	 * {@link ExportIndoorGMLTask} 생성자
	 * @param gmlFile
	 * @param layer
	 * @param isForInviewer
	 */
	public ExportIndoorGMLTask2(File gmlFile, VectorLayer layer, boolean isForInviewer){
		this(gmlFile, new VectorLayer[]{layer}, isForInviewer);
	}

	/**
	 * {@link ExportIndoorGMLTask} 생성자
	 * @param gmlFile
	 * @param layers
	 * @param isForInviewer
	 */
	public ExportIndoorGMLTask2(File gmlFile, VectorLayer[] layers, boolean isForInviewer){
		super(I18n.tr("Export Vector Layer..."), false);
		this.file = gmlFile;
		this.layers = layers;
		Arrays.sort(layers, getComparator());

		this.isForInviewer = isForInviewer;
	}

	@Override
	protected void cancel() {
		this.canceled = true;
	}

	@Override
	protected void realRun() throws SAXException, IOException, OsmTransferException {

		FileOutputStream fos = null;
		try{
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			doc = docBuilder.newDocument();
			Element root = doc.createElement(Tags.INDOOR_FEATURES);
			for(String[] attr : Tags.INDOOR_FEATURES_ATTR){
				root.setAttribute(attr[0], (attr[0].equals("gml:id") ? getGmlId() : attr[1]));
			}
			root.appendChild(getBoundedBy());
			root.appendChild(getPrimalSpaceFeatures());
			Element primalSpaceFeatures = (Element)root.getLastChild().getFirstChild();

			root.appendChild(getMultiLayeredGraph());
			Element multiLayeredGraph = (Element)root.getLastChild().getFirstChild();

			appendIndoorGmlTag(primalSpaceFeatures, multiLayeredGraph);
			doc.appendChild(root);

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			//transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
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
		}catch(TransformerConfigurationException e){
			e.printStackTrace();
		}catch(ParserConfigurationException e){
			e.printStackTrace();
		}catch(TransformerException e){
			e.printStackTrace();
		}finally{
			if(null != fos){fos.close();}
		}
	}

	/**
	 * IndoorGML boundedBy 태그를 생성하는 함수
	 * @return IndoorGML 태그 반환
	 */
	public Element getBoundedBy(){
		Element el = doc.createElement(Tags.BOUNDED_BY);
		for(String[] attr : Tags.BOUNDED_BY_ATTR){
			el.setAttribute(attr[0], attr[1]);
		}
		return el;
	}

	/**
	 * IndoorGML primalSpaceFeatures 태그를 생성하는 함수
	 * @return IndoorGML 태그 반환
	 */
	public Element getPrimalSpaceFeatures(){
		Element el = doc.createElement(Tags.PRIMAL_SPACE);
		Element child = doc.createElement(Tags.PRIMAL_SPACE_FEATURES);
		for(String[] attr : Tags.PRIMAL_SPACE_FEATURES_ATTR){
			child.setAttribute(attr[0], (attr[0].equals("gml:id") ? getGmlId() : attr[1]));
		}
		child.appendChild(getBoundedBy());
		el.appendChild(child);
		return el;
	}

	/**
	 * IndoorGML multiLayeredGraph 태그를 생성하는 함수
	 * @return IndoorGML 태그 반환
	 */
	public Element getMultiLayeredGraph(){
		Element el = doc.createElement(Tags.MULTI_LAYERED);
		Element child = doc.createElement(Tags.MULTI_LAYERED_GRAPH);
		for(String[] attr : Tags.MULTI_LAYERED_GRAPH_ATTR){
			child.setAttribute(attr[0], (attr[0].equals("gml:id") ? getGmlId() : attr[1]));
		}
		child.appendChild(getBoundedBy());
		el.appendChild(child);
		return el;
	}

	/**
	 * 벡터 레이어의 오브젝트를 IndoorGML 태그로 생성하는 함수
	 * @param primalSpaceFeatures
	 * @param multiLayeredGraph
	 */
	public void appendIndoorGmlTag(Element primalSpaceFeatures, Element multiLayeredGraph){

		List<Element> cellSpaceList = new ArrayList<>();
		List<Element> transitionList = new ArrayList<>();
		List<Element> cellSpaceBoundaryList = new ArrayList<>();
		for(VectorLayer layer : layers){
			int storey = layer.getFloor();
//			ExportIndoorGMLTask2.this.setHeightMap(storey, layer.getHeight() * 0.001);
			ExportIndoorGMLTask2.this.setHeightMap(storey, Double.MIN_VALUE);
		}
		
		for(VectorLayer layer : layers){
			Collection<Way> ways = layer.getRealDataSet().getWays();
			int storey = layer.getFloor();
			for(Way way : ways){
				Map<String, String> map = way.getKeys();
				String cls = map.get(Constants.INDOOR_OBJECT_KEY);
				
				String indoorgml_class = map.get("indoorgml_class");
				
				if(null != cls && AbstractIndoorObject.isIndoorObject(cls)){
					if(Constants.WALL_CLASS_CODE[0].equals(cls)){
						// 건물 층 높이 설정 : 가장 높은 wall값을 건물의 높이로 설정함
						try {
							double height = Float.parseFloat(map.get("height")) * 0.001;
							heightMap.put(storey, Math.max(heightMap.get(storey), height));
						} catch(Exception e) {
							
						}

					}else if(Constants.WINDOW_CLASS_CODE[0].equals(cls)){
						CellSpaceBoundary csb = new CellSpaceBoundary(storey, "window", way);
						cellSpaceBoundaryList.add(csb.getTag());
					}else if(Constants.DOOR_CLASS_CODE[0].equals(cls)){
						CellSpaceBoundary csb = new CellSpaceBoundary(storey, "door", way);
						cellSpaceBoundaryList.add(csb.getTag());
					}else if(Constants.STAIR_CLASS_CODE[0].equals(cls)){
						Transition t = new Transition(storey, "stair", way);
						transitionList.add(t.getTag());
					}else if(Constants.LIFT_CLASS_CODE[0].equals(cls)){
						Transition t = new Transition(storey, "elevator", way);
						transitionList.add(t.getTag());
					}else if(Constants.SPACE_CLASS_CODE[0].equals(cls)){
						if(!indoorgml_class.equals("1000")) { // indoorgml_class가 1000일 경우 처리
							CellSpace cs = new CellSpace(storey, "room", way);
							cellSpaceList.add(cs.getTag());
						} else {
							Transition t = new Transition(storey, "room", way);
							transitionList.add(t.getTag());
						}
					}else if(Constants.SUBSPACE_CLASS_CODE[0].equals(cls)){
						Transition t = new Transition(storey, "corridor", way);
						transitionList.add(t.getTag());
					}
				}else{
					CellSpace cs = new CellSpace(storey, "", way);
					cellSpaceList.add(cs.getTag());
				}
			}
		}
		cellSpaceList.addAll(transitionList);
		cellSpaceList.addAll(cellSpaceBoundaryList);

		for(Element el : cellSpaceList){
			if(null != el){
				primalSpaceFeatures.appendChild(el);
			}
		}
	}

	/**
	 * gmlId 값을 반환하는 함수
	 * @return gmlId 반환
	 */
	public static String getGmlId(){
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<32; i++){
			if(i == 8 || i == 12 || i == 16 || i == 20){
				sb.append("-");
			}
			if(random.nextBoolean()){
				sb.append((char)(random.nextInt(26)+97));
			}else{
				sb.append(random.nextInt(10));
			}
		}
		return sb.toString();
	}

	/**
	 * 벡터 레이어의 정렬 기준을 Comparator 객체를 통해 정의한 함수
	 * @return 벡터 레이어의 정렬 기준 객체를 반환
	 */
	public static Comparator<VectorLayer> getComparator(){
		return new Comparator<VectorLayer>(){
			@Override
			public int compare(VectorLayer l1, VectorLayer l2) {
				if(l1.getFloor() * l2.getFloor() > 0){
					if(Math.abs(l1.getFloor()) > Math.abs(l2.getFloor())){
						return 1;
					}else if(Math.abs(l1.getFloor()) < Math.abs(l2.getFloor())){
						return -1;
					}
				}else if(l1.getFloor() * l2.getFloor() < 0){
					if(l1.getFloor() > l2.getFloor()){
						return -1;
					}else if(l1.getFloor() < l2.getFloor()){
						return 1;
					}
				}
				return 0;
			}};
	}

	/**
	 * 벡터 레이어들의 높이 값을 설정하는 함수
	 * @param storey
	 * @param height
	 */
	public void setHeightMap(int storey, double height){
		if(heightMap.containsKey(storey)){
			heightMap.put(storey, Math.max(heightMap.get(storey), height));
		}else{
			heightMap.put(storey, height);
		}
	}
	
	public double getFloorHeight(int storey) {
		return heightMap.get(storey);
	}

//	/**
//	 * 해당 층의 상대 높이 값을 반환하는 함수
//	 * @param storey
//	 * @return 해당 층의 상대 높이 반환
//	 */
//	public double getFloorHeight(int storey){
//		double floorHeight = 0;
//		if(storey > 0){
//			for(int i=1; i<storey; i++){
//				if(heightMap.containsKey(i) && null != heightMap.get(i)){
//					floorHeight += heightMap.get(storey);
//				}else{
//					floorHeight += Constants.DEFALT_HEIGHT;
//				}
//			}
//		}else if(storey < 0){
//			for(int i=-1; i>storey; i--){
//				if(heightMap.containsKey(i) && null != heightMap.get(i)){
//					floorHeight -= heightMap.get(storey);
//				}else{
//					floorHeight -= Constants.DEFALT_HEIGHT;
//				}
//			}
//		}
//		return floorHeight;
//	}
//
//	/**
//	 * 해당 층의 절대 높이 값을 반환하는 함수
//	 * @param storey
//	 * @return 해당 층의 절대 높이 반환
//	 */
//	public double getHeight(int storey){
//		double floorHeight = 0;
//		if(storey > 0){
//			if(heightMap.containsKey(storey) && null != heightMap.get(storey)){
//				floorHeight = heightMap.get(storey);
//			}else{
//				floorHeight = Constants.DEFALT_HEIGHT;
//			}
//		}else if(storey < 0){
//			if(heightMap.containsKey(storey) && null != heightMap.get(storey)){
//				floorHeight = -1 * heightMap.get(storey);
//			}else{
//				floorHeight = -1 * Constants.DEFALT_HEIGHT;
//			}
//		}
//		return floorHeight;
//	}

	@Override
	protected void finish() {
		String msg = tr("File export finished.");
		JOptionPane.showMessageDialog(MainApplication.getMainFrame(), msg);
	}

	/**
	 * IndoorGML 오브젝트 구현을 위한 추상 클래스
	 */
	protected abstract class IC {

		protected int type;
		protected int outer = 0;
		protected int storey = 99999;

		protected String indoor;
		protected String name;
		protected double height;

		protected String description;

		protected IC duality = null;
		protected List<IC> partialboundedBy;
		protected List<IC> connects;
		protected List<IC> inner;
		protected int class_value;
		protected String indoorgml_class = "";
		protected String indoorgml_function = "";
		protected String indoorgml_usage = "";
		
		public IC()
		{
			
		}

		/**
		 * 해당 객체의 IndoorGML 태그를 반환하는 함수
		 * @return IndoorGML 태그를 반환
		 */
		protected abstract Element getTag();

		/**
		 * IndoorGML description 태그를 설정하는 함수
		 * @param tagged
		 */
		protected void setAttributes(Map<String, String> tagged){
			StringBuffer sb = new StringBuffer();
//			sb.append("storey");
			sb.append("level");
			sb.append("=\"");
			sb.append(storey);
			sb.append("\":");
			if(null!= indoor && !indoor.equals("")){
				sb.append("indoor");
				sb.append("=\"");
				sb.append(indoor);
				sb.append("\":");
			}

			Set<Entry<String, String>> set = tagged.entrySet();
			for(Entry<String, String> entry : set){
				if(entry.getKey().toLowerCase().equals("name")){
					this.name = entry.getValue();
				}else if(entry.getKey().toLowerCase().equals("storey")){
					continue;
				}else if(entry.getKey().toLowerCase().equals("indoor")){
					continue;
				}else if(entry.getKey().toLowerCase().equals("height")){
					try {
						this.height = Double.parseDouble(entry.getValue()) * 0.001;
					}catch(NumberFormatException e){}
				}else{
					if(entry.getKey().equals(Constants.INDOOR_OBJECT_KEY) && !entry.getValue().equals("")){
						sb.append(entry.getKey());
						sb.append("=\"");
						sb.append(entry.getValue());
						sb.append("\":");
						class_value = Integer.parseInt(entry.getValue());
					}
				}
				
				if(entry.getKey().toLowerCase().equals("indoorgml_class")){
					this.indoorgml_class =  entry.getValue();
				} else if(entry.getKey().toLowerCase().equals("indoorgml_function")){
					this.indoorgml_function =  entry.getValue();
				} else if(entry.getKey().toLowerCase().equals("indoorgml_usage")){
					this.indoorgml_usage =  entry.getValue();
				}

			}
			this.description = sb.toString();
		}
		
		protected void setIndoorGML_Attribute(Element parent)
		{
			Element indoorgml_class = doc.createElement("navi:class");
			indoorgml_class.setTextContent(this.indoorgml_class);
			Element indoorgml_function = doc.createElement("navi:function");
			indoorgml_function.setTextContent(this.indoorgml_function);
			Element indoorgml_usage = doc.createElement("navi:usage");
			indoorgml_usage.setTextContent(this.indoorgml_usage);
			
			parent.appendChild(indoorgml_class);
			parent.appendChild(indoorgml_function);
			parent.appendChild(indoorgml_usage);
		}

		/**
		 * 폴리곤 오브젝트의 3차원 좌표 정보를 반환하는 함수
		 * @param storey
		 * @param nodes
		 * @param ceilingHeight
		 * @return 폴리곤 오브젝트의 좌표 정보를 반환
		 */
		protected List<List<Pos>> getPolygonPosList(int storey, List<Node> nodes, double height){
			List<List<Pos>> polygonList = new ArrayList<>();
			if(nodes.size() > 3 && nodes.get(0).getEastNorth().equals(nodes.get(nodes.size() - 1).getEastNorth())){

				double bottomHeight = ExportIndoorGMLTask2.this.getFloorHeight(storey) * (storey - 1);
				if(0 == height){
					height = Constants.DEFALT_HEIGHT;
				}	
				
				Coordinate[] coords = new Coordinate[nodes.size()];
				
				for(int i=0; i<nodes.size(); i++) {
					Coordinate coord = new Coordinate(nodes.get(i).lon(), nodes.get(i).lat());
					coords[i] = coord;
				}
				
				if(!CGAlgorithms.isCCW(coords)) {
					Collections.reverse(nodes);
				}

				List<Pos> topPosList = new ArrayList<>();
				List<Pos> bottomPosList = new ArrayList<>();
				for(int i=0; i<nodes.size(); i++){
//					LatLon ll = projection.eastNorth2latlon(nodes.get(i).getEastNorth());
					EastNorth ll = nodes.get(i).getEastNorth();

					topPosList.add(new Pos(ll.getY(), ll.getX(), bottomHeight + height));
					bottomPosList.add(new Pos(ll.getY(), ll.getX(), bottomHeight));
				}
				polygonList.add(topPosList);
				for(int i=0; i<nodes.size()-1; i++){
					List<Pos> posList = new ArrayList<>();

//					LatLon ll1 = projection.eastNorth2latlon(nodes.get(i).getEastNorth());
//					LatLon ll2 = projection.eastNorth2latlon(nodes.get(i+1).getEastNorth());

					EastNorth ll1 = nodes.get(i).getEastNorth();
					EastNorth ll2 = nodes.get(i + 1).getEastNorth();

					posList.add(new Pos(ll1.getY(), ll1.getX(), bottomHeight));
					posList.add(new Pos(ll2.getY(), ll2.getX(), bottomHeight));
					posList.add(new Pos(ll2.getY(), ll2.getX(), bottomHeight + height));
					posList.add(new Pos(ll1.getY(), ll1.getX(), bottomHeight + height));
					posList.add(new Pos(ll1.getY(), ll1.getX(), bottomHeight));

					polygonList.add(posList);
				}
				Collections.reverse(bottomPosList);
				polygonList.add(bottomPosList);
			}
			return polygonList;
		}

		/**
		 * 라인 오브젝트의 3차원 좌표 정보를 반환하는 함수
		 * @param storey
		 * @param nodes
		 * @param ceilingHeight
		 * @return 라인 오브젝트의 좌표 정보를 반환
		 */
		protected List<Pos> getLinePosList(int storey, List<Node> nodes, double height){
			List<Pos> posList = new ArrayList<>();
			if(nodes.size() >= 2 && !nodes.get(0).getEastNorth().equals(nodes.get(1).getEastNorth())){
				double bottomHeight = ExportIndoorGMLTask2.this.getFloorHeight(storey) * (storey - 1);
				
//				LatLon ll1 = projection.eastNorth2latlon(nodes.get(0).getEastNorth());
//				LatLon ll2 = projection.eastNorth2latlon(nodes.get(1).getEastNorth());
				
				EastNorth ll1 = nodes.get(0).getEastNorth();
				EastNorth ll2 = nodes.get(1).getEastNorth();

				posList.add(new Pos(ll1.getY(), ll1.getX(), bottomHeight));
				posList.add(new Pos(ll2.getY(), ll2.getX(), bottomHeight));
				posList.add(new Pos(ll2.getY(), ll2.getX(), bottomHeight + height));
				posList.add(new Pos(ll1.getY(), ll1.getX(), bottomHeight + height));
				posList.add(new Pos(ll1.getY(), ll1.getX(), bottomHeight));
				
			}
			return posList;
		}
		
		/**
		 * 라인 오브젝트의 3차원 좌표 정보를 반환하는 함수
		 * @param storey
		 * @param nodes
		 * @param ceilingHeight
		 * @return 라인 오브젝트의 좌표 정보를 반환
		 */
		protected List<Pos> getLinePosList(int storey, List<Node> nodes, double bottom, double height){
			List<Pos> posList = new ArrayList<>();
			if(nodes.size() >= 2 && !nodes.get(0).getEastNorth().equals(nodes.get(1).getEastNorth())){
				double bottomHeight = ExportIndoorGMLTask2.this.getFloorHeight(storey) * (storey - 1);
				
//				LatLon ll1 = projection.eastNorth2latlon(nodes.get(0).getEastNorth());
//				LatLon ll2 = projection.eastNorth2latlon(nodes.get(1).getEastNorth());

				EastNorth ll1 = nodes.get(0).getEastNorth();
				EastNorth ll2 = nodes.get(1).getEastNorth();

				posList.add(new Pos(ll1.getY(), ll1.getX(), bottomHeight + bottom));
				posList.add(new Pos(ll2.getY(), ll2.getX(), bottomHeight + bottom));
				posList.add(new Pos(ll2.getY(), ll2.getX(), bottomHeight + height));
				posList.add(new Pos(ll1.getY(), ll1.getX(), bottomHeight + height));
				posList.add(new Pos(ll1.getY(), ll1.getX(), bottomHeight + bottom));
			}
			return posList;
		}
	}

	/**
	 * 오브젝트의 3차원 좌표 정보를 관리하는 클래스
	 */
	protected class Pos extends IC {

		protected String latitude;
		protected String longitude;
		protected String z;

		/**
		 * {@link Pos} 생성자
		 * @param latitude
		 * @param longitude
		 * @param z
		 */
		public Pos(double latitude, double longitude, String z){
			this.type = 0;
			if(isForInviewer){
//				this.latitude = String.valueOf(String.format("%.14f", latitude*10000));
//				this.longitude = String.valueOf(String.format("%.14f", longitude*10000));
//				this.z = String.valueOf(String.format("%.6f", Double.parseDouble(z)*0.01));

				// EPSG : 5686으로 변경하기 전 주석
				this.latitude = String.valueOf(String.format("%.14f", latitude));
				this.longitude = String.valueOf(String.format("%.14f", longitude));
				this.z = String.valueOf(String.format("%.6f", Double.parseDouble(z)));
			}else{
				this.latitude = String.valueOf(String.format("%.14f", latitude));
				this.longitude = String.valueOf(String.format("%.14f", longitude));
				//this.z = z;
				//this.z = String.valueOf(String.format("%.14f", (Double.parseDouble(z)/(25+30)/2)/3600));
				this.z = String.valueOf(String.format("%.6f", Double.parseDouble(z)*0.00001));
				//this.z = String.valueOf(String.format("%.6f", Double.parseDouble(z)*0.01));
			}
		}
		public Pos(double latitude, double longitude, double z){
			this.type = 0;
			if(isForInviewer){
//				this.latitude = String.valueOf(String.format("%.14f", latitude*10000));
//				this.longitude = String.valueOf(String.format("%.14f", longitude*10000));
//				this.z = String.valueOf(z*0.01);

				// EPSG : 5686으로 변경하기 전 주석
				this.latitude = String.valueOf(String.format("%.14f", latitude));
				this.longitude = String.valueOf(String.format("%.14f", longitude));
				this.z = String.valueOf(z);
			}else{
				this.latitude = String.valueOf(String.format("%.14f", latitude));
				this.longitude = String.valueOf(String.format("%.14f", longitude));
				//this.z = z;
				//this.z = String.valueOf(String.format("%.14f", (Double.parseDouble(z)/(25+30)/2)/3600));
				this.z = String.valueOf(z*0.00001);
				//this.z = String.valueOf(String.format("%.6f", Double.parseDouble(z)*0.01));
			}
		}
		@Override
		protected Element getTag(){
			Element pos = doc.createElement(Tags.POS);
			for(String attr[] : Tags.POS_ATTR){
				pos.setAttribute(attr[0], attr[1]);
			}
			pos.setTextContent(longitude + " " + latitude + " " + z);
			return pos;
		}
		
		protected Element getTagWithoutHeight(){
			Element pos = doc.createElement(Tags.POS);
			for(String attr[] : Tags.POS_ATTR){
				pos.setAttribute(attr[0], attr[1]);
			}
			pos.setTextContent(longitude + " " + latitude + " 0");
			return pos;
		}
	}

	/**
	 * IndoorGML CellSpaceMember 태그 생성을 위한 클래스
	 */
	protected class CellSpace extends IC {

		protected List<List<Pos>> pos_vector;

		/**
		 * {@link CellSpace} 생성자
		 * @param storey
		 * @param indoor
		 * @param way
		 */
		public CellSpace(int storey, String indoor, Way way){
			this.type = 1;
			this.outer = 1;
			this.storey = storey;
			this.indoor = indoor;
			setAttributes(way.getKeys());
			pos_vector = getPolygonPosList(storey, way.getNodes(), height);
		}

		@Override
		protected Element getTag(){
			if(pos_vector.size() > 0){
				Element cellSpaceMember = doc.createElement(Tags.CELL_SPACE_MEMBER);

				Element generalSpace = doc.createElement(Tags.GENERAL_SPACE);
//				generalSpace.setAttribute(Tags.ID, "C" + cellSpace_id);
				generalSpace.setAttribute(Tags.ID, "C" + space_id);
				cellSpaceMember.appendChild(generalSpace);

				Element desc = doc.createElement(Tags.DESCRIPTION);
				desc.setTextContent(description);
				generalSpace.appendChild(desc);

				Element nm = doc.createElement(Tags.NAME);
				nm.setTextContent(name);
				generalSpace.appendChild(nm);

				generalSpace.appendChild(getBoundedBy());

				Element cellSpaceGeometry = doc.createElement(Tags.CELL_SPACE_GEOMETRY);
				generalSpace.appendChild(cellSpaceGeometry);

				Element geometry3D = doc.createElement(Tags.GEOMETRY3D);
				cellSpaceGeometry.appendChild(geometry3D);

				Element solid = doc.createElement(Tags.SOLID);
//				solid.setAttribute(Tags.ID, "CG-C" + cellSpace_id++);
				solid.setAttribute(Tags.ID, "CG-C" + space_id++);
				geometry3D.appendChild(solid);

				Element exterior = doc.createElement(Tags.EXTERIOR);
				solid.appendChild(exterior);

				Element shell = doc.createElement(Tags.SHELL);
				exterior.appendChild(shell);
				
				// attribute
				setIndoorGML_Attribute(generalSpace);

				for(List<Pos> posList : pos_vector){
					Element surfaceMember = doc.createElement(Tags.SURFACE_MEMBER);
					shell.appendChild(surfaceMember);

					Element polygon = doc.createElement(Tags.POLYGON);
					surfaceMember.appendChild(polygon);

					Element exterior_in = doc.createElement(Tags.EXTERIOR);
					polygon.appendChild(exterior_in);

					Element linearRing = doc.createElement(Tags.LINEARRING);
					exterior_in.appendChild(linearRing);

					for(Pos pos : posList){
						linearRing.appendChild(pos.getTag());
					}
				}
				return cellSpaceMember;
			}
			return null;
		}
	}

	/**
	 * IndoorGML CellSpaceBoundary 태그 생성을 위한 클래스
	 */
	protected class CellSpaceBoundary extends IC {

		protected List<Pos> pos_vector;

		/**
		 * {@link CellSpaceBoundary} 생성자
		 * @param storey
		 * @param indoor
		 * @param way
		 */
		public CellSpaceBoundary(int storey, String indoor, Way way){
			this.type = 2;
			this.outer = 1;
			this.storey = storey;
			this.indoor = indoor;
			setAttributes(way.getKeys());
			
			if(this.height == 0) {
				double storeyHeight = ExportIndoorGMLTask2.this.getFloorHeight(storey);
				if("door".equals(indoor)){
//					pos_vector = getLinePosList(storey, way.getNodes(), storeyHeight);
					pos_vector = getLinePosList(storey, way.getNodes(), 4);
				}else{ // window
//					pos_vector = getLinePosList(storey, way.getNodes(), storeyHeight/3);
					pos_vector = getLinePosList(storey, way.getNodes(), 1.5, 3.5);
				}
 			} else {
				if("door".equals(indoor)){
					pos_vector = getLinePosList(storey, way.getNodes(), this.height);
				} else {
					pos_vector = getLinePosList(storey, way.getNodes(), 1.5, this.height + 1.5);
				}
			}
		}

		@Override
		protected Element getTag(){
			if(pos_vector.size() > 0){
				Element cellSpaceBoundaryMember = doc.createElement(Tags.CELL_SPACE_BOUNDARY_MEMBER);

				Element connectionBoundary = doc.createElement(Tags.CONNECTION_BOUNDARY);
				connectionBoundary.setAttribute(Tags.ID, "B" + cellSpaceBoundary_id);
				cellSpaceBoundaryMember.appendChild(connectionBoundary);

				Element desc = doc.createElement(Tags.DESCRIPTION);
				desc.setTextContent(description);
				connectionBoundary.appendChild(desc);

				Element nm = doc.createElement(Tags.NAME);
				nm.setTextContent(name);
				connectionBoundary.appendChild(nm);

				connectionBoundary.appendChild(getBoundedBy());

				if(null != duality){
					Element dual = doc.createElement(Tags.DUALITY);
					dual.setAttribute(Tags.HREF, "#");
					connectionBoundary.appendChild(dual);
				}

				Element cellSpaceBoundaryGeometry = doc.createElement(Tags.CELL_SPACE_BOUNDARY_GEOMETRY);
				connectionBoundary.appendChild(cellSpaceBoundaryGeometry);

				Element geometry3D = doc.createElement(Tags.GEOMETRY3D);
				cellSpaceBoundaryGeometry.appendChild(geometry3D);

				Element polygon = doc.createElement(Tags.POLYGON);
				polygon.setAttribute(Tags.ID, "CBG-B" + cellSpaceBoundary_id++);
				geometry3D.appendChild(polygon);

				Element exterior = doc.createElement(Tags.EXTERIOR);
				polygon.appendChild(exterior);

				Element linearRing = doc.createElement(Tags.LINEARRING);
				exterior.appendChild(linearRing);
				
				
				// attribute
				setIndoorGML_Attribute(connectionBoundary);

//				// door, window 높이값 설정
//				if(class_value == 2 || class_value == 3) {
//					linearRing.appendChild(pos_vector.get(0).getTagWithoutHeight());
//					linearRing.appendChild(pos_vector.get(1).getTagWithoutHeight());
//					linearRing.appendChild(pos_vector.get(2).getTag());
//					linearRing.appendChild(pos_vector.get(3).getTag());
//					linearRing.appendChild(pos_vector.get(4).getTagWithoutHeight());
//				} else {
//					for(Pos pos : pos_vector){
//						linearRing.appendChild(pos.getTag());
//					}
//				}
				for(Pos pos : pos_vector){
					linearRing.appendChild(pos.getTag());
				}
				return cellSpaceBoundaryMember;
			}
			return null;
		}
	}
	/**
	 * IndoorGML State 태그 생성을 위한 클래스
	 */
	protected class State extends IC {
		protected Pos pos;

		/**
		 * {@link State} 생성자
		 */
		public State(){
			type = 3;
		}

		@Override
		protected Element getTag(){
			return null;
		}
	}

	/**
	 *
	 * IndoorGML TransitionSpace 태그 생성을 위한 클래스
	 */
	protected class Transition extends IC {

		protected String weight;
		protected List<List<Pos>> pos_vector;

		/**
		 * {@link Transition} 생성자
		 * @param storey
		 * @param indoor
		 * @param way
		 */
		public Transition(int storey, String indoor, Way way){
			this.type = 4;
			this.outer = 1;
			this.storey = storey;
			this.indoor = indoor;
			setAttributes(way.getKeys());
			pos_vector = getPolygonPosList(storey, way.getNodes(), height);
		}

		@Override
		protected Element getTag(){
			if(pos_vector.size() > 0){
				Element cellSpaceMember = doc.createElement(Tags.CELL_SPACE_MEMBER);

				Element transitionSpace = doc.createElement(Tags.TRANSITION_SPACE);
//				transitionSpace.setAttribute(Tags.ID, "C" + transition_id);
				transitionSpace.setAttribute(Tags.ID, "C" + space_id);
				cellSpaceMember.appendChild(transitionSpace);

				Element desc = doc.createElement(Tags.DESCRIPTION);
				desc.setTextContent(description);
				transitionSpace.appendChild(desc);

				Element nm = doc.createElement(Tags.NAME);
				nm.setTextContent(name);
				transitionSpace.appendChild(nm);

				transitionSpace.appendChild(getBoundedBy());

				Element cellSpaceGeometry = doc.createElement(Tags.CELL_SPACE_GEOMETRY);
				transitionSpace.appendChild(cellSpaceGeometry);
				
				Element geometry3D = doc.createElement(Tags.GEOMETRY3D);
				cellSpaceGeometry.appendChild(geometry3D);

				Element solid = doc.createElement(Tags.SOLID);
//				solid.setAttribute(Tags.ID, "CG-C" + transition_id++);
				solid.setAttribute(Tags.ID, "CG-C" + space_id++);
				geometry3D.appendChild(solid);

				Element exterior = doc.createElement(Tags.EXTERIOR);
				solid.appendChild(exterior);

				Element shell = doc.createElement(Tags.SHELL);
				exterior.appendChild(shell);

				// attribute
				setIndoorGML_Attribute(transitionSpace);

				for(List<Pos> posList : pos_vector){
					Element surfaceMember = doc.createElement(Tags.SURFACE_MEMBER);
					shell.appendChild(surfaceMember);

					Element polygon = doc.createElement(Tags.POLYGON);
					surfaceMember.appendChild(polygon);

					Element exterior_in = doc.createElement(Tags.EXTERIOR);
					polygon.appendChild(exterior_in);

					Element linearRing = doc.createElement(Tags.LINEARRING);
					exterior_in.appendChild(linearRing);

					for(Pos pos : posList){
						linearRing.appendChild(pos.getTag());
					}
				}
				return cellSpaceMember;
			}
			return null;
		}
	}
}
