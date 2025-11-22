package edu.asu.jmars.layer.shape2.xb.swing;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;
import edu.asu.jmars.swing.IconButtonUI;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeSnackBar;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.CustomBalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;

public enum Messager {
	
	ERROR(ThemeSnackBar.getForegroundError(), ThemeSnackBar.getBackgroundError()) {
	},
	
	STANDARD(ThemeSnackBar.getForegroundStandard(), ThemeSnackBar.getBackgroundStandard()) {
	},
	
	WARNING(ThemeSnackBar.getForegroundWarning(), ThemeSnackBar.getBackgroundWarning()) {
	};
	
	private Color background;
	private Color foreground;
	private CustomBalloonTip myBalloonTip;
	private static Color imgcolor = ThemeProvider.getInstance().getImage().getFill();
	private static Icon close = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CLEAR.withDisplayColor(imgcolor)));
	private  JButton closebutton;	
	private JTextArea comp1;
	private JScrollPane sp;
	
	static {
		BalloonTip.setDefaultCloseButtonIcons(close, close, close);
	}
	
	
	private Messager (Color front, Color back) {
		this.background = back;
		this.foreground = front;
		createCalloutUI();
		createContentUI();
	}
	
	private void createContentUI() {
		comp1 = new JTextArea(8,1);
		comp1.setEditable(false);
		//comp1.setWrapStyleWord(true);
		DefaultCaret caret = (DefaultCaret) comp1.getCaret();
		caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		comp1.setBackground(this.background);
		comp1.setForeground(this.foreground);
		sp = new JScrollPane(comp1);
		sp.setPreferredSize(new Dimension(300, 70));
		sp.setViewportView(comp1);
		sp.setBackground(this.background);				
	}

	private  void createCalloutUI() {
		JLabel dummy = new JLabel();
		EdgedBalloonStyle style = new EdgedBalloonStyle(this.background, 
	                ThemeProvider.getInstance().getBackground().getBorder());
		
		 myBalloonTip = new CustomBalloonTip(dummy,
				  dummy,
				  new Rectangle(),
				  style,
				  BalloonTip.Orientation.LEFT_ABOVE, BalloonTip.AttachLocation.WEST,
				  1, 10,
				  true);	
		 myBalloonTip.setPadding(5);
		 closebutton = BalloonTip.getDefaultCloseButton();
		 myBalloonTip.setCloseButton(closebutton,false);	
		 closebutton.setUI(new IconButtonUI());	
		 myBalloonTip.setVisible(false);
	}	
	
	public void showCallout(Container parent2, List<String> msg2) {	
		if (myBalloonTip != null && parent2 != null) {
			if (parent2 instanceof JComponent) {
				JComponent comp = (JComponent) parent2;
				if (comp.getRootPane() == null) {
					return;
				}
				myBalloonTip.setAttachedComponent(comp);
				int xoffset = parent2.getX() + 100;
				int yoffset = parent2.getHeight();
				Rectangle rectoffset = new Rectangle(xoffset, yoffset, 1, 1);
				String res = "";
				for (int i =0; i < msg2.size(); i++) {
					res = res + msg2.get(i) + "\n";
				}
				comp1.setText(res);
				myBalloonTip.setContents(sp);
				myBalloonTip.setOffset(rectoffset);
				myBalloonTip.setVisible(true);
			}
		}
	}
	
	public void hideCallout() {
		if (myBalloonTip != null) {
			myBalloonTip.setVisible(false);
		}
	}

	public boolean isCalloutVisible() {
		return (myBalloonTip != null && myBalloonTip.isVisible());
	}

	public Color getForeground() {
		return this.foreground;
	}
	
}
	 
