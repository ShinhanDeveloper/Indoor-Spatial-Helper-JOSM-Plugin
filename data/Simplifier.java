package org.openstreetmap.josm.plugins.indoorSpatialHelper.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;

/**
 * Ramer–Douglas–Peucker 알고지름을 구현한 클래스
 */
public class Simplifier {

	/**
	 * 벡터 단순화 알고리즘을 적용하는 함수
	 * @param nodes
	 * @param epsilon
	 * @param isPolygon
	 * @return 벡터 단순화가 적용된 {@link Node} 리스트
	 */
	public List<Node> simplify(List<Node> nodes, double epsilon, boolean isPolygon) {
		if(isPolygon){
			List<Node> result = simplify(nodes.subList(0, nodes.size()-1), epsilon);
			result.add(nodes.get(nodes.size()-1));
			return result;
		}else{
			return simplify(nodes.subList(0, nodes.size()-1), epsilon);
		}
	}

	/**
	 * 벡터 단순화 알고리즘을 적용하는 함수
	 * @param nodes
	 * @param epsilon
	 * @return 벡터 단순화가 적용된 {@link Node} 리스트
	 * @see Straight
	 */
	public List<Node> simplify(List<Node> nodes, double epsilon) {

		double dmax = 0.0;
		int index = 0;
		Straight line = new Straight(nodes.get(0), nodes.get(nodes.size()-1));
		for (int i=1; i<nodes.size()-1; i++) {
			double d = line.distance(nodes.get(i));
			if(d > dmax ){
				dmax = d;
				index = i;
			}
		}
		if(dmax > epsilon){
			List<Node> reduced1 = simplify(nodes.subList(0, index+1), epsilon);
			List<Node> reduced2 = simplify(nodes.subList(index, nodes.size()), epsilon);
			List<Node> result = new ArrayList<Node>(reduced1);
			result.addAll(reduced2.subList(1, reduced2.size()));
			return result;
		}else{
			return line.asList();
		}
	}

	/**
	 * 벡터의 첫 점과 끝 점을 관리하는 클래스
	 */
	private class Straight {

		private Node start;
		private Node end;

		private double dx;
		private double dy;
		private double sxey;
		private double exsy;
		private double length;

		/**
		 * {@link Straight} 생성자
		 * @param start
		 * @param end
		 */
		public Straight(Node start, Node end) {
			this.start = start;
			this.end = end;
			this.dx = start.getCoor().getX() - end.getCoor().getX();
			this.dy = start.getCoor().getY() - end.getCoor().getY();
			this.sxey = start.getCoor().getX() * end.getCoor().getY();
			this.exsy = end.getCoor().getX() * start.getCoor().getY();
			this.length = Math.sqrt(dx*dx + dy*dy);
		}

		/**
		 * 벡터의 첫 점과 끝 점을 리스트로 반환하는 함수
		 * @return 첫 점과 끝 점 리스트
		 */
		public ArrayList<Node> asList() {
			return new ArrayList<Node>(Arrays.asList(start, end));
		}

		/**
		 * 첫 점과 끝 점을 이은 선형으로 부터 해당 {@link Node}까지의 거리를 반환하는 함수
		 * @param node
		 * @return 해당 노드와의 거리를 반환
		 */
		double distance(Node node) {
			return Math.abs(dy * node.getCoor().getX() - dx * node.getCoor().getY() + sxey - exsy) / length;
		}
	}
}
