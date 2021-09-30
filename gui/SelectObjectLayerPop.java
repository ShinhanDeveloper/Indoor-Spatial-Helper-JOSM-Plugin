package org.openstreetmap.josm.plugins.indoorSpatialHelper.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.ComboItem;
import org.openstreetmap.josm.plugins.indoorSpatialHelper.util.GuiUtil;

/**
 * 오브젝트 레이어 병합, 분리 시 레이어 선택을 처리하는 뷰 클래스
 */
public class SelectObjectLayerPop extends JDialog {

	JComboBox<ComboItem> objectLayerSelect;

	/**
	 * {@link SelectObjectLayerPop} 생성자
	 * @param frame
	 * @param list
	 * @param btnListener
	 * @param isSplit
	 */
	public SelectObjectLayerPop(JFrame frame, List<ComboItem> list, ActionListener btnListener, Boolean isSplit) {
		super(frame, tr("Select target layer"), true);

		JPanel mainPanel = new JPanel();

		objectLayerSelect = new JComboBox<>();
		for(ComboItem item : list){
			objectLayerSelect.addItem(item);
		}
		JButton btnRun = new JButton();
		if(isSplit){
			btnRun.setText(tr("Split"));
		}else{
			btnRun.setText(tr("Merge"));
		}
		btnRun.addActionListener(btnListener);
		JButton btnCancel = new JButton(tr("Cancel"));
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SelectObjectLayerPop.this.dispose();
			}
		});

		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
		mainPanel.setLayout(new GridBagLayout());
		((GridBagLayout) mainPanel.getLayout()).columnWidths = new int[] {0, 0, 0};
		((GridBagLayout) mainPanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
		((GridBagLayout) mainPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
		((GridBagLayout) mainPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

		mainPanel.add(new JLabel(tr("Please select the target layer.")), GuiUtil.getGridBagConst(0, 0, 2, 1));
		mainPanel.add(objectLayerSelect, GuiUtil.getGridBagConst(0, 1, 2, 1));
		mainPanel.add(btnRun, GuiUtil.getGridBagConst(0, 2, 1, 1));
		mainPanel.add(btnCancel, GuiUtil.getGridBagConst(1, 2, 1, 1));

		contentPane.add(mainPanel, BorderLayout.NORTH);

		pack();
		setLocationRelativeTo(getOwner());
	}

	/**
	 * 선택된 오브젝트 레이어 코드를 반환하는 함수
	 * @return 오브젝트 코드 반환
	 */
	public String getSelectedCls() {
		return ((ComboItem)objectLayerSelect.getSelectedItem()).getKey();
	}
}
