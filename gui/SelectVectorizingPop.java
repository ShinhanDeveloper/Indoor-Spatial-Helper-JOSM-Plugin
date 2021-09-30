package org.openstreetmap.josm.plugins.indoorSpatialHelper.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * 벡터라이징 모듈 선택을 처리하는 뷰 클래스
 */
public class SelectVectorizingPop extends JDialog {

	private ButtonGroup buttonGroup;
	private int index = -1;

	/**
	 * {@link SelectVectorizingPop} 생성자
	 * @param frame
	 */
	public SelectVectorizingPop(JFrame frame) {
		super(frame, tr("실내 공간 정보 백터화"), true);
		createPopUI();
		this.addWindowListener(new WindowListener(){
			@Override
			public void windowOpened(WindowEvent e) {}

			@Override
			public void windowClosing(WindowEvent e) {
				index = -1;
				SelectVectorizingPop.this.setVisible(false);
			}

			@Override
			public void windowClosed(WindowEvent e) {}

			@Override
			public void windowIconified(WindowEvent e) {}

			@Override
			public void windowDeiconified(WindowEvent e) {}

			@Override
			public void windowActivated(WindowEvent e) {}

			@Override
			public void windowDeactivated(WindowEvent e) {}
		});
	}

	/**
	 * 레이아웃 설정 및 컴포넌트 초기화
	 */
	private void createPopUI() {
		JPanel mainPanel = new JPanel();
		JPanel contentPanel = new JPanel();
		JPanel buttonPanel = new JPanel();

		//======== this ========
		setTitle(tr("백터화 옵션"));
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		//======== mainPanel ========
		mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

		//======== contentPanel ========
		Border border = BorderFactory.createTitledBorder("옵션");
		contentPanel.setBorder(border);
		buttonGroup = new ButtonGroup();
		JRadioButton radio2 = new JRadioButton("Open source 기반");
		contentPanel.add(radio2);
		buttonGroup.add(radio2);
		radio2.setSelected(true);

		mainPanel.add(contentPanel);

		//======== buttonPanel ========
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		JButton btnOk = new JButton(tr("확인"));
		buttonPanel.add(btnOk);
		btnOk.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				int confirmResult = JOptionPane.showConfirmDialog (null, "백터화 작업을 실행 할까요?", "백터화 실행", JOptionPane.YES_NO_OPTION);
				if(confirmResult == JOptionPane.YES_OPTION) {
					index = 0;
					for(Enumeration en = buttonGroup.getElements(); en.hasMoreElements();){
						JRadioButton radio = (JRadioButton)en.nextElement();
						if (radio.getModel() == buttonGroup.getSelection()) {
							SelectVectorizingPop.this.setVisible(false);
							return;
						}
						index++;
					}
					index = -1;
					SelectVectorizingPop.this.setVisible(false);
				} else {
					SelectVectorizingPop.this.requestFocus();
				}
			}
		});

		JButton btnCancel = new JButton(tr("취소"));
		buttonPanel.add(btnCancel);
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				index = -1;
				SelectVectorizingPop.this.setVisible(false);
			}
		});

		mainPanel.add(buttonPanel);

		contentPane.add(mainPanel, BorderLayout.NORTH);
		pack();
		setLocationRelativeTo(getOwner());
	}

	/**
	 * 선택된 모듈의 순번 반환하는 함수
	 * @return 선택된 순번 반환
	 */
	public int getIndex() {
		return index;
	}
}
