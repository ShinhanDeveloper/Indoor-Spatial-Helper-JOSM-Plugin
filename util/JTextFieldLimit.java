package org.openstreetmap.josm.plugins.indoorSpatialHelper.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * 텍스트 입력 자리수 제한을 구현한 클래스
 */
public class JTextFieldLimit extends PlainDocument {

	private static final long serialVersionUID = -2482943225734802754L;

	private int limit;

	public JTextFieldLimit(int limit) {
		super();
		this.limit = limit;
	}

	public JTextFieldLimit(int limit, boolean upper){
		super();
		this.limit = limit;
	}

	@Override
	public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
		if(str == null){
			return;
		}

		if((getLength() + str.length()) <= limit){
			super.insertString(offset, str, attr);
		}
	}
}