package edu.asu.jmars.swing;

import edu.asu.jmars.swing.linklabel.LinkLabel;
import edu.asu.jmars.swing.linklabel.LinkLabelUI;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeText;
import edu.asu.jmars.util.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.net.*;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;

public class UrlLabel extends LinkLabel {
	private static DebugLog log = DebugLog.instance();

	/**
	 * Creates a label that uses the url as the text for the label, and uses the
	 * default color (theme.getHyperlinkTextColor())
	 * 
	 * Uses an underline state when the cursor enters the label. (To simulate a
	 * hyperlink)
	 * 
	 * Has no icon.
	 * 
	 * @param url The url for the label to use
	 */
	public UrlLabel(URL url) {
		this(url.toString(), null);
	}

	/**
	 * Creates a label that uses the url as the text for the label, and uses the
	 * default color (theme.getHyperlinkTextColor())
	 * 
	 * Uses an underline state when the cursor enters the label. (To simulate a
	 * hyperlink)
	 * 
	 * Has no icon.
	 * 
	 * @param url The url for the label to use
	 */
	public UrlLabel(String url) {
		this(url, null);
	}

	/**
	 * Creates a label that uses the url as the text for the label, and uses the
	 * color specified.
	 * 
	 * Uses an underline state when the cursor enters the label. (To simulate a
	 * hyperlink)
	 * 
	 * Has no icon.
	 * 
	 * @param url   The url for the label
	 * @param color Color to display as the label foreground (if color is null, then
	 *              theme.getHyperlinkTextColor() will be used)
	 */
	public UrlLabel(String url, String color) {
		this(url, url, color, null);
	}

	/**
	 * Creates a label with the name given, which will open a browser to the
	 * specified URL. Sets the label to the specified color. If no color is passed
	 * in then the color is the default hyperLink text color from the GUITheme. Uses
	 * an underline state when the cursor enters the label. (To simulate a
	 * hyperlink) Also sets an icon to the RIGHT of the text, and sets an icon text
	 * gap of 10 pixels.
	 * 
	 * @param name  Text displayed in label
	 * @param url   The url which the browser will go to
	 * @param color Color of the label
	 * @param icon  Icon that is displayed to the right of the text
	 */

	public UrlLabel(String name, String url, String color, Icon icon) {
		super(name, url, icon);
		if (color != null) {
			Color colorFromString = hex2Rgb(color);
			setUI(new CustomLinkLabelUI(colorFromString));			
			revalidate();
			repaint();
		} else {
			Color hyperlink = ((ThemeText) GUITheme.get("text")).getHyperlink();			
			setUI(new CustomLinkLabelUI(hyperlink));			
			revalidate();
			repaint();
		}
		if (icon != null) {
			setIconTextGap(10);
		}
		addMouseListener(madapter);	
	}

	private MouseAdapter madapter = new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e)) {
				try {
					Util.launchBrowser(url.trim());
				} catch (Exception ex) {
					log.aprintln(ex);
					log.aprintln(url);
					Util.showMessageDialog("Unable to open browser due to:\n" + ex, "JMARS",
							JOptionPane.ERROR_MESSAGE);
				}
			}
			if (SwingUtilities.isRightMouseButton(e)) {
				// Show right-click popup menu (has copy item)
				showMenu(e.getX(), e.getY());
			}
		}
	};

	// Builds and displays the copy popup menu
	private void showMenu(int x, int y) {
		JPopupMenu rcMenu = new JPopupMenu();
		JMenuItem copyItem = new JMenuItem(copyAct);
		rcMenu.add(copyItem);

		rcMenu.show(this, x, y);
	}

	// Copy url string to clipboard
	private Action copyAct = new AbstractAction("Copy url text") {
		public void actionPerformed(ActionEvent e) {
			StringSelection copyString = new StringSelection(url);
			Clipboard cboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			cboard.setContents(copyString, null);
		}
	};

	/**
	 *
	 * @param colorStr e.g. "#FFFFFF"
	 * @return
	 */
	public static Color hex2Rgb(String colorStr) {
		return new Color(Integer.valueOf(colorStr.substring(1, 3), 16), Integer.valueOf(colorStr.substring(3, 5), 16),
				Integer.valueOf(colorStr.substring(5, 7), 16));
	}

	public static class CustomLinkLabelUI extends LinkLabelUI {

		private Color personalColor;

		public CustomLinkLabelUI() {
		}

		public CustomLinkLabelUI(Color color) {
			this.personalColor = color;
		}

		@SuppressWarnings({ "MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration" })
		public static ComponentUI createUI(JComponent c) {
			return new CustomLinkLabelUI();
		}

		@Override
		public void installUI(JComponent c) {
			super.installUI(c);
			super.foreground = this.personalColor;
			c.setForeground(foreground);
			super.mouseHoverColor = this.personalColor;
		}
	}
}
