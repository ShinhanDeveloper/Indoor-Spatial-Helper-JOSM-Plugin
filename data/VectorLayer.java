package org.openstreetmap.josm.plugins.indoorSpatialHelper.data;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;

import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.UndoRedoHandler.CommandAddedEvent;
import org.openstreetmap.josm.data.UndoRedoHandler.CommandQueueCleanedEvent;
import org.openstreetmap.josm.data.UndoRedoHandler.CommandQueuePreciseListener;
import org.openstreetmap.josm.data.UndoRedoHandler.CommandRedoneEvent;
import org.openstreetmap.josm.data.UndoRedoHandler.CommandUndoneEvent;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.IndoorSpatialHelperController;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.Josm3DViewer.Josm3DViewerLauncher;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.ExportGmlAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.ExportIndoorGmlAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.SetFloorHeightAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.ShowFloorPlanAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.ZoomToLayerAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.object.AbstractIndoorObject;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.CommonUtil;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

/**
 * 실내공간 벡터 데이터를 관리하는 레이어 클래스
 */
public class VectorLayer extends OsmDataLayer implements Cloneable, CommandQueuePreciseListener, Serializable {

	private static final long serialVersionUID = -5691247479686947697L;

	private long osmPrimitiveId;
	private int floor;
	private int height = 2500;

	private boolean isEventPrevent = true;

	private transient DataSet realDataSet;
	private Map<String, Boolean> visibleMap = new HashMap<>();

	/**
	 * {@link VectorLayer} 생성자
	 * @param data
	 * @param name
	 * @param osmPrimitiveId
	 * @param floor
	 */
	public VectorLayer(DataSet data, String name, long osmPrimitiveId, int floor){
		this(data, name, null);
		this.osmPrimitiveId = osmPrimitiveId;
		this.floor = floor;
	}

	/**
	 * {@link VectorLayer} 생성자
	 * @param data
	 * @param name
	 * @param associatedFile
	 */
	public VectorLayer(DataSet data, String name, File associatedFile){
		super(data, name, associatedFile);
		//realDataSet = new DataSet(data);
		List<Layer> layers = MainApplication.getLayerManager().getLayers();
		for(Layer layer : layers) {
			if(layer instanceof ImageLayer) {
				osmPrimitiveId = ((ImageLayer)layer).getOsmPrimitiveId();
				floor = ((ImageLayer)layer).getFloor();
				StringBuffer sb = new StringBuffer();
				sb.append(IndoorSpatialHelperController.getBuildingManager().getBuildingInfo(osmPrimitiveId).getNm());
				sb.append("(");
				sb.append(CommonUtil.getFloorFormat(floor));
				sb.append(")");
				setName(sb.toString());
				break;
			}
		}

		UndoRedoHandler.getInstance().addCommandQueuePreciseListener(this);

		this.getDataSet().addDataSetListener(new DataSetListener() {

			@Override
			public void wayNodesChanged(WayNodesChangedEvent event) {
				//System.out.println("############### VectorLayer wayNodesChanged");
			}

			@Override
			public void tagsChanged(TagsChangedEvent event) {
				System.out.println("############### VectorLayer tagsChanged");
				if(null != realDataSet && !isEventPrevent){
					OsmPrimitive p = event.getPrimitive();
				
					if(p.hasKey(Constants.INDOOR_OBJECT_KEY)){
						String oldValue = CommonUtil.nvl(event.getOriginalKeys().get(Constants.INDOOR_OBJECT_KEY));
						String newValue = p.get(Constants.INDOOR_OBJECT_KEY);
						if(!newValue.equals(oldValue)){
							isEventPrevent = true;
							p.getDataSet().getReadLock().unlock();
							p.setKeys(AbstractIndoorObject.getTagMap(p.get(Constants.INDOOR_OBJECT_KEY), p.getKeys()));
							p.getDataSet().getReadLock().lock();
							isEventPrevent = false;
						}
						
					}
					LayerManager.getInstance().updateTree(VectorLayer.this);
				}
			}

			@Override
			public void relationMembersChanged(RelationMembersChangedEvent event) {
				System.out.println("############### VectorLayer relationMembersChanged");
			}

			@Override
			public void primitivesRemoved(PrimitivesRemovedEvent event) {
				System.out.println("############### VectorLayer primitivesRemoved");
				if(null != realDataSet && !isEventPrevent){
					LayerManager.getInstance().updateTree(VectorLayer.this);
				}
			}

			@Override
			public void primitivesAdded(PrimitivesAddedEvent event) {
				System.out.println("############### VectorLayer primitivesAdded");
				if(null != realDataSet && !isEventPrevent){
					LayerManager.getInstance().updateTree(VectorLayer.this);
				}
			}

			@Override
			public void otherDatasetChange(AbstractDatasetChangedEvent event) {
				System.out.println("############### VectorLayer otherDatasetChange");
			}

			@Override
			public void nodeMoved(NodeMovedEvent event) {
				
				System.out.println("############### VectorLayer nodeMoved");
			}

			@Override
			public void dataChanged(DataChangedEvent event) {
				System.out.println("############### VectorLayer dataChanged");
			}
		});
		this.getDataSet().addSelectionListener(new DataSelectionListener() {
			@Override
			public void selectionChanged(SelectionChangeEvent event) {}
		});
	}


	/**
	 * Osm 데이터의 키 값을 반환하는 함수
	 * @return Osm 데이터 키 반환
	 */
	public long getOsmPrimitiveId() {
		return osmPrimitiveId;
	}

	/**
	 * 벡터 레이어의 층 정보를 반환하는 함수
	 * @return 층 정보를 반환
	 */
	public int getFloor() {
		return floor;
	}
	
	public void setHeight(int height)
	{
		this.height = height;
	}
	
	public int getHeight()
	{
		return this.height;
	}

	/**
	 * 파라미터와 벡터 레이어의 키 값이 일치하는지 반환하는 함수
	 * @param osmPrimitiveId
	 * @param floor
	 * @return 벡터 레이어의 키 여부 반환
	 */
	public boolean isKey(long osmPrimitiveId, int floor){
		return this.osmPrimitiveId == osmPrimitiveId && this.floor == floor;
	}

	@Override
	public Action[] getMenuEntries() {
		syncDataSet();
		List<Action> actions = new ArrayList<>();
		actions.add(LayerListDialog.getInstance().createActivateLayerAction(this));
		actions.add(LayerListDialog.getInstance().createShowHideLayerAction());
		actions.add(LayerListDialog.getInstance().createDeleteLayerAction());
		actions.add(SeparatorLayerAction.INSTANCE);
		actions.add(new ZoomToLayerAction(this));
		actions.add(new SetFloorHeightAction(this));
		actions.add(SeparatorLayerAction.INSTANCE);
		//actions.add(LayerListDialog.getInstance().createMergeLayerAction(this));
		//actions.add(LayerListDialog.getInstance().createDuplicateLayerAction(this));
		actions.add(new LayerSaveAction(this));
		actions.add(new LayerSaveAsAction(this));
		actions.add(new ExportGmlAction(this));
		actions.add(new ExportIndoorGmlAction(this));
		actions.add(SeparatorLayerAction.INSTANCE);
		actions.add(new RenameLayerAction(getAssociatedFile(), this));
		actions.add(SeparatorLayerAction.INSTANCE);
		actions.add(new ShowFloorPlanAction(this));
		actions.add(new LayerListPopup.InfoAction(this));
		return actions.toArray(new Action[0]);
	}

	@Override
	public Icon getIcon() {
		return Constants.VECTOR_LAYER_ICON;
	}

	@Override
	public boolean isSavable() {
		return true;
	}

	@Override
	public boolean isUploadable() {
		return false;
	}

	@Override
	public boolean requiresSaveToFile() {
		return false;
	}

	/**
	 * 벡터 레이어의 실제 {@link DataSet}을 초기화 하는 함수
	 */
	public void initDataSet() {
		realDataSet = new DataSet(data);
		LayerManager.getInstance().updateTree(this);
		isEventPrevent = false;
	}

	/**
	 * 벡터 레이어의 실제 {@link DataSet}을 반환하는 함수
	 * @return 실제 데이터 셋 반환
	 */
	public DataSet getRealDataSet() {
		return realDataSet;
	}

	/**
	 * 벡터 레이어의 실제 데이터 셋과 가시화 데이터 셋을 동기화 시키는 함수
	 */
	public void syncDataSet() {
		
//		if(data.allPrimitives().size() <= 0)
//			return;
//		
//		if(realDataSet != null) {
//			realDataSet.clear();
//			realDataSet = null;
//		}
//		realDataSet = new DataSet(data);
		
		Map<OsmPrimitive, OsmPrimitive> primMap = new HashMap<>();
		//analysisDataSet(data);
		//analysisDataSet(realDataSet);
		for(Node n : data.getNodes()){
			Node newNode = new Node(n);
			if(n.isDeleted()){
				Node rn = null;
				if(null != (rn = (Node)realDataSet.getPrimitiveById(n))){
					for(Way w : rn.getParentWays()) {
						w.removeNode(rn);
						
						if(w.getNodesCount() <= 0)
							w.setDeleted(true);
					}
					realDataSet.removePrimitive(n);
				}
			}else{
				primMap.put(n, newNode);
			}

			for(Way w : n.getParentWays()) {
				w.setModified(true);
			}
		}
		
		
		for(Way w : data.getWays()){
			if(w.isModified()) {
				if(w.isNew()){
					List<Node> nodes = new ArrayList<>();
					for(Node n : w.getNodes()) {
						Node node = (Node)primMap.get(n);
						try{
							if(null == realDataSet.getPrimitiveById(node)){
								realDataSet.addPrimitive(node);
							}
							nodes.add(node);
						}catch(Exception e){
							e.printStackTrace();
						}
					}
					Way way = new Way(++Constants.TEMP_OSM_ID, Constants.TEMP_OSM_VERSION);
					way.cloneFrom(w);
					way.setNodes(nodes);

					Way rw = null;
					if(null == (rw = (Way)realDataSet.getPrimitiveById(way))){
						try{
							realDataSet.addPrimitive(way);
						}catch(Exception e){
							e.printStackTrace();
						}
					} else {
						Map<String, String> tag = rw.getKeys();
					   for( String key : way.getKeys().keySet() ){
						   String v = way.getKeys().get(key);
						   tag.put(key, v);//way.getKeys().get(key));
						}
					    rw.setKeys(tag);
					}
					// Delete way 추가
					if(w.isDeleted()) {
						OsmPrimitive way1 = (Way)realDataSet.getPrimitiveById(w);
						if(null != way1){
							realDataSet.removePrimitive(way1);
						}
					}
				}else if(w.isDeleted()){
					OsmPrimitive way = realDataSet.getPrimitiveById(w);
					if(null != way){
						for(Node n : ((Way)way).getNodes()) {
							if(null != n && null != realDataSet.getPrimitiveById(n)){
								realDataSet.removePrimitive(n);
							}
						}
						realDataSet.removePrimitive(way);
					}
				}else{
					Way way = (Way)realDataSet.getPrimitiveById(w);
					if(null != way){
						List<Node> nodes = new ArrayList<>();
						List<Node> tempLsit = new ArrayList<>();
						for(Node n : w.getNodes()) {
							Node node = (Node)primMap.get(n);

							if(null == realDataSet.getPrimitiveById(node)){
								try{
									realDataSet.addPrimitive(node);
									nodes.add(node);
								}catch(Exception e){
									e.printStackTrace();
								}
							}else{
								node = (Node)realDataSet.getPrimitiveById(node);
								try{
									node.cloneFrom(primMap.get(n));
									nodes.add(node);
								}catch(Exception e){
									tempLsit.add(n);
									System.out.println("#################### first node error!!!!");
								}
							}
						}
						int retry = 0;
						while(tempLsit.size() > 0 && retry < 5){
							List<Node> tempNodes = new ArrayList<>(tempLsit);
							tempLsit = new ArrayList<>();
							System.out.println("####################["+retry+"] temp start["+tempNodes.size()+"]!!!!");
							for(int i=tempNodes.size()-1; i>=0; i--) {
								Node n = tempNodes.get(i);
								Node node = (Node)realDataSet.getPrimitiveById(primMap.get(n));
								try{
									node.cloneFrom(primMap.get(n));
									nodes.add(node);
								}catch(Exception e){
									tempLsit.add(n);
									System.out.println("#################### ["+retry+"] node error!!!!");
								}
							}
							retry++;
						}

						Way temp = new Way(w);
						temp.setNodes(nodes);

						way.cloneFrom(temp);
					}
				}
			}
		}
		realDataSet.cleanupDeletedPrimitives();
//		//analysisDataSet(data);
//		//analysisDataSet(realDataSet);
	}

	/**
	 * 오브젝트의 가시화 여부를 설정하는 함수
	 * @param cls
	 * @param visible
	 */
	public void setVisibleLayer(String cls, boolean visible) {
		visibleMap.put(cls, visible);
		syncDataSet();
		drawLayer();
	}

	/**
	 * 오브젝트의 가시화 여부를 설정하는 함수
	 * @param cls
	 */
	public void setVisibleOneLayer(String cls) {
		visibleMap.put(cls, true);
		syncDataSet();
		for(Way w : realDataSet.getWays()){
			String c = AbstractIndoorObject.getIndoorObjectCode(w.get(Constants.INDOOR_OBJECT_KEY));
			if(!c.equals(cls)){
				visibleMap.put(c, false);
			}
		}
		drawLayer();
	}

	/**
	 * 가시화 데이터 셋을 설정하는 함수
	 */
	public void drawLayer(){
		isEventPrevent = true;

		data.clear();
		data.cleanupDeletedPrimitives();
		Map<OsmPrimitive, OsmPrimitive> primMap = new HashMap<>();

		for(Node n : realDataSet.getNodes()){
			Node node = new Node(n);
			primMap.put(n, node);
		}
		for(Way w : realDataSet.getWays()){
			String c = AbstractIndoorObject.getIndoorObjectCode(w.get(Constants.INDOOR_OBJECT_KEY));
			if(visibleMap.containsKey(c) && !visibleMap.get(c)){
				continue;
			}
			List<Node> nodes = new ArrayList<>();
			for(Node n : w.getNodes()){
				if(null != primMap.get(n) && null == data.getPrimitiveById(primMap.get(n))){
					data.addPrimitive(primMap.get(n));
				}
				nodes.add((Node) primMap.get(n));
			}
			Way way = new Way(w);
			way.setNodes(nodes);
			data.addPrimitive(way);
		}
		LayerManager.getInstance().updateTree(VectorLayer.this, true);
		isEventPrevent = false;
	}

	/**
	 * 오브젝트의 가시화 여부를 반환하는 함수
	 * @param cls
	 * @return 오브젝트의 가시화 여부를 반환
	 */
	public boolean getVisible(String cls){
		return visibleMap.containsKey(cls) ? visibleMap.get(cls) : true;
	}

	public void analysisDataSet(DataSet ds) {
		System.out.println("##########################################");

		for(Way w : ds.getWays()){
			if(w.isModified() || w.isNew() || w.isDeleted()){
				System.out.println("### " + w.getId() + " M["+w.isModified()+"] N["+w.isNew()+"] D["+w.isDeleted()+"]");
				for(Node n : w.getNodes()){
					System.out.println("### " + w.getId() + "node["+n.getId() + "] M["+n.isModified()+"] N["+n.isNew()+"] D["+n.isDeleted()+"]");
				}
			}
		}

		System.out.println("##########################################");
	}

	@Override
	public void commandAdded(CommandAddedEvent e) {}

	@Override
	public void cleaned(CommandQueueCleanedEvent e) {
	}

	@Override
	public void commandUndone(CommandUndoneEvent e) {
		syncDataSet();
		LayerManager.getInstance().updateTree(this, true);
	}

	@Override
	public void commandRedone(CommandRedoneEvent e) {
		syncDataSet();
		LayerManager.getInstance().updateTree(this, true);
	}

	@Override
	public VectorLayer clone(){
		syncDataSet();
		VectorLayer l = null;
		try{
			l = (VectorLayer)super.clone();
		}catch(Exception e){
			return null;
		}
		return l;
	}
}
