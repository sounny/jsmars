package edu.asu.jmars.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import edu.asu.jmars.Main;
import edu.asu.jmars.lmanager.AddLayerDialog;
import edu.asu.jmars.swing.landmark.search.LandmarkSearchPanel;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.P_OUTLINE_IMG;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;

public final class PannerGlass extends BaseGlass {
	
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static final BufferedImage panner = (BufferedImage)ImageFactory.createImage(P_OUTLINE_IMG
             .withDisplayColor(imgColor)
             .withStrokeColor(imgColor));	
	
	PannerGlass(final LViewManager pannVMan, final LViewManager mainVMan) {
		super(pannVMan, mainVMan);
	}

	MouseInputListener createMouseHandler() {
		final JPopupMenu popup = new JPopupMenu();
		JMenu sub;
		JMenuItem menuItem;

		sub = new JMenu("Zoom");
		ButtonGroup group = new ButtonGroup();
		for (final int zoom: myVMan.getZoomManager().getZoomFactors()) {
			menuItem = new JRadioButtonMenuItem(new AbstractAction(zoom + " Pix/Deg") {
				public void actionPerformed(ActionEvent e) {
					myVMan.getZoomManager().setZoomPPD(zoom, true);
				}
			});
			group.add(menuItem);
			sub.add(menuItem);
			menuItem.setSelected(zoom == myVMan.getZoomManager().getZoomPPD());
		}
		popup.add(sub);

		MouseInputListener mouseHandler = new MouseInputAdapter() {
			boolean menuVisible = false;
			boolean closeAddLayer = false;
			Point mouseDown = null;

			public void mouseClicked(MouseEvent e) {
				if (closeAddLayer) {
					return;
				}
				if (!menuVisible
				&& SwingUtilities.isLeftMouseButton(e)
				&& e.getClickCount() == 1) {
					Point p = e.getPoint();
					Point2D world = myVMan.getProj().screen.toWorld(p);
					Main.testDriver.offsetToWorld(world);
				}
				proxy(e);
			}

			public void mousePressed(MouseEvent e) {
				LandmarkSearchPanel.closeSearchDialog();
				closeAddLayer = false;
				if (AddLayerDialog.getInstance().isShowing()) {
					//Hide the add layer dialog if it is showing and do not propagate the mouse event.
					AddLayerDialog.getInstance().closeAddLayerDialog();
					closeAddLayer = true;					
				} else {
					menuVisible = popup.isVisible();
					if (SwingUtilities.isRightMouseButton(e))
						popup.show(PannerGlass.this, e.getX(), e.getY());
					else
						mouseDown = e.getPoint();
					if (SwingUtilities.isMiddleMouseButton(e))
						fastPan.beg();
					if (! menuVisible && SwingUtilities.isLeftMouseButton(e))
						proxy(e);
				}				
			}

			public void mouseReleased(MouseEvent e) {
				if (closeAddLayer) {
					return;
				}
				if (SwingUtilities.isMiddleMouseButton(e)) {
					fastPan.end(e.getX(), e.getY());
					return;
				}

				if (mouseLast != null) {
					drawLine(mouseDown.x, mouseDown.y, mouseLast.x, mouseLast.y);
					mouseLast = null;
				}
				mouseDown = null;
				if (! menuVisible && SwingUtilities.isLeftMouseButton(e))
					proxy(e);
			}

			public void mouseEntered(MouseEvent e) {
				updateLocation(e.getPoint());
				proxy(e);
			}

			public void mouseExited(MouseEvent e) {
				Main.setStatus(null);
				proxy(e);
			}

			public void mouseMoved(MouseEvent e) {
				updateLocation(e.getPoint());
				proxy(e);
			}

			Point mouseLast = null;

			public void mouseDragged(MouseEvent e) {
				if (closeAddLayer) {
					return;
				}
				// Don't catch menu popup drags
				if (mouseDown == null)
					return;

				if (SwingUtilities.isMiddleMouseButton(e)) {
					mouseDraggedMiddle(e);
					return;
				}

				Point mouseCurr = e.getPoint();

				if (mouseLast != null)
					drawLine(mouseDown.x, mouseDown.y, mouseLast.x, mouseLast.y);
				drawLine(mouseDown.x, mouseDown.y, mouseCurr.x, mouseCurr.y);

				Main.setStatusFromWorld(
					myVMan.getProj().screen.toWorld(mouseDown),
					myVMan.getProj().screen.toWorld(mouseCurr));

				mouseLast = mouseCurr;

				if (! menuVisible && SwingUtilities.isLeftMouseButton(e))
					proxy(e);
			}

			public void mouseDraggedMiddle(MouseEvent e) {
				mouseLast = e.getPoint();
				fastPan.panTo(mouseLast.x - mouseDown.x, mouseLast.y
						- mouseDown.y);
			}
		};
		return mouseHandler;
	}

	public void paintComponent(Graphics g) {
		if (fastPan.paintComponent(g))
			return;

		Graphics2D g2 = (Graphics2D) g;

		g2.transform(myVMan.getProj().getWorldToScreen());

		g2.setStroke(new BasicStroke(0));
		g2.setColor(Color.red);
		g2.draw(mainVMan.getProj().getWorldWindow());
		
		//Draw the dark, semi transparent overlay, if needed,  and "P" logo
		//translate back into screen coords
		g2.transform(myVMan.getProj().getScreenToWorld());		
		Rectangle2D viewRect = myVMan.getProj().getScreenWindow();
		int pad = 5;
		int x = (int)viewRect.getX();
		int y = (int)viewRect.getY();
		int w = (int)viewRect.getWidth();
		int h = (int)viewRect.getHeight();
		/*no overlay as per beta 5.0.6 */	
		if (!BaseGlass.getHideMPIcon()) {
			g2.drawImage(panner, null, x+pad, y+pad);
		}
	}

	/**
	 ** Draws a screen-coordinate line, using a spatial graphics
	 ** context (in time mode, this results in a nicely-curved
	 ** geodesic).
	 **/
	private void drawLine(int x1, int y1, int x2, int y2) {
		MultiProjection proj = myVMan.getProj();

		Graphics2D g2 = (Graphics2D) PannerGlass.this.getGraphics();
		g2.transform(proj.getWorldToScreen());
		g2 = myVMan.wrapWorldGraphics(g2);
		g2.setXORMode(Color.gray);
		g2.setStroke(new BasicStroke(0));
		Graphics2D g2s = proj.createSpatialGraphics(g2);

		Point2D down = proj.screen.toSpatial(x1, y1);
		Point2D curr = proj.screen.toSpatial(x2, y2);
		g2s.draw(new Line2D.Double(down, curr));
		g2s.dispose();
	}
}
