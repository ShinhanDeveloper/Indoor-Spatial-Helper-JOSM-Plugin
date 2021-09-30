package org.openstreetmap.josm.plugins.indoorSpatialHelper.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import javax.imageio.ImageIO;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.CommonUtil;

/**
 * Class with information of the building
 */
public class BuildingInfo implements Serializable {

	private static final long serialVersionUID = 8332960990432331923L;

	private int topFloor = 1;
	private int lowestFloor = 1;

	private String nm;
	private String addr;
	private double lon;
	private double lat;

	private ArrayList<FloorInfo> floorList = new ArrayList<>();

	private BuildingTransform transform;
	/**
	 * {@link BuildingInfo} Constructor
	 * @param osmPrimitive
	 */
	public BuildingInfo(OsmPrimitive osmPrimitive) {
		LatLon latLon = osmPrimitive.getBBox().getCenter();
		this.lon = latLon.getX();
		this.lat = latLon.getY();
		TagMap tag = osmPrimitive.getKeys();
		if(tag.containsKey("name")){
			this.nm = tag.get("name");
		}
		if(tag.containsKey("building:levels")){
			try{
				this.topFloor = Integer.parseInt(tag.get("building:levels"));
			}catch(NumberFormatException e){
				this.topFloor = 1;
			}
		}
	}

	/**
	 * Returns top floor of the building.
	 * @return top floor info
	 */
	public int getTopFloor() {
		return topFloor;
	}

	/**
	 * Sets top floor of the building.
	 * @param floor top floor info
	 */
	public void setTopFloor(int floor) {
		this.topFloor = floor;
	}

	/**
	 * Returns lowest floor of the building.
	 * @return lowest floor info
	 */
	public int getLowestFloor() {
		return lowestFloor;
	}

	/**
	 * Sets lowest floor of the building.
	 * @param floor lowest floor info
	 */
	public void setLowestFloor(int floor) {
		this.lowestFloor = floor;
	}

	/**
	 * Returns the seted name of the building.
	 * @return nm building name
	 */
	public String getNm() {
		return nm;
	}

	/**
	 * Sets name of then selected building
	 * @param nm building name
	 */
	public void setNm(String nm) {
		this.nm = nm;
	}

	/**
	 * Returns the seted address of the building.
	 * @return building address
	 */
	public String getAddr() {
		return addr;
	}

	/**
	 * Sets address of selected building
	 * @param addr building address
	 */
	public void setAddr(String addr) {
		this.addr = addr;
	}

	/**
	 * Returns the seted center longitude of the building.
	 * @return building longitude
	 */
	public double getLon() {
		return lon;
	}

	/**
	 * Sets center longitude of selected building
	 * @param lon building longitude
	 */
	public void setLon(double lon) {
		this.lon = lon;
	}

	/**
	 * Returns the seted center latitude of the building.
	 * @return building latitude
	 */
	public double getLat() {
		return lat;
	}

	/**
	 * Sets center latitude of selected building
	 * @param lat building latitude
	 */
	public void setLat(double lat) {
		this.lat = lat;
	}

	/**
	 * Returns coordinates in text format
	 * @return coordinates
	 */
	public String getLonLatText() {
		StringBuffer sb = new StringBuffer();
		sb.append(lon);
		sb.append(",");
		sb.append(lat);
		return tr(sb.toString());
	}

	/**
	 * Returns number of floors in text format
	 * @return number of floors
	 */
	public String getFloorInfoText() {
		StringBuffer sb = new StringBuffer();
		sb.append(CommonUtil.getFloorFormat(lowestFloor));
		sb.append(" - ");
		sb.append(CommonUtil.getFloorFormat(topFloor));
		return tr(sb.toString());
	}

	/**
	 * Returns whether a floor plan exists
	 * @return true/false
	 */
	public int isExistsFloor(int floor){
		for(int i=0; i<floorList.size(); i++){
			FloorInfo info = floorList.get(i);
			if(info.getFloor() == floor){
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns a floor plan image
	 * @return floor plan image
	 */
	public BufferedImage getFloorPlanImage(int floor){
		for(FloorInfo info : floorList){
			if(info.getFloor() == floor){
				return info.getFloorPlan();
			}
		}
		return null;
	}

	/**
	 * Sets a floor plan image of the floor
	 * @param floor
	 * @param floorPlan
	 */
	public void setFloorPlanInfo(int floor, BufferedImage floorPlan) {
		int index = isExistsFloor(floor);
		if(index < 0){
			FloorInfo info = new FloorInfo(floor, floorPlan);
			floorList.add(info);
		}else{
			floorList.get(index).setFloorPlan(floorPlan);
		}
		Collections.sort(floorList);
	}

	/**
	 * Returns FloorInfo list of the building
	 * @return FloorInfo list
	 */
	public ArrayList<FloorInfo> getFloorList(){
		return floorList;
	}

	public void setTransform(int floor, EastNorth center, double scale, double radian){
		if(null == transform || (floorList.size() == 1 && floorList.get(0).getFloor() == floor)){
			transform = new BuildingTransform(center, scale, radian);
		}
	}

	public BuildingTransform getTransform(){
		return transform;
	}

	/**
	 * Class with floor information of the building
	 */
	public class FloorInfo implements Comparable<FloorInfo>, Serializable {

		private static final long serialVersionUID = -195517217454765982L;

		private int floor;
		private transient BufferedImage floorPlan;

		/**
		 * {@link FloorInfo} 생성자
		 * @param floor
		 * @param floorPlan
		 */
		public FloorInfo(int floor, BufferedImage floorPlan) {
			this.floor = floor;
			this.floorPlan = floorPlan;
		}

		/**
		 * 층 정보를 반환하는 함수
		 * @return 층 정보 반환
		 */
		public int getFloor(){
			return floor;
		}

		/**
		 * 층 정보를 설정하는 함수
		 * @param floor
		 */
		public void setFloor(int floor){
			this.floor = floor;
		}

		/**
		 * 층의 도면 이미지를 반환하는 함수
		 * @return 도면 이미지 반환
		 */
		public BufferedImage getFloorPlan(){
			return floorPlan;
		}

		/**
		 * 층의 도면 이미지를 설정하는 함수
		 * @param floorPlan
		 */
		public void setFloorPlan(BufferedImage floorPlan){
			this.floorPlan = floorPlan;
		}

		/**
		 * 텍스트 형식의 층 정보를 반환하는 함수
		 * @return 층 정보를 반환
		 */
		public String getFloorText(){
			return CommonUtil.getFloorFormat(floor);
		}

		@Override
		public String toString(){
			return CommonUtil.getFloorFormat(floor);
		}

		@Override
		public int compareTo(FloorInfo info) {
			return this.floor < info.getFloor() ? -1 : this.floor > info.getFloor() ? 1 : 0;
		}

		/**
		 * 도면 이미지 객체를 파일로 생성하는 함수
		 * @param filePath
		 */
		public void writeImage(String filePath){
			try {
				File dir = new File(filePath.substring(0, filePath.lastIndexOf("/")+1));
				if(!dir.exists()){
					dir.mkdirs();
				}
				ImageIO.write(floorPlan, "png", new File(filePath));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Class with transform information of the building
	 */
	public class BuildingTransform implements Serializable {

		private static final long serialVersionUID = 5067242958811701314L;

		private EastNorth center;
		private double scale = 1;
		private double radian = 0;

		/**
		 * {@link BuildingTransform} 생성자
		 * @param center
		 * @param scale
		 * @param radian
		 */
		public BuildingTransform(EastNorth center, double scale, double radian){
			this.center = center;
			this.scale = scale;
			this.radian = radian;
		}

		/**
		 * 건물의 센터 정보를 반환하는 함수
		 * @return 센터 값 반환
		 */
		public EastNorth getCenter(){
			return center;
		}

		/**
		 * 건물의 축척 정보를 반환하는 함수
		 * @return 축척 값 반환
		 */
		public double getScale(){
			return scale;
		}

		/**
		 * 건물의 회전 정보를 반환하는 함수
		 * @return 회전 값 반환
		 */
		public double getRadian(){
			return radian;
		}
	}
}
