package org.openstreetmap.josm.plugins.indoorSpatialHelper.modules;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.MLRunner;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.data.MLRunnerAbstract;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.Constants;
import org.openstreetmap.josm.tools.I18n;
import org.xml.sax.SAXException;

/**
 * ML python 모듈 및 java 실행을 처리하는 쓰레드 클래스
 */
public class MLModuleTask extends PleaseWaitRunnable {

	static final String ML_TEAM1_DIR = "/resources/ml_team1";
	static final String ML_TEAM1_FILEPATH = Constants.PLUGIN_DIR + ML_TEAM1_DIR;
	static final String ML_TEAM1_COMMAND = "vectorizing.exe";
	static final String[] ML_TEAM1_OPTIONS = {"./temp_output", "./outputs", "./model"};

	static final String ML_TEAM1_RESULT_SHP_FILEPATH = ML_TEAM1_DIR + "/outputs/output.shp";
	static final String ML_TEAM1_RESULT_DBF_FILEPATH = ML_TEAM1_DIR + "/outputs/output.dbf";

	private MLRunnerAbstract runner;
	private Runnable callback;
	boolean isSucc = false;

	/**
	 * {@link MLModuleTask} 생성자
	 * @param runner
	 * @param callback
	 */
	public MLModuleTask(MLRunnerAbstract runner, Runnable callback) {
		super(I18n.tr("ML Module이 실행 중 입니다. 작업 종료시까지 기다려 주세요."), false);
		this.runner = runner;
		this.callback = callback;
	}

	private boolean canceled;

	@Override
	protected void cancel() {
		this.canceled = true;
		runner.stopModule();
	}

	@Override
	protected void realRun() throws SAXException, IOException, OsmTransferException {
		try {
			isSucc = runner.runModule();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr(e.getMessage()));
		}
	}

	@Override
	protected void finish() {
		if(null != callback && isSucc){
			MainApplication.worker.submit(callback);
		}
	}

}
