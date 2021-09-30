package org.openstreetmap.josm.plugins.indoorSpatialHelper.util;

import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.net.URL;

import javax.swing.ImageIcon;

/**
 * 공통 디자인 관련 함수를 모아놓은 클래스
 */
public class GuiUtil {

	public static GridBagConstraints getGridBagConst(int gridx, int gridy, int gridwidth, int gridheight) {
		return getGridBagConst(gridx, gridy, gridwidth, gridheight, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
	}
	public static GridBagConstraints getGridBagConst2(int gridx, int gridy, int gridwidth, int gridheight) {
		return new GridBagConstraints(gridx, gridy, gridwidth, gridheight, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 10, 5, 5), 0, 0);
	}

	public static GridBagConstraints getGridBagConst(int gridx, int gridy, int gridwidth, int gridheight, int anchor) {
		return getGridBagConst(gridx, gridy, gridwidth, gridheight, anchor, GridBagConstraints.BOTH);
	}

	public static GridBagConstraints getGridBagConst(int gridx, int gridy, int gridwidth, int gridheight, int anchor, int fill) {
		return new GridBagConstraints(gridx, gridy, gridwidth, gridheight, 0.0, 1.0, anchor, fill, new Insets(0, 0, 5, 5), 0, 0);
	}

	private static Image createImage(String path){
		URL resource = GuiUtil.class.getResource(path);
		if(null == resource){
			return null;
		}
		return Toolkit.getDefaultToolkit().createImage(resource);
	}

	private static Image getScaledImage(Image src, int w, int h){
		if(null == src){
			return null;
		}
		return src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
	}

	public static ImageIcon getImageIcon(String path){
		return new ImageIcon(createImage(path));
	}

	public static ImageIcon getImageIcon(String path, int w, int h){
		return new ImageIcon(GuiUtil.getScaledImage(createImage(path), w, h));
	}
}
