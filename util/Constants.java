package org.openstreetmap.josm.plugins.indoorSpatialHelper.util;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Image;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.Preferences;

public class Constants {

	/**
	 * 시험 성적서용 여부
	 * true : 시험 성적서용
	 * false : 비시험 성적서용
	 */
	public static final boolean IS_CERTIFICATE_VERSION = false;

	public static long TEMP_OSM_ID = 0;
	public static int TEMP_OSM_VERSION = 1;

	/**
	 * 플러그인 jar 파일 경로
	 */
	public static final String JAR_FILE_PATH = Preferences.main().getPluginsDirectory().getAbsolutePath().replaceAll("\\\\", "/") + "/indoor-spatial-helper.jar";
	/**
	 * 플러그인의 작업 파일들이 저장될 경로
	 */
	public static final String PLUGIN_DIR = Preferences.main().getPluginsDirectory().getAbsolutePath().replaceAll("\\\\", "/") + "/IndoorSpatialHelper";
	/**
	 * 사용자 작업 파일이 저장될 경로
	 */
	public static final String WORKSPACE_DIR = PLUGIN_DIR + "/workspace";

	/**
	 * 이미지 필터 종류
	 */
	public static final ExtensionFileFilter BMP_FILE_FILTER = new ExtensionFileFilter("bmp", "bmp", tr("Bitmap Image File") + " (*.bmp)");
	public static final ExtensionFileFilter JPG_FILE_FILTER = new ExtensionFileFilter("jpg,jpeg", "jpg", tr("Joint Photographic Experts Group File") + " (*.jpg,*.jpeg)");
	public static final ExtensionFileFilter GIF_FILE_FILTER = new ExtensionFileFilter("gif", "gif", tr("Graphics Interchange Format File") + " (*.gif)");
	public static final ExtensionFileFilter TIF_FILE_FILTER = new ExtensionFileFilter("tif,tiff", "tif", tr("Tagged Image File Format File") + " (*.tif,*.tiff)");
	public static final ExtensionFileFilter PNG_FILE_FILTER = new ExtensionFileFilter("png", "png", tr("Portable Network Graphics File") + " (*.png)");
	public static final ExtensionFileFilter IMG_FILE_FILTER = new ExtensionFileFilter("bmp,jpg,jpeg,gif,tif,tiff,png", "png", tr("Image File"));
	public static final ExtensionFileFilter GML_FILE_FILTER = new ExtensionFileFilter("gml", "gml", tr("Geography Markup Language File") + " (*.gml)");
	public static final List<ExtensionFileFilter> IMAGE_FILTERS = Arrays.asList(BMP_FILE_FILTER, JPG_FILE_FILTER, GIF_FILE_FILTER, TIF_FILE_FILTER, PNG_FILE_FILTER, IMG_FILE_FILTER);

	/**
	 * 이미지 전철 Undo-Redo 제한 회수
	 */
	public static final int REDO_UNDO_COUNT = 10;

	public static final ImageIcon OPEN_ARROW_ICON = GuiUtil.getImageIcon("/images/dialogs/open.png");

	public static final ImageIcon CLOSE_ARROW_ICON = GuiUtil.getImageIcon("/images/dialogs/close.png");

	public static final ImageIcon BUILDING_ICON = GuiUtil.getImageIcon("/images/dialogs/building.png", 18, 18);

	public static final ImageIcon UPLOAD_IMAGE_ICON = GuiUtil.getImageIcon("/images/dialogs/upload_image.png", 18, 18);

	public static final ImageIcon VECTOR_ICON = GuiUtil.getImageIcon("/images/dialogs/vector.png", 18, 18);

	public static final ImageIcon VECTOR_LAYER_ICON = GuiUtil.getImageIcon("/images/layer/floorplan.png");

	public static final ImageIcon IMAGE_LAYER_ICON = GuiUtil.getImageIcon("/images/layer/image.png");

	public static final ImageIcon ZOOM_ICON = GuiUtil.getImageIcon("/images/layer/zoom.png");

	public static final ImageIcon GML_ICON = GuiUtil.getImageIcon("/images/layer/gml.png");

	public static final ImageIcon FLOOR_HEIGHT_ICON = GuiUtil.getImageIcon("/images/layer/floorheight.png");

	public static final ImageIcon SHOW_ICON = GuiUtil.getImageIcon("/images/dialogs/show.png");

	public static final ImageIcon HIDE_ICON = GuiUtil.getImageIcon("/images/dialogs/hide.png");

	public static final ImageIcon SHOWHIDE_ICON = GuiUtil.getImageIcon("/images/dialogs/showhide.png", 16, 16);

	public static final ImageIcon SHOWONLY_ICON = GuiUtil.getImageIcon("/images/dialogs/showonly.png", 16, 16);

	public static final ImageIcon MERGE_ICON = GuiUtil.getImageIcon("/images/dialogs/merge.png", 18, 18);

	public static final ImageIcon SPLIT_ICON = GuiUtil.getImageIcon("/images/dialogs/split.png", 18, 18);

	public static final ImageIcon DELETE_ICON = GuiUtil.getImageIcon("/images/dialogs/delete.png", 18, 18);

	public static final ImageIcon MERGE_ICON_16 = GuiUtil.getImageIcon("/images/dialogs/merge.png", 16, 16);

	public static final ImageIcon DELETE_ICON_16 = GuiUtil.getImageIcon("/images/dialogs/delete.png", 16, 16);

	public static final ImageIcon UPSTAIRS_ICON = GuiUtil.getImageIcon("/images/dialogs/upstairs.png", 16, 16);

	public static final ImageIcon DOWNSTAIRS_ICON = GuiUtil.getImageIcon("/images/dialogs/downstairs.png", 16, 16);

	public static final Image MOVE_CURSOR = GuiUtil.getImageIcon("/images/cursor/move_icon.png").getImage();

	public static final Image RESIZE_CURSOR = GuiUtil.getImageIcon("/images/cursor/resize_icon.png").getImage();

	public static final Image ROTATE_CURSOR = GuiUtil.getImageIcon("/images/cursor/rotate_icon.png").getImage();

	public static final String INDOOR_OBJECT_KEY = "class";

	/**
	 * 오브젝트 코드 및 텍스트
	 */
	public static final String[] WALL_CLASS_CODE = {"01", "Wall"};
	public static final String[] WINDOW_CLASS_CODE = {"02", "Window"};
	public static final String[] DOOR_CLASS_CODE = {"03", "Door"};
	public static final String[] STAIR_CLASS_CODE = {"04", "Stair"};
	public static final String[] LIFT_CLASS_CODE = {"05", "Lift"};
	public static final String[] SPACE_CLASS_CODE = {"06", "Space"};
	public static final String[] SUBSPACE_CLASS_CODE = {"07", "Subspace"};
	public static final String[] ETC_CLASS_CODE = {"99", "Etc"};

	public static final String[][] INDOOR_OBJECTS = { WALL_CLASS_CODE, WINDOW_CLASS_CODE, DOOR_CLASS_CODE,
			STAIR_CLASS_CODE, LIFT_CLASS_CODE, SPACE_CLASS_CODE, SUBSPACE_CLASS_CODE };

	public static final String[][] ALL_INDOOR_OBJECTS = { WALL_CLASS_CODE, WINDOW_CLASS_CODE, DOOR_CLASS_CODE,
			STAIR_CLASS_CODE, LIFT_CLASS_CODE, SPACE_CLASS_CODE, SUBSPACE_CLASS_CODE, ETC_CLASS_CODE };

	/**
	 * 층의 기본 높이(미입력 시 사용)
	 */
	public static final double DEFALT_HEIGHT = 2.5;
}
