package edu.asu.jmars.layer.map2.msd;

import java.awt.Color;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.LinkedList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapAttrReceiver;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.Main;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeLabel;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

/**
 * A customized JTree which handles available {@link MapSource}s.
 *  
 * @author saadat
 *
 */
public class AvailableMapsTree extends JTree {
	private static DebugLog log = DebugLog.instance();
	
	public AvailableMapsTree() {
		//setCellRenderer(new MapTreeCellRenderer());
		setupDragAndDrop();
	}
	
	private void setupDragAndDrop(){
		setTransferHandler(new AvailableMapsTreeTransferHandler());
		setDragEnabled(true);
	}
	
	public MapSource[] getSelectedMapSources() {
		TreePath[] tps = getSelectionPaths();
		if (tps == null || tps.length == 0)
			return null;

		MapSource[] srcs = new MapSource[tps.length];
		for (int i = 0; i < tps.length; i++) {
			Object obj = ((DefaultMutableTreeNode) tps[i].getLastPathComponent()).getUserObject();
			if (obj instanceof MapSource) {
				srcs[i] = (MapSource) obj;
			} else {
				log.println("One of selected nodes is not a MapSource, returning null.");
				return null;
			}
		}
		return srcs;
	}
	
	public MapServer getSelectedMapServer() {
		TreePath[] tps = getSelectionPaths();
		if (tps == null || tps.length != 1)
			return null;
		Object obj = ((DefaultMutableTreeNode) tps[0].getLastPathComponent()).getUserObject();
		if (obj instanceof MapServer)
			return (MapServer)obj;
		else
			return null;
	}
	
	public MapSource getSelectedMapSource() {
		MapSource[] sources = getSelectedMapSources();
		if (sources != null && sources.length == 1) {
			return sources[0];
		} else {
			return null;
		}
	}
	
	/**
	 * Prevents multiple paint requests from causing multiple data requests that
	 * cause multiple repaints that... bleck
	 */
	static List<MapSource> unresolvedSources = new LinkedList<MapSource>();
	
	static class MapTreeCellRenderer extends DefaultTreeCellRenderer {
//		private static ImageIcon server = Util.loadIcon("resources/wmsserver.gif");
		private static ImageIcon numericSource = Util.loadIcon("resources/numsource.gif");
		private static ImageIcon grayscaleSource = Util.loadIcon("resources/graysource.gif");
		private static ImageIcon colorSource = Util.loadIcon("resources/colorsource.gif");
		private static ImageIcon badSource = Util.loadIcon("resources/error.gif");
		private static ImageIcon unkSource = Util.loadIcon("resources/unknown.gif");
		public Component getTreeCellRendererComponent(final JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			final JLabel label = (JLabel)super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			//For material look and feel
			Color background = ((ThemeLabel)GUITheme.get("label")).getBackgroundhilight();
			label.setBackground(background);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
			Object obj = node.getUserObject();
			if (obj instanceof MapServer) {
//				label.setIcon(server);
			} else if (obj instanceof MapSource) {
				final MapSource source = (MapSource)obj;
				if (source.getOwner() != null && !source.getOwner().equalsIgnoreCase(Main.USER)) {
					label.setText(label.getText() + " - Owned By " + source.getOwner());
				}
				MapAttr attr = source.getMapAttr();
				if (attr == null) {
					label.setIcon(unkSource);
					if (!unresolvedSources.contains(source)) {
						unresolvedSources.add(source);
						source.getMapAttr(new MapAttrReceiver() {
							public void receive(MapAttr attr) {
								tree.repaint();
								unresolvedSources.remove(source);
							}
						});
					}
				} else  if (source.getMapAttr().isColor()) {
					label.setIcon(colorSource);
				} else if (source.getMapAttr().isGray()) {
					label.setIcon(grayscaleSource);
				} else if (source.getMapAttr().isNumeric()) {
					label.setIcon(numericSource);
				} else if (source.getMapAttr().isFailed()) {
					label.setIcon(badSource);
				}
			}
			return label;
		}
	}
	static class AvailableMapsTreeTransferHandler extends TransferHandler {
		public int getSourceActions(JComponent c) {
			return TransferHandler.COPY;
		}

		public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
			return false;
		}

		protected Transferable createTransferable(JComponent c) {
			AvailableMapsTree tree = (AvailableMapsTree)c;
			MapSource[] srcs = tree.getSelectedMapSources();

			if (srcs == null)
				return null;

			return new MapSourceArrTransferable(srcs);
		}
	}
}

