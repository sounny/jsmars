package edu.asu.jmars.tool.strategy;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.PROFILE_IMG;
import java.awt.Color;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.UUID;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.material.component.swingsnackbar.SnackBar;
import org.material.component.swingsnackbar.action.AbstractSnackBarAction;
import edu.asu.jmars.Main;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.profile.ProfileLView;
import edu.asu.jmars.lmanager.Row;
import edu.asu.jmars.swing.snackbar.SnackBarBuilder;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeSnackBar;
import mdlaf.utils.MaterialImageFactory;
import mdlaf.utils.icons.MaterialIconFont;

public class ProfileToolStrategy implements ToolStrategy {

	private static SnackBar snackBar = null;
	private static Icon closeicon = MaterialImageFactory.getInstance().getImage(MaterialIconFont.CLOSE,
			UIManager.getColor("SnackBar.foreground"));
	private static final UUID uuid = UUID.randomUUID();
	protected static boolean focusLOST = false;
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static Icon profileicon = new ImageIcon(ImageFactory.createImage(PROFILE_IMG.withStrokeColor(imgColor)));

	@Override
	public void doMode(int newmode, int oldmode) {
		activateProfileLayer();
		String message = "Profile Line: Click to start drawing and to add points. Double click to complete. Right-click in Main view for more options.";
		runSnackBar(message, false);
	}
	
	@Override
	public void preMode(int newmode, int oldmode) {
		dismissProfileSnackbar();		
		if (oldmode == ToolManager.PROFILE) {
			deactivate();
		}
	}
	
	@Override
	public void postMode(int newmode, int oldmode) {
	}

	private static void activateProfileLayer() {
		outersearch: {
			for (LView lv : LManager.getLManager().viewList) {
				if (lv.getName().equalsIgnoreCase("Profile")) {
					LManager.getLManager().setActiveLView(lv);
					lv.setVisible(true);
					if (((ProfileLView) lv).isCalloutVisible()) {
						((ProfileLView) lv).hideCallout();
					}
					List<Row> rows = LManager.getLManager().getMainPanel().overlayRows;
					for (Row r : rows) {
						if (r.getView() == lv) {
							r.updateVis();
							break outersearch;
						}
					}
				}
			}
		}
	}

	private static void runSnackBar(String message, boolean force) {
		int gap = message.length() / 4;
		if (snackBar == null) {
			snackBar = SnackBarBuilder.build(Main.mainFrame, message, closeicon, profileicon, uuid)
					.setSnackBarBackground(ThemeSnackBar.getBackgroundStandard())
					.setSnackBarForeground(ThemeSnackBar.getForegroundError()).setDuration(SnackBar.LENGTH_INDEFINITE)
					.setPosition(SnackBar.BOTTOM).setMarginBottom(60).setGap(gap)
					.setAction(new AbstractSnackBarAction() {
						@Override
						public void mousePressed(MouseEvent e) {
							SnackBarBuilder.getSnackBarOn(uuid).dismiss();
						}
					});
			snackBar.setFocusable(false);
		}
		if (!focusLOST) {
			if ((!snackBar.isRunning() || force)) {
				snackBar.refresh().run();
			}
		}
		Window parentwindowforsnackbar = SwingUtilities.windowForComponent(snackBar);
		if (parentwindowforsnackbar != null) {
			parentwindowforsnackbar.addWindowFocusListener(new WindowAdapter() {
				public void windowGainedFocus(WindowEvent e) {
					focusLOST = false;
				}

				public void windowLostFocus(WindowEvent e) {
					focusLOST = true;
					if (snackBar != null && (snackBar.isRunning())) {
						snackBar.dismiss();
					}
				}
			});
		}
	}

	public static void dismissProfileSnackbar() {
		if (snackBar != null && snackBar.isRunning()) {
			snackBar.dismiss();
		}
	}

	public static void resetProfileSnackbar() {
		if (snackBar != null && snackBar.isRunning()) {
			int x = (snackBar.getOwner().getX() + ((snackBar.getOwner().getWidth() - snackBar.getWidth()) / 2));
			int y = (snackBar.getOwner().getY() + snackBar.getOwner().getHeight() - snackBar.getHeight()
					- snackBar.getMarginBottom());
			Point point = new Point(x, y);
			snackBar.setLocation(point);
		}
	}

	public static void deactivate() {
		dismissProfileSnackbar();
	}
}
