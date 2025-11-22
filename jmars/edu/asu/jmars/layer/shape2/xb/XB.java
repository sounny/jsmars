package edu.asu.jmars.layer.shape2.xb;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.shape2.ColumnEditor;
import edu.asu.jmars.layer.shape2.xb.swing.ColumnSearchPanel;
import edu.asu.jmars.layer.shape2.xb.swing.XBMainPanel;
import javax.swing.JDialog;

public enum XB {

	INSTANCE;

	public JDialog XBDialog = null;
	private XBMainPanel xbpanelbuiltin = null;
	private XBMainPanel xbpanel = null;
	private JPanel contentPane;
	private int screenX, screenY;

	private XB() {
	}

	public void init() {
	}

	public void show(Point locationonscreen) {
		if (XBDialog == null) {
			XBDialog = new JDialog(new Frame(), false);
			XBDialog.setLocation(locationonscreen.x + 40, locationonscreen.y);
			initXBDialog();
		}
		if (!XBDialog.isVisible()) {
			if (screenX != -1 && screenY != -1) {
				XBDialog.setLocation(screenX, screenY);
			} else {
				calculateLocation();
			}
			XBDialog.pack();
			XBDialog.setVisible(true);
		}
	}

	/*
	 * there are 2 panels in XBPanel: 1st panel - all the XB UI 2nd panel - APPLY
	 * and CLOSE buttons. In a built-in mode we don't need these buttons as the
	 * "host" of XBPanel provides their own.
	 */
	public JPanel XBPanel(ColumnEditor host) {
		xbpanelbuiltin = new XBMainPanel(host);
		Component[] componentList = xbpanelbuiltin.getComponents();
		if (componentList.length == 2) {
			xbpanelbuiltin.remove(componentList[1]);
			xbpanelbuiltin.revalidate();
			xbpanelbuiltin.repaint();
		}
		return xbpanelbuiltin;
	}

	public void hide() {
		if (XBDialog != null && XBDialog.isVisible()) {
			XBDialog.setVisible(false);
		}
		ColumnSearchPanel.closeSearchDialog();
	}

	public javax.swing.JTextArea getXBTextComponent() {
		return XBMainPanel.getTextExpr();
	}

	public JPanel getXBResultPreviewComponent() {
		return XBMainPanel.getResultTab();
	}

	public JPanel getXBErrorPreviewComponent() {
		return XBMainPanel.getErrorTab();
	}

	public JTabbedPane getXBTabbedPane() {
		return XBMainPanel.getResultPreviewTabbedPane();
	}

	private void initXBDialog() {
		XBDialog.addComponentListener(new MyComponentListener());
		XBDialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				hide();
			}
		});
		XBDialog.setSize(620, 600);
		XBDialog.setResizable(true);
		XBDialog.setTitle("Expression Builder");
		XBDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		XBDialog.setContentPane(contentPane);
		screenX = -1;
		screenY = -1;
		createUI();
	}

	public boolean isXBVisible() {
		return (XBDialog != null && XBDialog.isVisible());
	}

	private void calculateLocation() {
		int margin = 25;
		JPanel owner = Main.testDriver.locMgr;
		Double dx = owner.getLocationOnScreen().getX();
		Double dy = owner.getLocationOnScreen().getY();
		int x = dx.intValue() + owner.getWidth() / 2 + margin;
		int y = dy.intValue() + owner.getHeight() + margin;
		screenX = x;
		screenY = y;
		XBDialog.setLocation(x, y);
	}

	private void createUI() {
		if (xbpanel == null) {
			xbpanel = new XBMainPanel(XBDialog);
		}
		contentPane.add(xbpanel, BorderLayout.CENTER);
	}

	private static class MyComponentListener implements ComponentListener {

		@Override
		public void componentResized(ComponentEvent e) {
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			ColumnSearchPanel.resetSearchColumnDialogLocationOnScreen();
		}

		@Override
		public void componentShown(ComponentEvent e) {
		}

		@Override
		public void componentHidden(ComponentEvent e) {
		}
	}

	/*
	 * public static void main(String[] args) {
	 * 
	 * java.awt.EventQueue.invokeLater(new Runnable() { public void run() { try { XB
	 * xb = XB.INSTANCE; xb.show(new Point(200,100)); } catch (Exception e) {
	 * e.printStackTrace(); } } }); }
	 */

}
