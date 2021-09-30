package org.openstreetmap.josm.plugins.indoorSpatialHelper.data;

import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.BuildingInfo.BuildingTransform;

/**
 * 건물 정보 객체를 관리하는 매니저 클래스
 * @see BuildingInfo
 */
public class BuildingManager implements Cloneable, Serializable {

	private static final long serialVersionUID = 2507513569375475284L;

	private Map<Long, BuildingInfo> map;

	/**
	 * {@link BuildingManager} 생성자
	 */
	public BuildingManager() {
		map = new HashMap<Long, BuildingInfo>();
	}

	/**
	 * Osm Primitive 데이터의 건물 정보 객체를 설정하는 함수
	 * @param osmPrimitive
	 * @param info
	 */
	public void setBuildingInfo(OsmPrimitive osmPrimitive, BuildingInfo info) {
		if(null != info){
			map.put(osmPrimitive.getId(), info);
		}
	}
	

	/**
	 * Osm Primitive 데이터의 건물 정보를 반환하는 함수
	 * @param osmPrimitiveId
	 * @return 건물 정보 객체 반환
	 */
	public BuildingInfo getBuildingInfo(long osmPrimitiveId) {
		if(map.containsKey(osmPrimitiveId)){
			return map.get(osmPrimitiveId);
		}
		return null;
	}
	/**
	 * Osm Primitive 데이터의 건물 정보를 반환하는 함수
	 * @param osmPrimitive
	 * @return 건물 정보 객체 반환
	 */
	public BuildingInfo getBuildingInfo(OsmPrimitive osmPrimitive) {
		if(null != osmPrimitive){
			if(map.containsKey(osmPrimitive.getId())){
				return map.get(osmPrimitive.getId());
			}
		}
		return null;
	}

	/**
	 * Osm Primitive 데이터의 건물 정보를 반환하는 함수
	 * @param osmPrimitive
	 * @return 건물 정보 객체 반환
	 */
	public BuildingInfo getBuildingInfoObj(OsmPrimitive osmPrimitive) {
		if(map.containsKey(osmPrimitive.getId())){
			return map.get(osmPrimitive.getId());
		}
		return new BuildingInfo(osmPrimitive);
	}

	/**
	 * Osm Primitive 데이터의 건물 정보가 존재하는지 확인하는 함수
	 * @param osmPrimitive
	 * @return 건물 정보 존재 여부 반환
	 */
	public boolean contains(OsmPrimitive osmPrimitive) {
		return containsKey(osmPrimitive.getId());
	}

	/**
	 * Osm Primitive 데이터의 건물 정보가 존재하는지 확인하는 함수
	 * @param key
	 * @return 건물 정보 존재 여부 반환
	 */
	public boolean containsKey(long key) {
		return map.containsKey(key);
	}

	/**
	 * 건물 정보 객체를 기반으로 Osm Primitive 데이터의 키 값을 반환
	 * @param info
	 * @return Osm Primitive 키 반환
	 */
	public long getOsmIdByBuildingInfo(BuildingInfo info) {
		for (Entry<Long, BuildingInfo> entry : map.entrySet()) {
			if (Objects.equals(info, entry.getValue())) {
				return entry.getKey();
			}
		}
		return 0;
	}

	/**
	 * Osm Primitive 데이터의 건물 정보 객체에 층별 도면 이미지를 설정하는 함수
	 * @param osmPrimitiveId
	 * @param floor
	 * @param floorPlan
	 * @return 건물 정보 객체 반환
	 */
	public BuildingInfo setFloorPlan(long osmPrimitiveId, int floor, BufferedImage floorPlan) {
		BuildingInfo info = map.get(osmPrimitiveId);
		if(null != info){
			info.setFloorPlanInfo(floor, floorPlan);
		}
		map.put(osmPrimitiveId, info);
		return info;
	}

	/**
	 * Osm Primitive 데이터의 건물 정보 객체에 좌표 변환 정보를 설정하는 함수
	 * @param osmPrimitiveId
	 * @param floor
	 * @param center
	 * @param scale
	 * @param radian
	 */
	public void setTransform(long osmPrimitiveId, int floor, EastNorth center, double scale, double radian){
		if(map.containsKey(osmPrimitiveId)){
			map.get(osmPrimitiveId).setTransform(floor, center, scale, radian);
		}
	}

	/**
	 * Osm Primitive 데이터의 건물 정보 객체에 좌표 변환 정보를 설정하는 함수
	 * @param osmPrimitiveId
	 * @return 건물 좌표 변환 정보 객체를 반환
	 */
	public BuildingTransform getTransform(long osmPrimitiveId){
		if(map.containsKey(osmPrimitiveId)){
			return map.get(osmPrimitiveId).getTransform();
		}
		return null;
	}

	/**
	 * 건물 정보 객체를 관리하는 map 객체를 반환
	 * @return map 객체 반환
	 */
	public Map<Long, BuildingInfo> getBuildingInfoMap() {
		return map;
	}

	@Override
	public BuildingManager clone(){
		BuildingManager b = null;
		try{
			b = (BuildingManager)super.clone();
		}catch(Exception e){
			return null;
		}
		return b;
	}
}
