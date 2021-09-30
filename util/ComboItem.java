package org.openstreetmap.josm.plugins.indoorSpatialHelper.util;

/**
 * 콤보박스 콤보아이템 객체를 구현한 클래스
 */
public class ComboItem {
	private String key;
	private String value;
	public int height;

	/**
	 * {@link ComboItem}의 생성자
	 * @param key
	 * @param value
	 */
	public ComboItem(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public ComboItem(String key, String value, int height) {
		this.key = key;
		this.value = value;
		this.height = height;
	}

	/**
	 * 콤보아이템의 키를 반환하는 함수
	 * @return 키를 반환
	 */
	public String getKey() {
		return key;
	}

	/**
	 * 콤보아이템의 값을 반환하는 함수
	 * @return 값을 반환
	 */
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}
	
}
