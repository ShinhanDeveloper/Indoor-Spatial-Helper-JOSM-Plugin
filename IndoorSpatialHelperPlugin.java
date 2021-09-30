package org.openstreetmap.josm.plugins.indoorSpatialHelper;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainFrame;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.layer.MapViewPaintable.PaintableInvalidationEvent;
import org.openstreetmap.josm.gui.layer.MapViewPaintable.PaintableInvalidationListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.io.OsmWriterFactory;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.BuildingInfo;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.BuildingInfo.FloorInfo;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.BuildingManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.LayerManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.CommonUtil;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;
import org.xml.sax.SAXException;
import org.openstreetmap.josm.tools.Destroyable;
/**
 * 실내공간정보 구축을 위한 IndoorSpatialHelper 메인 클래스
 *
 * @author shw
 */
public class IndoorSpatialHelperPlugin extends Plugin implements PaintableInvalidationListener, ActiveLayerChangeListener {

	private IndoorSpatialHelperController controller;

	public static IndoorSpatialHelperShutdownHook hook;

	/**
	 * {@link IndoorSpatialHelperPlugin}의 생성자
	 * @param info
	 */
	public IndoorSpatialHelperPlugin(PluginInformation info) {
		super(info);
		MainApplication.getLayerManager().addAndFireActiveLayerChangeListener(this);
		hook = new IndoorSpatialHelperShutdownHook();
//		Runtime.getRuntime().addShutdownHook(hook);

		MainFrame main = MainApplication.getMainFrame();
		for(WindowListener l : main.getWindowListeners()){
			if(l.getClass().getSimpleName().equals("ExitWindowAdapter")){
				main.removeWindowListener(l);
			}
		}
		main.addWindowListener(new ExitWindowAdapterImpl());
	}
    
	@Override
	public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
		if (oldFrame == null && newFrame != null) {
			controller = new IndoorSpatialHelperController();
			importWorkData();
		}
	}

	@Override
	public void activeOrEditLayerChanged(ActiveLayerChangeEvent event) {
		OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
		if (editLayer != null) {
			editLayer.addInvalidationListener(this);
		}
		if(null != controller){
			controller.activeOrEditLayerChanged(event);
		}
	}

	@Override
	public void paintableInvalidated(PaintableInvalidationEvent event) {

	}

	/**
	 * 이전 사용자의 작업 내역을 불러오는 함수
	 */
	public void importWorkData(){
		if(isExistsWorkData()){
			MainApplication.worker.execute(new PleaseWaitRunnable(tr("Indoor Spatial Helper")) {
				@Override
				protected void realRun() throws SAXException, IOException, OsmTransferException {
					try{
						while(MainApplication.getLayerManager().getLayers().size() < 1){
							Thread.sleep(100);
						}
					}catch(InterruptedException e){
						e.printStackTrace();
					}
//					String msg = tr("There is indoor working data.\nWould you like to import the data?");
					String msg = tr("작업중인 실내공간 데이터가 있습니다.\n이 데이터를 불러올까요?");
					int result = JOptionPane.showConfirmDialog(null, msg, tr("Confirm"), JOptionPane.YES_NO_OPTION);
					if(result == JOptionPane.YES_OPTION){
						BuildingManager buildingManager = readSer(Constants.WORKSPACE_DIR + "/buildings.dat", BuildingManager.class);
						if(null != buildingManager){
							Set<Entry<Long, BuildingInfo>> set = buildingManager.getBuildingInfoMap().entrySet();
							for(Entry<Long, BuildingInfo> entry : set){
								List<FloorInfo> floors = entry.getValue().getFloorList();
								for(FloorInfo floor : floors){
									StringBuffer sb = new StringBuffer();
									sb.append(Constants.WORKSPACE_DIR);
									sb.append("/" + entry.getKey());
									sb.append("/" + floor.getFloor());

									/*
									VectorLayer layer = readSer(sb.toString() + "/layer.dat", VectorLayer.class);
									DataSet ds = getSavedOsmDataSet(sb.toString() + "/layer.osm");
									if(null != ds && null != layer){
										VectorLayer vectorLayer = new VectorLayer(ds, layer);
										LayerManager.getInstance().addVectorLayer(vectorLayer);
										vectorLayer.initDataSet();
										break;
									}
									 */
									DataSet ds = getSavedOsmDataSet(sb.toString() + "/layer.osm");
									if(null != ds){
										try{
											BufferedImage image = ImageIO.read(new File(sb.toString() + "/floorPlan.png"));
											floor.setFloorPlan(image);
										}catch(Exception e){
											e.printStackTrace();
										}

										String layerNm = entry.getValue().getNm() + "(" + CommonUtil.getFloorFormat(floor.getFloor()) + ")";
										VectorLayer vectorLayer = new VectorLayer(ds, layerNm, entry.getKey(), floor.getFloor());
										LayerManager.getInstance().addVectorLayer(vectorLayer, false);
										vectorLayer.initDataSet();
									}
								}
							}
							IndoorSpatialHelperController.setBuildingManager(buildingManager);
						}
					}else{
//						deleteWorkspace();
					}
				}

				@Override
				protected void finish() {}

				@Override
				protected void cancel() {}
			});

		}
	}

	/**
	 * 이전 사용자의 작업 내역이 존재하는지 체크하는 함수
	 * @return 사용자 작업 내역 파일 존재 여부
	 */
	private boolean isExistsWorkData(){
		File file = new File(Constants.WORKSPACE_DIR + "/buildings.dat");
		return file.exists();
	}

	/**
	 * 이전 사용자의 작업 내역을 삭제하는 함수
	 */
	public static void deleteWorkspace() {
		CommonUtil.deleteDirectory(Constants.WORKSPACE_DIR);
	}

	/**
	 * 사용자의 작업 내역을 파일로 저장하는 함수
	 * @param filePath
	 * @param clazz
	 * @return 파일 생성 성공 여부
	 */
	public static boolean writeSer(String filePath, Object clazz){
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try{
			File dir = new File(filePath.substring(0, filePath.lastIndexOf("/")+1));
			if(!dir.exists()){
				dir.mkdirs();
			}

			fos = new FileOutputStream(filePath);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(clazz);
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			if(null != oos){try{ oos.close();}catch(IOException e){}}
			if(null != fos){try{ fos.close();}catch(IOException e){}}
		}
		return true;
	}

	/**
	 * 이전 사용자의 작업 내역이 저장된 파일을 불러오는 함수
	 * @param filePath
	 * @param clazz
	 * @return 클래스 객체
	 */
	public static <T> T readSer(String filePath, Class<T> clazz){
		FileInputStream fis = null;
		ObjectInputStream ois = null;

		T result = null;
		try{
			File file = new File(filePath);
			if(!file.exists()){
				return null;
			}

			fis = new FileInputStream(filePath);
			ois = new ObjectInputStream(fis);
			result = clazz.cast(ois.readObject());
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}catch(ClassCastException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			if(null != ois){try{ ois.close();}catch(IOException e){}}
			if(null != fis){try{ fis.close();}catch(IOException e){}}
		}
		return result;
	}

	/**
	 * 사용자의 작업된 벡터레이어를 OSM 데이터로 저장
	 * @param filePath
	 * @param layer
	 */
	public static void doSaveOsm(String filePath, VectorLayer layer){
		File dir = new File(filePath.substring(0, filePath.lastIndexOf("/")+1));
		if(!dir.exists()){
			dir.mkdirs();
		}

		OutputStream os = null;
		Writer w = null;
		OsmWriter ow = null;

		try{
			File file = new File(filePath);
			os = Compression.getCompressedFileOutputStream(file);
			w = new OutputStreamWriter(os, StandardCharsets.UTF_8);
			ow = OsmWriterFactory.createOsmWriter(new PrintWriter(w), false, layer.getRealDataSet().getVersion());

			layer.getRealDataSet().getReadLock().lock();
			try{
				ow.write(layer.getRealDataSet());
			}finally {
				layer.getRealDataSet().getReadLock().unlock();
			}

		}catch(IOException e){
			e.printStackTrace();
		}finally {
			if(null != w){try{w.close();}catch(IOException e){}}
			if(null != ow){try{ow.close();}catch(IOException e){}}
			if(null != os){try{os.close();}catch(IOException e){}}
		}
	}

	public static void doSaveOsm2(String filePath, VectorLayer layer){
		File dir = new File(filePath.substring(0, filePath.lastIndexOf("/")+1));
		if(!dir.exists()){
			dir.mkdirs();
		}

		OutputStream os = null;
		Writer w = null;
		OsmWriter ow = null;

		try{
			layer.syncDataSet();

			File file = new File(filePath);
			os = Compression.getCompressedFileOutputStream(file);
			w = new OutputStreamWriter(os, StandardCharsets.UTF_8);
			ow = OsmWriterFactory.createOsmWriter(new PrintWriter(w), false, layer.getRealDataSet().getVersion());

//			layer.data로 저장을 했으나 visible이 false가 된경우 layer.data를 Vectorlayer.drawLayer에서 제외 시키기 때문에
//			저장이 안되는 버그가 있어서 layer.getRealDataSet()으로 저장하는 로직으로 변경

//			layer.data.getReadLock().lock();
//			try{
//				ow.write(layer.data);
//			}finally {
//				layer.data.getReadLock().unlock();
//			}

			layer.getRealDataSet().getReadLock().lock();
			try{
				ow.write(layer.getRealDataSet());
			}finally {
				layer.getRealDataSet().getReadLock().unlock();
			}
		}catch(IOException e){
			e.printStackTrace();
		}finally {
			if(null != w){try{w.close();}catch(IOException e){}}
			if(null != ow){try{ow.close();}catch(IOException e){}}
			if(null != os){try{os.close();}catch(IOException e){}}
		}
	}
	
	/**
	 * 이전 사용자의 작업이 저장된 OSM 데이터로 불러오기
	 * @param filePath
	 * @return OSM 데이터셋 반환
	 */
	public static DataSet getSavedOsmDataSet(String filePath){
		InputStream in = null;
		DataSet ds = null;
		try{
			File file = new File(filePath);
			if(file.exists()){
				in = Compression.getUncompressedFileInputStream(file);
				ds = OsmReader.parseDataSet(in, null);
			}
		}catch(IOException e){
			e.printStackTrace();
		}catch(IllegalDataException e){
			e.printStackTrace();
		}finally{
			if(null != in){
			try {
				in.close();
			} catch(IOException e) {
				}
			}
		}
		return ds;
	}

	/**
	 * 프로그램 종료시 사용자의 작업 내역을 체크 후 저장을 수행하는 클래스
	 */
	class IndoorSpatialHelperShutdownHook extends Thread {

		private List<VectorLayer> layers;
		private BuildingManager builingManager;

		public IndoorSpatialHelperShutdownHook() {}

		public void setWorkingData(){
			
			builingManager = IndoorSpatialHelperController.getBuildingManager().clone();
			if(builingManager.getBuildingInfoMap().size() > 0){
				IndoorSpatialHelperPlugin.writeSer(Constants.WORKSPACE_DIR + "/buildings.dat", builingManager);
				Set<Entry<Long, BuildingInfo>> set = builingManager.getBuildingInfoMap().entrySet();
				for(Entry<Long, BuildingInfo> entry : set){
					List<FloorInfo> floors = entry.getValue().getFloorList();
					for(FloorInfo floor : floors){
						StringBuffer sb = new StringBuffer();
						sb.append(Constants.WORKSPACE_DIR);
						sb.append("/" + entry.getKey());
						sb.append("/" + floor.getFloor());

						List<Layer> layers = MainApplication.getLayerManager().getLayers();
						for(Layer u : layers){
							if(!(u instanceof VectorLayer))
								continue;
							VectorLayer l = (VectorLayer)u;
							
							if(entry.getKey() == l.getOsmPrimitiveId() && floor.getFloor() == l.getFloor()){
								IndoorSpatialHelperPlugin.doSaveOsm2(sb.toString() + "/layer.osm", l);
	//							l.syncDataSet();
	//							IndoorSpatialHelperPlugin.doSaveOsm(sb.toString() + "/layer.osm", l);
								break;
							}
						}
					}
				}
			}
			
//			List<VectorLayer> list = new ArrayList<>();
//			for(Layer l : MainApplication.getLayerManager().getLayers()){
//				if(l instanceof VectorLayer){
//					((VectorLayer)l).syncDataSet();
//					VectorLayer layer = ((VectorLayer)l).clone();
//					if(null != layer){
//						list.add(layer);
//					}
//				}
//			}
//			layers = list;
//			builingManager = IndoorSpatialHelperController.getBuildingManager().clone();
//			saveOsm();
		}
		public void saveOsm(){
			try{
				IndoorSpatialHelperPlugin.deleteWorkspace();
				Thread.sleep(100);

				if(builingManager.getBuildingInfoMap().size() > 0){
					IndoorSpatialHelperPlugin.writeSer(Constants.WORKSPACE_DIR + "/buildings.dat", builingManager);
					Set<Entry<Long, BuildingInfo>> set = builingManager.getBuildingInfoMap().entrySet();
					for(Entry<Long, BuildingInfo> entry : set){
						List<FloorInfo> floors = entry.getValue().getFloorList();
						for(FloorInfo floor : floors){
							StringBuffer sb = new StringBuffer();
							sb.append(Constants.WORKSPACE_DIR);
							sb.append("/" + entry.getKey());
							sb.append("/" + floor.getFloor());

							for(VectorLayer l : layers){
								if(entry.getKey() == l.getOsmPrimitiveId() && floor.getFloor() == l.getFloor()){
									IndoorSpatialHelperPlugin.doSaveOsm2(sb.toString() + "/layer.osm", l);
									floor.writeImage(sb.toString() + "/floorPlan.png");
									break;
								}
							}
						}
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		@Override
		public void run(){
			try{
				IndoorSpatialHelperPlugin.deleteWorkspace();
				Thread.sleep(100);

				if(builingManager.getBuildingInfoMap().size() > 0){
					IndoorSpatialHelperPlugin.writeSer(Constants.WORKSPACE_DIR + "/buildings.dat", builingManager);
					Set<Entry<Long, BuildingInfo>> set = builingManager.getBuildingInfoMap().entrySet();
					for(Entry<Long, BuildingInfo> entry : set){
						List<FloorInfo> floors = entry.getValue().getFloorList();
						for(FloorInfo floor : floors){
							StringBuffer sb = new StringBuffer();
							sb.append(Constants.WORKSPACE_DIR);
							sb.append("/" + entry.getKey());
							sb.append("/" + floor.getFloor());

							for(VectorLayer l : layers){
								if(entry.getKey() == l.getOsmPrimitiveId() && floor.getFloor() == l.getFloor()){
									l.syncDataSet();
									IndoorSpatialHelperPlugin.doSaveOsm(sb.toString() + "/layer.osm", l);
									//IndoorSpatialHelperPlugin.writeSer(sb.toString() + "/layer.dat", l);
									floor.writeImage(sb.toString() + "/floorPlan.png");
									break;
								}
							}
						}
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	/**
	 * 프로그램 종료시 실행되는 함수
	 */
	static final class ExitWindowAdapterImpl extends WindowAdapter {
		@Override
		public void windowClosing(final WindowEvent evt) {
			hook.setWorkingData();
			MainApplication.exitJosm(true, 0, null);
		}
	}
}
