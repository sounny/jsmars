package edu.asu.jmars.swing.linklabel;

import javax.swing.*;
import javax.swing.plaf.LabelUI;

public class LinkLabel extends JLabel {

	private static final String uiClassID = "LinkLabelUI";

	protected String url;

	public LinkLabel(String text, String url) {
		super(text);
		this.url = url;
	}

	public LinkLabel(String text, String url, Icon icon) {
		super(text);
		this.url = url;
		if (icon != null) {
			setIcon(icon);
			setHorizontalTextPosition(SwingConstants.LEFT);
		}
	}

	public LinkLabel(String url, Icon image, int horizontalAlignment) {
		super(image, horizontalAlignment);
		this.url = url;
	}

	public LinkLabel(String url, Icon image) {
		super(image);
		this.url = url;
	}

	public LinkLabel(String url) {
		this.url = url;
	}

	public LinkLabel(String text, String url, Icon icon, int horizontalAlignment) {
		super(text, icon, horizontalAlignment);
		this.url = url;
	}

	public LinkLabel(String text, String url, int horizontalAlignment) {
		super(text, horizontalAlignment);
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setUI(LabelUI ui) {
		super.setUI(ui);
	}

	@Override
	public void updateUI() {
		if (UIManager.get(getUIClassID()) != null) {
			LabelUI ui = (LabelUI) UIManager.getUI(this);
			setUI(ui);
		} else {
			LabelUI ui = new LinkLabelUI();
			setUI(ui);
		}
	}

	public LabelUI getUI() {
		return (LabelUI) ui;
	}

	@Override
	public String getUIClassID() {
		return uiClassID;
	}
}
