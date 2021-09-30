package org.openstreetmap.josm.plugins.indoorSpatialHelper.data;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.IndoorSpatialHelperController;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.DeleteObjectLayerAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.MergeObjectLayerAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.SetFloorHeightAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.ShowAllObjectLayerAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.ShowHideObjectLayerAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.ShowOneObjectLayerOnlyAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.AbstractIndoorObject;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.ComboItem;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

/**
 * 오브젝트 레이어 및 트리를 관리하는 클래스
 * 병합/Split/삭제를 위한 리스트
 */
public class LayerManager extends JTree {

	/**
	 * 오브젝트 레이어 변경 이벤트 처리를 위한 리스너 인터페이스
	 */
	public interface ObjectLayerChangeListener {
		void objectLayerChangeEvent(ObjectLayerChangeEvent e);
	}

	/**
	 * 오브젝트 레이어 변경 이벤트를 구현한 클래스
	 */
	public class ObjectLayerChangeEvent extends EventObject {
		public ObjectLayerChangeEvent(Object source){
			super(source);
		}
	}

	private List<ObjectLayerChangeListener> _listeners = new ArrayList<>();

	/**
	 * {@link ObjectLayerChangeListener}를 리스너 목록에 추가하는 함수
	 * @param listener
	 */
	public synchronized void addObjectLayerChangeListener(ObjectLayerChangeListener listener)  {
		_listeners.add(listener);
	}
	/**
	 * {@link ObjectLayerChangeListener}를 리스너 목록에서 제거하는 함수
	 * @param listener
	 */
	public synchronized void removeObjectLayerChangeListener(ObjectLayerChangeListener listener)   {
		_listeners.remove(listener);
	}

	/**
	 * 등록된 {@link ObjectLayerChangeListener} 리스너의 이벤트를 발생시키는 함수
	 */
	private synchronized void fireObjectLayerChange() {
		ObjectLayerChangeEvent event = new ObjectLayerChangeEvent(this);
		Iterator<ObjectLayerChangeListener> i = _listeners.iterator();
		while(i.hasNext())  {
			i.next().objectLayerChangeEvent(event);
		}
	}

	private VectorLayer layer;
	private LayerTreeModel model;

	/**
	 * {@link LayerManager} 생성자
	 */
	private LayerManager() {
		super();
		model = new LayerTreeModel();
		this.setModel(model);
		this.setCellRenderer(new LayerTreeCellRenderer());
		this.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

		this.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				fireObjectLayerChange();
			}
		});

		this.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseClicked(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				JTree tree = (JTree)e.getSource();
				TreePath path = tree.getPathForLocation(x, y);
				if (path == null){
					return;
				}
				tree.setSelectionPath(path);
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
				if(node.getUserObject() instanceof AbstractIndoorObject){
					if(SwingUtilities.isRightMouseButton(e)){
						JPopupMenu popup = new JPopupMenu();
						popup.add(new ShowAllObjectLayerAction());
						popup.add(new ShowHideObjectLayerAction());
						popup.add(new ShowOneObjectLayerOnlyAction());
						popup.addSeparator();
						popup.add(new SetFloorHeightAction(layer, ((AbstractIndoorObject)node.getUserObject()).getCls()));
						popup.addSeparator();
						popup.add(new MergeObjectLayerAction());
						popup.add(new DeleteObjectLayerAction());
						popup.show(tree, x, y);
					} else if(e.getClickCount() == 2) {
						new ShowHideObjectLayerAction().actionPerformed(null);;
					}
					
				}
				fireObjectLayerChange();
			}
		});
	}

	/**
	 * 싱글톤 처리를 위한 클래스
	 */
	private static class Singleton {
		private static final LayerManager instance = new LayerManager();
	}

	/**
	 * {@link LayerManager} 객체를 반환하는 함수
	 * @return LayerManager 객체 반환
	 */
	public static synchronized LayerManager getInstance() {
		return Singleton.instance;
	}

	/**
	 * 오브젝트 레이어 트리 갱신 및 오브젝트 레이어 변경 이벤트를 발생시키는 함수
	 * @param manager
	 */
	public void updateTree(MainLayerManager manager) {
		if(null != manager && manager.getActiveLayer() instanceof VectorLayer){
			updateTree((VectorLayer)manager.getActiveLayer(), true);
		}else{
			model.clear();
		}
		fireObjectLayerChange();
	}

	/**
	 * 오브젝트 레이어 트리를 갱신하는 함수
	 * @param layer
	 */
	public void updateTree(VectorLayer layer) {
		updateTree(layer, false);
	}

	/**
	 * 오브젝트 레이어 트리를 갱신하는 함수
	 * @param layer
	 * @param isInit
	 */
	public void updateTree(VectorLayer layer, boolean isInit) {
		this.layer = layer;
		model.load(layer, isInit);
	}

	/**
	 * 벡터 레이어를 반환하는 함수
	 * @return 벡터 레이어 반환
	 */
	public VectorLayer getLayer() {
		return layer;
	}

	/**
	 * 벡터 레이어를 생성해 레이어 목록에 추가하는 함수
	 * @return 추가된 벡터 레이어
	 */
	public VectorLayer addVectorLayer() {
		IndoorSpatialHelperController.selectionChangeFlag = false;
		VectorLayer layer = new VectorLayer(new DataSet(), "", null);
		return addVectorLayer(layer, true);
	}

	/**
	 * 벡터 레이어를 레이어 목록에 추가하는 함수
	 * @param layer
	 * @param isActive
	 * @return
	 */
	public VectorLayer addVectorLayer(VectorLayer layer, boolean isActive) {
		IndoorSpatialHelperController.selectionChangeFlag = false;
		MainApplication.getLayerManager().addLayer(layer);
		if(isActive){
			MainApplication.getLayerManager().setActiveLayer(layer);
		}
		IndoorSpatialHelperController.selectionChangeFlag = true;
		return layer;
	}

	/**
	 * 벡터 레이어의 존재 여부를 확인하는 함수
	 * @param osmPrimitiveId
	 * @param floor
	 * @return 벡터 레이어 존재 여부 반환
	 */
	public boolean containsVectorLayer(long osmPrimitiveId, int floor){
		List<Layer> layers = MainApplication.getLayerManager().getLayers();
		for(Layer layer : layers){
			if(layer instanceof VectorLayer){
				if(((VectorLayer)layer).isKey(osmPrimitiveId, floor)){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 벡터 레이어를 반환하는 함수
	 * @param osmPrimitiveId
	 * @param floor
	 * @return 벡터 레이어 반환
	 */
	public VectorLayer getVectorLayer(long osmPrimitiveId, int floor){
		List<Layer> layers = MainApplication.getLayerManager().getLayers();
		for(Layer layer : layers){
			if(layer instanceof VectorLayer){
				if(((VectorLayer)layer).isKey(osmPrimitiveId, floor)){
					return (VectorLayer)layer;
				}
			}
		}
		return null;
	}

	/**
	 * 이미지 레이어를 제거하는 함수
	 */
	public void removeImageLayer() {
		IndoorSpatialHelperController.selectionChangeFlag = false;
		List<Layer> layers = MainApplication.getLayerManager().getLayers();
		for(Layer layer : layers){
			if(layer instanceof ImageLayer){
				MainApplication.getLayerManager().removeLayer(layer);
			}
		}
		IndoorSpatialHelperController.selectionChangeFlag = true;
	}

	/**
	 * 오브젝트 레이어의 해당 오브젝트를 제외한 존재하는 오브젝트 리스트 반환하는 함수
	 * @param cls
	 * @return 오브젝트 리스트 반환
	 */
	public List<ComboItem> getExistsLayer(String cls){
		return model.getExistsLayer(cls);
	}

	/**
	 * 오브젝트 레이어의 존재하지 않는 오브젝트 리스트 반환하는 함수
	 * @return 오브젝트 리스트 반환
	 */
	public List<ComboItem> getNotExistsLayer(){
		return model.getNotExistsLayer();
	}

	/**
	 * 오브젝트 레이어의 레이어 개수를 반환하는 함수
	 * @return 레이어 개수 반환
	 */
	public int getLayerCount(){
		return model.getLayerCount();
	}

	/**
	 * 오브젝트 레이어 트리의 트리 모델을 구현한 클래스
	 */
	class LayerTreeModel extends DefaultTreeModel {

		DefaultMutableTreeNode rootNode = null;
		LayerObjectCounter counter = null;

		/**
		 * {@link LayerTreeModel} 생성자
		 */
		public LayerTreeModel() {
			super(new DefaultMutableTreeNode(""));
			rootNode = (DefaultMutableTreeNode) getRoot();
		}

		/**
		 * 레이어 트리를 갱신하는 함수
		 * @param layer
		 * @param isInit
		 */
		public void load(VectorLayer layer, boolean isInit) {
			rootNode.removeAllChildren();
			rootNode.setUserObject(layer.getName());

			counter = new LayerObjectCounter();
			DataSet ds = isInit ? layer.getRealDataSet() : layer.getDataSet();
			if(null != ds){
				for(Way way : ds.getWays()){
					if(!way.isDeleted()){
						counter.addCount(way.get(Constants.INDOOR_OBJECT_KEY));
					}
				}
				if(!isInit){
					for(Way way : layer.getRealDataSet().getWays()){
						String cls = AbstractIndoorObject.getIndoorObjectCode(way.get(Constants.INDOOR_OBJECT_KEY));
						if(!way.isDeleted() && !layer.getVisible(cls)){
							counter.addCount(cls);
						}
					}
				}
			}
			makeTreeNode(layer, counter);
		}

		/**
		 * 레이어 트리를 지우는 함수
		 */
		public void clear() {
			rootNode.removeAllChildren();
			rootNode.setUserObject("");
			counter = new LayerObjectCounter();
			nodeStructureChanged(rootNode);
		}

		/**
		 * 트리의 루트 노드를 반환하는 함수
		 * @return 루트 노드 반환
		 */
		public TreeNode getRootNode() {
			return rootNode;
		}

		/**
		 * 레이어 트리를 갱신하는 함수
		 * @param layer
		 * @param counter
		 */
		public void makeTreeNode(VectorLayer layer, LayerObjectCounter counter) {
			DefaultMutableTreeNode childNode = null;
			Set<Entry<String, Integer>> set = counter.getEntries();
			for(Entry<String, Integer> entry : set){
				String cls = entry.getKey();
				childNode = new DefaultMutableTreeNode(AbstractIndoorObject.getIndoorObject(cls, entry.getValue(), layer.getVisible(cls)));
				rootNode.add(childNode);
			}
			nodeStructureChanged(rootNode);
		}

		/**
		 * 오브젝트 레이어의 해당 오브젝트를 제외한 존재하는 오브젝트 리스트 반환하는 함수
		 * @param cls
		 * @return 오브젝트 리스트 반환
		 */
		public List<ComboItem> getExistsLayer(String cls){
			ArrayList<ComboItem> list = new ArrayList<>();

			for(int i=0; i<rootNode.getChildCount(); i++){
				Object o = ((DefaultMutableTreeNode)rootNode.getChildAt(i)).getUserObject();
				if(o instanceof AbstractIndoorObject && !((AbstractIndoorObject)o).getCls().equals(cls)){
					for(String[] obj : Constants.ALL_INDOOR_OBJECTS){
						if(((AbstractIndoorObject)o).getCls().equals(obj[0])){
							ComboItem item = new ComboItem(obj[0], obj[1] + " layer");
							list.add(item);
							continue;
						}
					}
				}
			}
			return list;
		}

		/**
		 * 오브젝트 레이어의 존재하지 않는 오브젝트 리스트 반환하는 함수
		 * @return 오브젝트 리스트 반환
		 */
		public List<ComboItem> getNotExistsLayer(){
			ArrayList<ComboItem> list = new ArrayList<>();
			for(String[] obj : Constants.ALL_INDOOR_OBJECTS){
				boolean isContinue = false;
				for(int i=0; i<rootNode.getChildCount(); i++){
					Object o = ((DefaultMutableTreeNode)rootNode.getChildAt(i)).getUserObject();
					if(o instanceof AbstractIndoorObject && ((AbstractIndoorObject)o).getCls().equals(obj[0])){
						isContinue = true;
						continue;
					}
				}
				if(isContinue){
					continue;
				}
				ComboItem item = new ComboItem(obj[0], obj[1] + " layer");
				list.add(item);
			}
			return list;
		}

		/**
		 * 오브젝트 레이어의 레이어 개수를 반환하는 함수
		 * @return 레이어 개수 반환
		 */
		public int getLayerCount(){
			return null == counter ? 0 : counter.getSize();
		}
	}

	/**
	 * 오브젝트 레이어 트리의 트리 셀 렌더링을 구현한 클래스
	 */
	class LayerTreeCellRenderer extends JLabel implements TreeCellRenderer {

		/**
		 * {@link LayerTreeCellRenderer} 생성자
		 */
		public LayerTreeCellRenderer() {
			setPreferredSize(new Dimension(300, 20));
			setOpaque(true);
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			Object obj = ((DefaultMutableTreeNode) value).getUserObject();
			if(obj instanceof AbstractIndoorObject){
				AbstractIndoorObject indoorObj = (AbstractIndoorObject)obj;
				setIcon(indoorObj.getIcon());
				setText(indoorObj.getString());
			}else{
				Icon icon = "".equals(obj) ? null : Constants.VECTOR_LAYER_ICON;
				setIcon(icon);
				setText("" + value);
			}

			Font font = this.getFont();
			if(selected){
				setFont(font.deriveFont(font.getStyle() | Font.BOLD));
				setBackground(Color.blue);
				setForeground(Color.white);
			}else{
				setFont(font.deriveFont(font.getStyle() & ~Font.BOLD));
				setBackground(Color.white);
				setForeground(Color.black);
			}
			return this;
		}
	}

	/**
	 * 오브젝트 레이어의 개수를 관리하는 클래스
	 */
	class LayerObjectCounter {

		Map<String, Integer> cntMap = new HashMap<>();

		/**
		 * 오브젝트의 개수를 증가시키는 함수
		 * @param cls
		 */
		public void addCount(String cls){
			cls = AbstractIndoorObject.getIndoorObjectCode(cls);
			if(cntMap.containsKey(cls)){
				cntMap.put(cls, cntMap.get(cls)+1);
			}else{
				cntMap.put(cls, 1);
			}
		}

		/**
		 * 오브젝트의 개수를 반환하는 함수
		 * @param cls
		 * @return 개수 반환
		 */
		public int getCount(String cls){
			int cnt = 0;
			cls = AbstractIndoorObject.getIndoorObjectCode(cls);
			if(cntMap.containsKey(cls)){
				cnt = cntMap.get(cls);
			}
			return cnt;
		}

		/**
		 * 오브젝트 개수를 관리하는 Set 객체 반환
		 * @return Set 객체 반환
		 */
		public Set<Entry<String, Integer>> getEntries() {
			TreeMap<String, Integer> map = new TreeMap<String, Integer>(cntMap);
			return map.entrySet();
		}

		/**
		 * 오브젝트 종류의 개수를 반환하는 함수
		 * @return 오브젝트 종류의 개수 반환
		 */
		public int getSize(){
			return cntMap.size();
		}
	}
}
