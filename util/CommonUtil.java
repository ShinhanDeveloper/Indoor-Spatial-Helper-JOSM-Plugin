package org.openstreetmap.josm.plugins.indoorSpatialHelper.util;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * 공통 유틸 함수를 모아놓은 클래스
 */
public class CommonUtil {

	/**
	 * null 값을 치환하는 함수
	 * @param str
	 * @return null 값을 치환한 값
	 */
	public static String nvl(String str){
		return nvl(str, "");
	}

	/**
	 * null 또는 공백값을 지정된 문자로 치환하는 함수
	 * @param str
	 * @param value
	 * @return null 또는 공백 값을 치환한 값
	 */
	public static String nvl(String str, String value){
		return (null == str || "".equals(str)) ? value : str;
	}

	/**
	 * 화면의 사이즈를 반환하는 함수
	 * @return 화면 사이즈를 반환
	 */
	public static Dimension getScreenSize() {
		return getScreenSize(100);
	}

	/**
	 * 화면 사이즈의 지정된 비율의 사이즈를 반환하는 함수
	 * @param percent
	 * @return 비율이 적용된 사이즈를 반환
	 */
	public static Dimension getScreenSize(int percent) {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		if(percent <= 0 || percent > 100){
			percent = 100;
		}
		return new Dimension((int)(dim.getWidth() * percent / 100), (int)(dim.getHeight() * percent / 100));
	}

	/**
	 * 화면 사이즈 기준 여백 값을 포함한 최대 사이즈를 반환하는 함수
	 * @param width
	 * @param height
	 * @param margin
	 * @return 화면사이즈 및 여백 사이즈가 고려된 사이즈 반환
	 */
	public static Dimension getMaximumDim(int width, int height, int margin) {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		if(dim.getWidth() < width + margin){
			width = (int) dim.getWidth() - margin;
		}
		if(dim.getHeight() < height + margin){
			height = (int) dim.getHeight() - margin;
		}
		return new Dimension(width, height);
	}

	/**
	 * 화면 사이즈 비율에 따른 해당 사이즈가 위치할 좌표값을 반환하는 함수
	 * @param d
	 * @return 사이즈가 위치할 좌표값 반환
	 */
	public static Point getPosition(Dimension d) {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int top = 0;
		int left = 0;
		if(dim.getWidth() > d.getWidth()) {
			left = (int)(dim.getWidth() - d.getWidth()) / 2;
		}
		if(dim.getHeight() > d.getHeight()) {
			top = (int)(dim.getHeight() - d.getHeight()) / 2;
		}
		return new Point(left, top);
	}

	/**
	 * The function returns floor information to text format.
	 * @param floor
	 * @return Returns floor information in text format
	 */
	public static String getFloorFormat(int floor) {
		String floorStr = "1F";
		if(floor < 0){
			floorStr = "B" + (floor * -1);
		} else {
			floorStr = floor + "F";
		}
		return tr(floorStr);
	}

	/**
	 * 텍스트의 정수 여부를 반환하는 함수
	 * @param str
	 * @return 정수 여부
	 */
	public static boolean isInteger(String str) {
		try{
			Integer.parseInt(str);
		}catch(NumberFormatException e){
			return false;
		}
		return true;
	}

	/**
	 * 텍스트의 실수 여부를 반환하는 함수
	 * @param str
	 * @return 실수 여부
	 */
	public static boolean isRealNumber(String str) {
		try{
			Double.parseDouble(str);
		}catch(NumberFormatException e){
			return false;
		}
		return true;
	}

	/**
	 * 텍스트의 양수 여부를 반환하는 함수
	 * @param str
	 * @return 양수 여부
	 */
	public static boolean isPositiveNumber(String str) {
		try{
			if(Double.parseDouble(str) < 0){
				return false;
			}
		}catch(NumberFormatException e){
			return false;
		}
		return true;
	}

	/**
	 * OSM Primitive의 bbox 2배 영역 boundary를 반환하는 함수
	 * @param osmPrimitive
	 * @return boundary를 반환
	 */
	public static Bounds getBoundary(OsmPrimitive osmPrimitive) {
		BBox bbox = osmPrimitive.getBBox();
		double addx = bbox.getCenter().getX() - bbox.getTopLeftLon();
		double addy = bbox.getCenter().getY() - bbox.getBottomRightLat();
		return new Bounds(bbox.getBottomRightLat()- addy, bbox.getTopLeftLon() - addx, bbox.getTopLeftLat() + addy, bbox.getBottomRightLon() + addx);
	}

	/**
	 * OSM Primitive 집합의 boundary를 반환하는 함수
	 * @param osmPrimitives
	 * @return OSM Primitive 집합의 boundary를 반환
	 */
	public static Bounds getBoundary(Collection<OsmPrimitive> osmPrimitives) {
		if(osmPrimitives.size() > 0){
			double minLat = Double.MAX_VALUE;
			double minLon = Double.MAX_VALUE;
			double maxLat = Double.MIN_VALUE;
			double maxLon = Double.MIN_VALUE;
			for(OsmPrimitive o : osmPrimitives){
				BBox bbox = o.getBBox();
				minLat = Math.min(minLat, bbox.getBottomRightLat());
				minLon = Math.min(minLon, bbox.getTopLeftLon());
				maxLat = Math.max(maxLat, bbox.getTopLeftLat());
				maxLon = Math.max(maxLon, bbox.getBottomRightLon());
			}
			Bounds b = new Bounds(minLat, minLon, maxLat, maxLon);
			double addx = b.getCenter().getX() - b.getMinLon();
			double addy = b.getCenter().getY() - b.getMinLat();
			return new Bounds(b.getMinLat()- addy, b.getMinLon() - addx, b.getMaxLat() + addy, b.getMaxLon() + addx);
		}
		return null;
	}

	/**
	 * 배열을 병합하는 함수
	 * @param first
	 * @param second
	 * @return 병합된 배열을 반환
	 */
	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	/**
	 * 파일 directory를 삭제하는 함수
	 * @param directory
	 */
	public static void deleteDirectory(String directory){
		deleteDirectory(new File(directory));
	}

	/**
	 * 파일 directory를 삭제하는 함수
	 * @param dir
	 */
	public static void deleteDirectory(File dir){
		if(!dir.exists()){
			return;
		}
		if(dir.isDirectory()){
			for(File f : dir.listFiles()){
				deleteDirectory(f);
			}
		}
		dir.delete();
	}
}
