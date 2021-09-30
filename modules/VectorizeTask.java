package org.openstreetmap.josm.plugins.indoorSpatialHelper.modules;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.I18n;
import org.xml.sax.SAXException;

import jankovicsandras.imagetracer.ImageTracer;

/**
 * ImageTracer 모듈을 통한 이미지 벡터라이징을 처리하는 쓰레드 클래스
 */
public class VectorizeTask extends PleaseWaitRunnable {

	private String imageFile;
	private BufferedImage image = null;
	private String svgFile;
	private int numberOfColors;
	private Runnable callback;

	private boolean canceled;

	/**
	 * {@link VectorizeTask} 생성자
	 * @param imageFile
	 * @param svgFile
	 * @param numberOfColors
	 * @param callback
	 */
	public VectorizeTask(String imageFile, String svgFile, int numberOfColors, Runnable callback){
		super(I18n.tr("Start Vectorize..."), false);
		this.imageFile = imageFile;
		this.svgFile = svgFile;
		this.numberOfColors = numberOfColors;
		this.callback = callback;
	}

	/**
	 * {@link VectorizeTask} 생성자
	 * @param image
	 * @param svgFile
	 * @param numberOfColors
	 * @param callback
	 */
	public VectorizeTask(BufferedImage image, String svgFile, int numberOfColors, Runnable callback){
		super(I18n.tr("Start Vectorize..."), false);
		this.image = image;
		this.svgFile = svgFile;
		this.numberOfColors = numberOfColors;
		this.callback = callback;
	}

	@Override
	protected void cancel() {
		this.canceled = true;
	}

	@Override
	protected void realRun() throws SAXException, IOException, OsmTransferException {
		try {
			File path = new File(svgFile.substring(0, svgFile.lastIndexOf("/")));
			if(!path.exists()){
				path.mkdirs();
			}

			HashMap<String,Float> options = new HashMap<String,Float>();
			//options.put("ltres", 2f);
			//options.put("qtres", 2f);
			//options.put("pathomit", 0f);
			//options.put("blurdelta", 40f);
			//options.put("blurradius", 5f);
			options.put("colorsampling", 0f);
			options.put("numberofcolors", (float)numberOfColors);
			options.put("colorquantcycles", 1f);

			if(canceled){
				return;
			}

			if(image != null) {
				ImageTracer.saveString(svgFile, ImageTracer.imageToSVG(image, options, null));
			} else {
				ImageTracer.saveString(svgFile, ImageTracer.imageToSVG(imageFile, options, null));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void finish() {
		if(null != callback && !canceled){
			MainApplication.worker.submit(callback);
		}
	}
}
