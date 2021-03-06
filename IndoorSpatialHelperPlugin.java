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
 * ?????????????????? ????????? ?????? IndoorSpatialHelper ?????? ?????????
 *
 * @author shw
 */
public class IndoorSpatialHelperPlugin extends Plugin implements PaintableInvalidationListener, ActiveLayerChangeListener {

	private IndoorSpatialHelperController controller;

	public static IndoorSpatialHelperShutdownHook hook;

	/**
	 * {@link IndoorSpatialHelperPlugin}??? ?????????
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
	 * ?????? ???????????? ?????? ????????? ???????????? ??????
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
					String msg = tr("???????????? ???????????? ???????????? ????????????.\n??? ???????????? ????????????????");
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
	 * ?????? ???????????? ?????? ????????? ??????????????? ???????????? ??????
	 * @return ????????? ?????? ?????? ?????? ?????? ??????
	 */
	private boolean isExistsWorkData(){
		File file = new File(Constants.WORKSPACE_DIR + "/buildings.dat");
		return file.exists();
	}

	/**
	 * ?????? ???????????? ?????? ????????? ???????????? ??????
	 */
	public static void deleteWorkspace() {
		CommonUtil.deleteDirectory(Constants.WORKSPACE_DIR);
	}

	/**
	 * ???????????? ?????? ????????? ????????? ???????????? ??????
	 * @param filePath
	 * @param clazz
	 * @return ?????? ?????? ?????? ??????
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
	 * ?????? ???????????? ?????? ????????? ????????? ????????? ???????????? ??????
	 * @param filePath
	 * @param clazz
	 * @return ????????? ??????
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
	 * ???????????? ????????? ?????????????????? OSM ???????????? ??????
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

//			layer.data??? ????????? ????????? visible??? false??? ????????? layer.data??? Vectorlayer.drawLayer?????? ?????? ????????? ?????????
//			????????? ????????? ????????? ????????? layer.getRealDataSet()?????? ???????????? ???????????? ??????

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
	 * ?????? ???????????? ????????? ????????? OSM ???????????? ????????????
	 * @param filePath
	 * @return OSM ???????????? ??????
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
	 * ???????????? ????????? ???????????? ?????? ????????? ?????? ??? ????????? ???????????? ?????????
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
	 * ???????????? ????????? ???????????? ??????
	 */
	static final class ExitWindowAdapterImpl extends WindowAdapter {
		@Override
		public void windowClosing(final WindowEvent evt) {
			hook.setWorkingData();
			MainApplication.exitJosm(true, 0, null);
		}
	}
}
