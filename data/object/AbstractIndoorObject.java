package org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.CommonUtil;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

/**
 * Indoor 오브젝트 관리를 위한 클래스
 */
public abstract class AbstractIndoorObject {

	protected final String cls;
	protected final String name;
	protected final int cnt;

	protected boolean isShow = true;

	private static final String[] ATTRIBUTES = {"height"};

	/**
	 * {@link AbstractIndoorObject} 생성자
	 * @param cls
	 * @param name
	 * @param cnt
	 */
	public AbstractIndoorObject(String cls, String name, int cnt) {
		this.cls = cls;
		this.name = name;
		this.cnt = cnt;
	}

	/**
	 * Indoor 오브젝트의 코드를 반환하는 함수
	 * @return Indoor 오브젝트 코드 반환
	 */
	public String getCls() {
		return cls;
	}

	/**
	 * Indoor 오브젝트의 명칭을 반환하는 함수
	 * @return Indoor 오브젝트 명칭 반환
	 */
	public String getName() {
		return name;
	}

	/**
	 * 텍스트 형식의 Indoor 오브젝트의 명칭을 반환하는 함수
	 * @return Indoor 오브젝트 명칭 반환
	 */
	public String getString() {
		return name + "(" + cnt + ")";
	}

	/**
	 * Indoor 오브젝트의 이미지 아이콘을 반환하는 함수
	 * @return Indoor 오브젝트 이미지 아이콘 반환
	 */
	public ImageIcon getIcon() {
		return isShow ? Constants.SHOW_ICON : Constants.HIDE_ICON;
	}

	/**
	 * Indoor 오브젝트의 가시화 여부를 반환하는 함수
	 * @return Indoor 오브젝트 가시화 여부 반환
	 */
	public boolean getVisible() {
		return isShow;
	}

	/**
	 * Indoor 오브젝트의 가시화 여부를 변경 및 반환하는 함수
	 * @return Indoor 오브젝트 가시화 여부 반환
	 */
	public boolean setVisible() {
		isShow = !isShow;
		return isShow;
	}

	/**
	 * Indoor 오브젝트의 가시화 여부를 설정하는 함수
	 * @param isShow
	 */
	public void setVisible(boolean isShow) {
		this.isShow = isShow;
	}

	/**
	 * Indoor 오브젝트의 속성 정보를 반환하는 함수
	 * @return Indoor 오브젝트 속성 정보 반환
	 */
	public abstract String[] getObjAttributes();

	/**
	 * Indoor 오브젝트의 코드 여부를 반환하는 함수
	 * @param cls
	 * @return Indoor 오브젝트 코드 여부 반환
	 */
	public static boolean isIndoorObject(String cls) {
		if(null != cls){
			for(String[] c : Constants.INDOOR_OBJECTS){
				if(cls.equals(c[0])){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Indoor 오브젝트의 코드를 유효성 체크 후 반환하는 함수
	 * @param cls
	 * @return Indoor 오브젝트 코드 반환
	 */
	public static String getIndoorObjectCode(String cls) {
		if(!isIndoorObject(cls)){
			return Constants.ETC_CLASS_CODE[0];
		}
		return cls;
	}

	/**
	 * Indoor 오브젝트 별 오브젝트 객체를 생성 후 반환하는 함수
	 * @param cls
	 * @param cnt
	 * @param isVisible
	 * @return Indoor 오브젝트 객체 반환
	 */
	public static AbstractIndoorObject getIndoorObject(String cls, int cnt, boolean isVisible) {
		AbstractIndoorObject indoorObj = null;
		if(isIndoorObject(cls)){
			if(Constants.WALL_CLASS_CODE[0].equals(cls)){
				indoorObj = new Wall(cnt);
			}else if(Constants.WINDOW_CLASS_CODE[0].equals(cls)){
				indoorObj = new Window(cnt);
			}else if(Constants.DOOR_CLASS_CODE[0].equals(cls)){
				indoorObj = new Door(cnt);
			}else if(Constants.STAIR_CLASS_CODE[0].equals(cls)){
				indoorObj = new Stair(cnt);
			}else if(Constants.LIFT_CLASS_CODE[0].equals(cls)){
				indoorObj = new Lift(cnt);
			}else if(Constants.SPACE_CLASS_CODE[0].equals(cls)){
				indoorObj = new Space(cnt);
			}else if(Constants.SUBSPACE_CLASS_CODE[0].equals(cls)){
				indoorObj = new Subspace(cnt);
			}
		}else{
			indoorObj = new Etc(cnt);
		}
		indoorObj.setVisible(isVisible);
		return indoorObj;
	}

	/**
	 * Indoor 오브젝트 별 오브젝트의 속성 정보 배열을 반환하는 함수
	 * @param cls
	 * @return 오브젝트 속성 정보 반환
	 */
	public static String[] getAttributes(String cls){
		if(isIndoorObject(cls)){
			if(Constants.WALL_CLASS_CODE[0].equals(cls)){
				return CommonUtil.concat(ATTRIBUTES, Wall.ATTRIBUTES);
			}else if(Constants.WINDOW_CLASS_CODE[0].equals(cls)){
				return CommonUtil.concat(ATTRIBUTES, Window.ATTRIBUTES);
			}else if(Constants.DOOR_CLASS_CODE[0].equals(cls)){
				return CommonUtil.concat(ATTRIBUTES, Door.ATTRIBUTES);
			}else if(Constants.STAIR_CLASS_CODE[0].equals(cls)){
				return CommonUtil.concat(ATTRIBUTES, Stair.ATTRIBUTES);
			}else if(Constants.LIFT_CLASS_CODE[0].equals(cls)){
				return CommonUtil.concat(ATTRIBUTES, Lift.ATTRIBUTES);
			}else if(Constants.SPACE_CLASS_CODE[0].equals(cls)){
				return CommonUtil.concat(ATTRIBUTES, Space.ATTRIBUTES);
			}else if(Constants.SUBSPACE_CLASS_CODE[0].equals(cls)){
				return CommonUtil.concat(ATTRIBUTES, Subspace.ATTRIBUTES);
			}
		}else{
			return CommonUtil.concat(ATTRIBUTES, Etc.ATTRIBUTES);
		}
		return ATTRIBUTES;
	}

	/**
	 * Indoor 오브젝트 별 오브젝트의 속성 정보를 Map으로 반환하는 함수
	 * @param cls
	 * @return 속성 정보 맵 객체 반환
	 */
	public static Map<String, String> getTagMap(String cls) {
		Map<String, String> tagMap = new HashMap<>();
		tagMap.put(Constants.INDOOR_OBJECT_KEY, cls);
		for(String key : getAttributes(cls)){
			tagMap.put(key, "");
		}
		return tagMap;
	}

	/**
	 * Indoor 오브젝트 별 오브젝트의 속성 정보를 Map으로 반환하는 함수
	 * @param cls
	 * @param keys
	 * @return 속성 정보 맵 객체 반환
	 */
	public static Map<String, String> getTagMap(String cls, Map<String, String> keys) {
		Map<String, String> tagMap = new HashMap<>(keys);
		tagMap.put(Constants.INDOOR_OBJECT_KEY, cls);
		for(String key : getAttributes(cls)){
			if(keys.containsKey(key)){
				tagMap.put(key, keys.get(key));
			}else{
				tagMap.put(key, "");
			}
		}
		return tagMap;
	}
}
