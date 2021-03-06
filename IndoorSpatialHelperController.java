package org.openstreetmap.josm.plugins.indoorSpatialHelper;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;

//import org.geotools.data.Base64.InputStream;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.gui.widgets.FileChooserManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.Josm3DViewer.Josm3DViewerCommunicator;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.Josm3DViewer.Josm3DViewerLauncher;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.actions.ImageAction;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.BuildingInfo;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.BuildingInfo.BuildingTransform;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.BuildingInfo.FloorInfo;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.BuildingManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.ImageLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.LayerManager;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.MLRunner;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.MLRunner2020;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.MLRunnerAbstract;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.VectorLayer;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.gui.BuildingInfoPop;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.gui.IndoorSpatialHelperDialog;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.gui.PreProcessingPop;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.gui.SelectVectorizingPop;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.modules.MLModuleTask;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.modules.MLShapeImportTask;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.modules.SVGImportTask;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.modules.VectorizeTask;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.CommonUtil;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;

/**
 * ?????????????????? ????????? ?????? ??? ??? ?????? ?????? ????????? ????????? ?????? ?????????
 */
public class IndoorSpatialHelperController {
	
	public static boolean doNotRunningforDebugging = false;
	
	public static boolean useML2020 = true;

	public static boolean selectionChangeFlag = true;

	private static BuildingManager buildingManager = new BuildingManager();
	private OsmPrimitive selectedOsmPrimitive;

	// Main UI => dialog
	private static IndoorSpatialHelperDialog dialog;
	private BuildingInfoPop buildingInfoPop;
	private PreProcessingPop preProcessingPop;

	private ImageLayer imageLayer;

	private ImageAction moveAction;
	private ImageAction resizeAction;
	private ImageAction rotateAction;
	private JToggleButton selectedToggl;

	/**
	 * ??? ??? ?????? ???????????? ???????????? {@link IndoorSpatialHelperController}??? ?????????
	 */
	public IndoorSpatialHelperController() {

		dialog = new IndoorSpatialHelperDialog();

		this.addSelectionListener();
		this.addDialogListeners();
		MainApplication.getMap().addToggleDialog(dialog);

		moveAction   = new ImageAction("MoveAction", ImageAction.getMoveCursor());
		resizeAction = new ImageAction("ResizeAction", ImageAction.getResizeCursor());
		rotateAction = new ImageAction("RotateAction", ImageAction.getRotateCursor());
	}

	/**
	 * OSM???????????? ????????? ???????????? ???????????? ??????????????? ?????? ??? ???????????? ??????
	 * @param event
	 */
	public void activeOrEditLayerChanged(ActiveLayerChangeEvent event){
		if(event.getPreviousActiveLayer() instanceof VectorLayer){
//			((VectorLayer)event.getPreviousActiveLayer()).syncDataSet();
		}
		LayerManager.getInstance().updateTree(event.getSource());
	}

	/**
	 * OSM????????? ?????? ??????????????? ?????? {@link IndoorSpatialHelperDialog} ???????????? ??????/????????? ??????
	 */
	private void addSelectionListener() {

		SelectionEventManager.getInstance().addSelectionListener(new DataSelectionListener() {
			@Override
			public void selectionChanged(SelectionChangeEvent event) {
				if(selectionChangeFlag){
					Layer layer = getActiveLayer();
					if(layer instanceof ImageLayer){
						dialog.setBuildingInfo(buildingManager.getBuildingInfo(((ImageLayer)layer).getOsmPrimitiveId()));
					}else if(layer instanceof VectorLayer){
						dialog.setBuildingInfo(buildingManager.getBuildingInfo(((VectorLayer)layer).getOsmPrimitiveId()));
					}else if(layer instanceof OsmDataLayer){
						if(event.getSelection().size() == 1){
							Iterator<? extends OsmPrimitive> itr = event.getSelection().iterator();
							while(itr.hasNext()) {
								selectedOsmPrimitive = itr.next();
							}
						}else{
							selectedOsmPrimitive = null;
						}
						dialog.setBuildingInfo(buildingManager.getBuildingInfo(selectedOsmPrimitive));
					}
				}
			}
		});
	}

	/**
	 * {@link IndoorSpatialHelperDialog} ???????????? ???????????? ???????????? ??????
	 */
	private void addDialogListeners() {
		if(null != dialog) {
			dialog.setBtnSetBuildingInfoListener(new DialogBtnSetBuildingInfoListener());
			dialog.setBtnSetImageListener(new DialogBtnSetImageListener());
			dialog.setBtnRotateItemListener(new DialogBtnRotateItemListener());
			dialog.setBtnResizeItemListener(new DialogBtnResizeItemListener());
			dialog.setBtnMoveItemListener(new DialogBtnMoveItemListener());
			dialog.setBtnRotateActionListener(new DialogBtnRotateActionListener());
			dialog.setBtnResizeActionListener(new DialogBtnResizeActionListener());
			dialog.setBtnMoveActionListener(new DialogBtnMoveActionListener());
			dialog.setBtnRunVectorizingListener(new DialogBtnRunVectorizingListener());
			dialog.setBtnCancelListener(new DialogBtnCancelListener());
			dialog.setBtnOpen3DViewerListener(new BtnOpen3DViewerListener());
		}
	}

	/**
	 * {@link IndoorSpatialHelperDialog} ????????? ???????????? ??????
	 * @return ???????????? ??????????????? ?????? ??????
	 */
	public static IndoorSpatialHelperDialog getDialog() {
		return dialog;
	}

	/**
	 * {@link addBuildingInfoPopListeners} ?????? ??????????????? ????????? ??????
	 */
	private void addBuildingInfoPopListeners() {
		if(null != this.buildingInfoPop) {
			this.buildingInfoPop.setOkButtonListener(new BuildingInfoPopOkButtonListener());
			this.buildingInfoPop.setCancelButtonListener(new BuildingInfoPopCancelButtonListener());
			this.buildingInfoPop.setBuildingInfoPopWindowListener(new BuildingInfoPopWindowListener());
		}
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnSetBuildingInfo ?????? ?????????????????? ????????? ?????????
	 */
	class DialogBtnSetBuildingInfoListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (getActiveLayer() instanceof OsmDataLayer && !(getActiveLayer() instanceof VectorLayer)
					&& selectedOsmPrimitive != null) {
				if (buildingInfoPop == null) {
					buildingInfoPop = new BuildingInfoPop(MainApplication.getMainFrame(), selectedOsmPrimitive);
					addBuildingInfoPopListeners();
					buildingInfoPop.setVisible(true);
				} else {
					buildingInfoPop.toFront();
				}
			}else{
//				JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Please select building from osm data layer"));
				JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("????????? ????????? ?????????."));
				return;
			}
		}
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnSetImage ?????? ?????????????????? ????????? ?????????
	 */
	class DialogBtnSetImageListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if(!(getActiveLayer() instanceof OsmDataLayer) || getActiveLayer() instanceof VectorLayer){
//				JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Please select building from osm data layer"));
				JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("osm layer??? ?????? ??? ????????? ?????? ?????????"));
				return;
			}else if(0 == dialog.getFloorSelect()){
//				JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Please select the floor of building to edit"));
				JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("?????? ?????? ?????? ??? ?????????."));
				return;
			}
			FileChooserManager fcm = new FileChooserManager(true);
			fcm.createFileChooser(false, null, Constants.IMAGE_FILTERS, Constants.IMG_FILE_FILTER, JFileChooser.FILES_ONLY);
			AbstractFileChooser fc = fcm.openFileChooser();
			if(fc != null) {
				try{
					File file = fc.getSelectedFile();

					preProcessingPop = new PreProcessingPop(MainApplication.getMainFrame(), file);
					preProcessingPop.setVisible(true);

					if(!preProcessingPop.getResult()){
						preProcessingPop = null;
						return;
					}

					BuildingTransform transform = buildingManager.getTransform(selectedOsmPrimitive.getId());

					if(null != transform){
//						String msg = tr("There is the image adjustment information you have worked.\nDo you want to apply this?");
						String msg = tr("?????? ????????? ????????? ????????????.\n?????????????????????????");
						int result = JOptionPane.showConfirmDialog(null, msg, tr("Confirm"), JOptionPane.YES_NO_OPTION);
						if(result != JOptionPane.YES_OPTION){
							transform = null;
						}
					}

					Bounds boundary = CommonUtil.getBoundary(selectedOsmPrimitive);
					
//					ProjectionBounds pb = MainApplication.getMap().mapView.getProjectionBounds();

					MainApplication.getMap().mapView.zoomTo(boundary);

					BufferedImage bufferedImage = preProcessingPop.getImage();

					if(null == bufferedImage){
						imageLayer = new ImageLayer(file, dialog.getFloorSelect(), selectedOsmPrimitive);
					}else{
						imageLayer = new ImageLayer(bufferedImage, file, dialog.getFloorSelect(), selectedOsmPrimitive);
					}
					imageLayer.initialize(transform);

					addImageLayer(imageLayer, transform);

					preProcessingPop = null;
				}catch(IOException ex){
					ex.printStackTrace();
//					JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Failed to import the image."));
					JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("???????????? ?????? ?????? ????????? ?????? ???????????????."));
				}catch(NullPointerException ex){
					ex.printStackTrace();
//					JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("File is not a image file.\nPlease try again after check file."));
					JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("????????? ????????? ????????????.\n?????? ??? ?????? ????????? ?????????."));
				}catch(Exception ex){
					ex.printStackTrace();
					JOptionPane.showMessageDialog(MainApplication.getMainFrame(), ex.getMessage());
				}
			}
		}
	}

	/**
	 * {@link BuildingManager} ????????? ??????
	 * @param buildingManager
	 */
	protected static void setBuildingManager(BuildingManager buildingManager) {
		IndoorSpatialHelperController.buildingManager = buildingManager;
	}

	/**
	 * {@link BuildingManager} ????????? ??????
	 * @return ?????? ?????? ?????? ??????
	 */
	public static BuildingManager getBuildingManager() {
		return buildingManager;
	}

	/**
	 * ???????????? ?????? ????????? {@link ImageLayer}??? ????????? ?????? ???????????? ??????
	 * @param imageLayer
	 */
	public void addImageLayer(ImageLayer imageLayer) {
		addImageLayer(imageLayer, null);
	}

	/**
	 * {@link ImageLayer} ????????? ?????????  ????????? ????????? ??????
	 * @param imageLayer
	 * @param transform
	 */
	public void addImageLayer(ImageLayer imageLayer, BuildingTransform transform) {
		selectionChangeFlag = false;
		List<Layer> layers = MainApplication.getLayerManager().getLayers();
		for(Layer layer : layers) {
			if(layer instanceof ImageLayer) {
				MainApplication.getLayerManager().removeLayer(layer);
			}
		}
		MainApplication.getLayerManager().addLayer(imageLayer);
		MainApplication.getMap().mapView.moveLayer(imageLayer, MainApplication.getLayerManager().getLayers().size() - 1);
		MainApplication.getLayerManager().setActiveLayer(imageLayer);

		exitActions();

		moveAction   = new ImageAction("MoveAction", ImageAction.getMoveCursor());
		resizeAction = new ImageAction("ResizeAction", ImageAction.getResizeCursor());
		rotateAction = new ImageAction("RotateAction", ImageAction.getRotateCursor());

		if(null != transform){
			resizeAction.setScale(transform.getScale());
			rotateAction.setRadian(transform.getRadian());
		}

		selectionChangeFlag = true;
	}

	/**
	 * {@link ImageLayer} ????????? ?????? ????????? ??????
	 */
	public void exitActions() {
		if(null != moveAction){
			moveAction.exitMode();
		}
		if(null != resizeAction){
			resizeAction.exitMode();
		}
		if(null != rotateAction){
			rotateAction.exitMode();
		}
		dialog.clearToggle();
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnRotate ?????? ?????????????????? ????????? ?????????
	 */
	class DialogBtnRotateItemListener implements ItemListener {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				selectedToggl = (JToggleButton)e.getSource();
				rotateAction.enterMode();
			} else {
				rotateAction.exitMode();
			}
		}
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnRotate ?????? ??????????????? ????????? ?????????
	 */
	class DialogBtnRotateActionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if(selectedToggl == null){
				dialog.clearToggle();
				rotateAction.exitMode();
			}
			selectedToggl = null;
		}
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnResize ?????? ?????????????????? ????????? ?????????
	 */
	class DialogBtnResizeItemListener implements ItemListener {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				selectedToggl = (JToggleButton)e.getSource();
				resizeAction.enterMode();
			} else {
				resizeAction.exitMode();
			}
		}
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnResize ?????? ??????????????? ????????? ?????????
	 */
	class DialogBtnResizeActionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if(selectedToggl == null){
				dialog.clearToggle();
				resizeAction.exitMode();
			}
			selectedToggl = null;
		}
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnMove ?????? ?????????????????? ????????? ?????????
	 */
	class DialogBtnMoveItemListener implements ItemListener {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				selectedToggl = (JToggleButton)e.getSource();
				moveAction.enterMode();
			} else {
				moveAction.exitMode();
			}
		}
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnMove ?????? ??????????????? ????????? ?????????
	 */
	class DialogBtnMoveActionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if(selectedToggl == null){
				dialog.clearToggle();
				moveAction.exitMode();
			}
			selectedToggl = null;
		}
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnRunVectorizing ?????? ??????????????? ????????? ?????????
	 */
	class DialogBtnRunVectorizingListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {

			if(null == imageLayer || !(getActiveLayer() instanceof ImageLayer)){
//				JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Please select image layer."));
				JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("????????? ???????????? ?????? ??? ?????????."));
				return;
			}

			VectorLayer v = null;
			if(LayerManager.getInstance().containsVectorLayer(imageLayer.getOsmPrimitiveId(), imageLayer.getFloor())){
//				String msg = tr("The vector layer for that floor already exists.\nOverwrite it?");
				String msg = tr("?????? ?????? ?????? ?????????????????? ?????? ?????????.\n?????? ??????????");
				int confirmResult = JOptionPane.showConfirmDialog(null, msg, tr("Confirm"), JOptionPane.YES_NO_OPTION);
				if(confirmResult != JOptionPane.YES_OPTION) {
					return;
				}
				v = LayerManager.getInstance().getVectorLayer(imageLayer.getOsmPrimitiveId(), imageLayer.getFloor());
			}

			int index = 1;

			if(!Constants.IS_CERTIFICATE_VERSION){
				SelectVectorizingPop vectorizingPop = new SelectVectorizingPop(MainApplication.getMainFrame());
				vectorizingPop.setVisible(true);

				index = vectorizingPop.getIndex();
				vectorizingPop = null;
				if(index < 0){
					return;
				}
			}else{
//				int confirmResult = JOptionPane.showConfirmDialog (null, "Would you like to run to vectorizing?", "Confirm", JOptionPane.YES_NO_OPTION);
				int confirmResult = JOptionPane.showConfirmDialog (null, "????????? ????????? ?????? ??????????", "????????? ??????", JOptionPane.YES_NO_OPTION);
				if(confirmResult != JOptionPane.YES_OPTION) {
					return;
				}
			}

			buildingManager.setFloorPlan(imageLayer.getOsmPrimitiveId(), imageLayer.getFloor(), imageLayer.getImage());
			buildingManager.setTransform(imageLayer.getOsmPrimitiveId(), imageLayer.getFloor(), imageLayer.getCenter(),
					resizeAction.getScale(), rotateAction.getRadian());

			try {
				Runnable callback = null;
				Runnable task = null;

				if(null != v){
					MainApplication.getLayerManager().removeLayer(v);
				}

				if(index == 1){ // OpenSource base
					String svgFile = Constants.PLUGIN_DIR + "/temp/temp.svg";

					callback = new SVGImportTask(svgFile, imageLayer, resizeAction.getScale(), rotateAction.getRadian());
					task = new VectorizeTask(imageLayer.getImage(), svgFile, 2, callback);
				}

				if(null != task){
					MainApplication.worker.submit(task);
				}
				exitActions();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnOpen3DViewer ?????? ??????????????? ????????? ?????????
	 */
	class BtnOpen3DViewerListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if(!(getActiveLayer() instanceof VectorLayer)){
//					JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Please select building layer from layer list"));
					JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("????????? ????????? ?????????."));
					return;
				}
				// ????????? ?????? ??????
				String height = Integer.toString(dialog.getHeight());
//				dialog.setFloorHeight(height);
				saveOsmLayer();

				VectorLayer layer = (VectorLayer)getActiveLayer();
				
				String buildingID = Long.toString(layer.getOsmPrimitiveId());
				String targetFloor = Integer.toString(layer.getFloor());
				
				Josm3DViewerLauncher.getInstance().startJosm3DViewer(Constants.WORKSPACE_DIR, buildingID, targetFloor, height,
					new Josm3DViewerLauncher.CommunicationListener() {
						
						@Override
						public void onCommunication(Josm3DViewerCommunicator communicator) {
							switch (communicator.getCommand()) {
								case sync_request:
									String height = Integer.toString(dialog.getHeight());
//									dialog.setFloorHeight(height);
									saveOsmLayer();
									Josm3DViewerCommunicator cmd = Josm3DViewerCommunicator.createCommand(Josm3DViewerCommunicator.Cmds.sync_response);
									Josm3DViewerLauncher.getInstance().sendMessage(cmd);
									break;
							}
						}
					}
				);
				
				
			} catch (Throwable ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();

			}
		}
		
		public <T> T readSer(String filePath, Class<T> clazz){
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
		
		private void saveOsmLayer()
		{
			Set<Entry<Long, BuildingInfo>> set = buildingManager.getBuildingInfoMap().entrySet();
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
	}

	/**
	 * {@link IndoorSpatialHelperDialog}.btnCancel ?????? ??????????????? ????????? ?????????
	 */
	class DialogBtnCancelListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
//			String msg = tr("Would you like to cancel your work on the image layer?");
			String msg = tr("????????? ????????? ????????? ?????? ???????????????????");
			int confirmResult = JOptionPane.showConfirmDialog(null, msg, tr("??????"), JOptionPane.YES_NO_OPTION);
			if(confirmResult == JOptionPane.YES_OPTION) {
				dialog.resetFloorSelect();
				exitActions();
				List<Layer> layers = MainApplication.getLayerManager().getLayers();
				for(Layer layer : layers) {
					if(layer instanceof ImageLayer) {
						MainApplication.getLayerManager().removeLayer(layer);
					}
				}
			}
		}
	}

	/**
	 * {@link BuildingInfoPop}.okButton ?????? ??????????????? ????????? ?????????
	 */
	class BuildingInfoPopOkButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if(selectedOsmPrimitive != null && buildingInfoPop.validation()) {
				buildingManager.setBuildingInfo(selectedOsmPrimitive, buildingInfoPop.getBuildingInfo());
				dialog.setBuildingInfo(buildingManager.getBuildingInfo(selectedOsmPrimitive));
				buildingInfoPop.dispose();
				buildingInfoPop = null;
				dialog.setFloorPlanInfoPanelVisible(true);
			}
		}
	}

	/**
	 * {@link BuildingInfoPop}.cancelButton ?????? ??????????????? ????????? ?????????
	 */
	class BuildingInfoPopCancelButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			buildingInfoPop.dispose();
			buildingInfoPop = null;
		}
	}

	/**
	 * {@link BuildingInfoPop} ????????? ????????? ?????????
	 */
	class BuildingInfoPopWindowListener implements WindowListener {

		@Override
		public void windowOpened(WindowEvent e) {}

		@Override
		public void windowClosing(WindowEvent e) {
			buildingInfoPop = null;
		}

		@Override
		public void windowClosed(WindowEvent e) {
			buildingInfoPop = null;
		}

		@Override
		public void windowIconified(WindowEvent e) {}

		@Override
		public void windowDeiconified(WindowEvent e) {}

		@Override
		public void windowActivated(WindowEvent e) {}

		@Override
		public void windowDeactivated(WindowEvent e) {}
	}

	/**
	 * ???????????? ????????? ?????? ?????? ?????? ??? ?????? ????????? ???????????? ??????
	 * @param imageLayer
	 * @return ???????????? ????????? ?????? ?????? ??????
	 */
	public String createImageFile(ImageLayer imageLayer) throws Exception {
		//String imagePath = imageLayer.getImageFile().getCanonicalPath();
		BufferedImage image = imageLayer.getImage();
		//String tempFileExt = imagePath.substring(imagePath.lastIndexOf(".") + 1);
		//String tempFileNm = Constants.PLUGIN_DIR + "/temp/temp." + tempFileExt;
		long osmId = imageLayer.getOsmPrimitiveId();
		String directory = Constants.PLUGIN_DIR + "/temp/ml/" + osmId + "/" + CommonUtil.getFloorFormat(imageLayer.getFloor());
		String tempFileNm = directory + "/" + osmId + ".png";

		File dir = new File(directory);
		if(dir.exists()){
			CommonUtil.deleteDirectory(dir);
		}
		dir.mkdirs();
		

		File tempTile = new File(tempFileNm);
		//ImageIO.write(image, tempFileExt, tempTile);
		ImageIO.write(image, "png", tempTile);
		return tempTile.getCanonicalPath().replaceAll("\\\\", "/");
	}

	public String createImageFile2020(ImageLayer imageLayer) throws Exception {
		BufferedImage image = imageLayer.getImage();
		long osmId = imageLayer.getOsmPrimitiveId();
		String directory = Constants.PLUGIN_DIR + "/temp2020/ml/" + osmId + "/" + CommonUtil.getFloorFormat(imageLayer.getFloor());
		String tempFileNm = directory + "/" + osmId + ".png";

		File dir = new File(directory);
		if(dir.exists()){
			if(!IndoorSpatialHelperController.doNotRunningforDebugging)
				CommonUtil.deleteDirectory(dir);
		}

		if(!dir.exists())
			dir.mkdirs();
		dir.createNewFile();
		
		File tempTile = new File(tempFileNm);
		if(tempTile.exists())
			tempTile.delete();
		
		ImageIO.write(image, "png", tempTile);
		return tempTile.getCanonicalPath().replaceAll("\\\\", "/");
	}

	/**
	 * ???????????? ???????????? ??????
	 * @return ????????? ?????? ??????
	 */
	public Layer getActiveLayer() {
		return MainApplication.getLayerManager().getActiveLayer();
	}
}
