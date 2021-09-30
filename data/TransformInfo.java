package org.openstreetmap.josm.plugins.indoorSpatialHelper.data;

/**
 * 원본 이미지와 변환된 이미지 해상도를 관리하는 클래스
 */
public class TransformInfo {

	private int originalW;
	private int originalH;
	private int transformW;
	private int transformH;

	/**
	 * {@link TransformInfo} 생성자
	 * @param ow
	 * @param oh
	 * @param tw
	 * @param th
	 */
	public TransformInfo(int ow, int oh, int tw, int th) {
		originalW = ow;
		originalH = oh;
		transformW = tw;
		transformH = th;
	}

	/**
	 * ML 모듈의 변환된 해상도를 설정하는 클래스
	 * @param tw
	 * @param th
	 */
	public void setTransForm(int tw, int th) {
		transformW = tw;
		transformH = th;
	}

	/**
	 * 원본 이미지와 변환된 이미지의 해상도가 같은지 확인하는 함수
	 * @return 해상도 변경 여부 반환
	 */
	public boolean isNeedTransForm() {
		return !(originalW == transformW && originalH == transformH);
	}

	/**
	 * 변환된 이미지 대비 원본 이미지의 너비 비율을 반환하는 함수
	 * @return 변환된 이미지 대비 원본 이미지의 너비 비율 반환
	 */
	public double getWidthRatio() {
		return (double)originalW / (double)transformW;
	}

	/**
	 * 변환된 이미지 대비 원본 이미지의 높이 비율을 반환하는 함수
	 * @return 변환된 이미지 대비 원본 이미지의 높이 비율 반환
	 */
	public double getHeightRatio() {
		return (double)originalH / (double)transformH;
	}

	/**
	 * 원본 이미지의 너비를 반환하는 함수
	 * @return 원본 이미지의 너비 반환
	 */
	public int getOriginalW() {
		return originalW;
	}

	/**
	 * 원본 이미지의 높이를 반환하는 함수
	 * @return 원본 이미지의 높이 반환
	 */
	public int getOriginalH() {
		return originalH;
	}

	/**
	 * 변환된 이미지의 너비를 반환하는 함수
	 * @return 변환된 이미지의 너비 반환
	 */
	public int getTransformW() {
		return transformW;
	}

	/**
	 * 변환된 이미지의 높이를 반환하는 함수
	 * @return 변환된 이미지의 높이 반환
	 */
	public int getTransformH() {
		return transformH;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("originalWith["+originalW+"]\n");
		sb.append("originalHeight["+originalH+"]\n");
		sb.append("transformWodth["+transformW+"]\n");
		sb.append("transformHeight["+transformH+"]\n");
		return sb.toString();
	}
}
