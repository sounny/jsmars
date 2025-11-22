package edu.asu.jmars.layer.map2.msd;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.util.DebugLog;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;
/**
 * A limited JTree which is based on {@link ProcTreeModel} which handles vis and plot
 * aggregation nodes and their sources. It also handles drag and drop of MapSource
 * objects from {@link AvailableMapsTree}.
 * 
 * @author saadat
 *
 */
public class ProcTree extends JTree {
	private static DebugLog log = DebugLog.instance();
	
	public ProcTree(){
		super(new ProcTreeModel());
		setEditable(true);
		setRootVisible(false);
		setCellRenderer(new ProcTreeCellRenderer());
		setCellEditor(new ProcTreeCellEditor(this));
		
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				TreePath path = ProcTree.this.getPathForLocation(e.getX(), e.getY());
				if (path != null && !isEditing() && isPathEditable(path) && getSelectionCount() == 1) {
					startEditingAtPath(path);
				}
			}
		});
		
		setExpandedState(((ProcTreeModel)getModel()).getVisNodePath(), true);
		setupDragAndDrop();
	}
	
	private void setupDragAndDrop(){
		setTransferHandler(new ProcTreeTransferHandler());
		setDragEnabled(true);
	}

	public MapSource[] getMapSources(){
		TreePath[] tps = getSelectionPaths();
		if (tps == null){
			log.println("No path selected, returning null.");
			return null;
		}
		
		MapSource[] srcs = new MapSource[tps.length];
		for(int i=0; i<tps.length; i++){
			if (tps[i].getLastPathComponent() instanceof WrappedMapSource){
				srcs[i] = ((WrappedMapSource)tps[i].getLastPathComponent()).getWrappedSource();
				if (srcs[i] == null){
					log.println("A MapSource is null, returning null.");
					return null;
				}
			}
			else {
				log.println("getMapSources: A component of selected path ["+
						tps[i].getLastPathComponent()+
						"] is other than a WrappedMapSource, returning null.");
				return null;
			}
		}
		
		return srcs;
	}
	
	public boolean setMapSources(MapSource[] mapSources){
		TreePath tp = getSelectionPath();
		if (tp == null){
			log.println("Null target path, nothing to do.");
			return false;
		}
		
		if (mapSources == null || mapSources.length == 0){
			log.println("Null or zero length input sources.");
			return false;
		}
		
		if (tp.getLastPathComponent() instanceof WrappedMapSource){
			WrappedMapSource wms = (WrappedMapSource)tp.getLastPathComponent();
			PipelineModel pm = (PipelineModel)tp.getParentPath().getLastPathComponent();
			pm.setSource(pm.getSourceIndex(wms), mapSources[0]);
		}
		else if (tp.getLastPathComponent() instanceof PipelineModel){
			PipelineModel pm = (PipelineModel)tp.getLastPathComponent();
			for(int i=0; i < Math.min(pm.getSourceCount(), mapSources.length); i++){
				pm.setSource(i, mapSources[i]);
			}
			if (pm.getSourceCount() > mapSources.length){
				for(int i=Math.min(pm.getSourceCount(), mapSources.length); i<pm.getSourceCount(); i++){
					pm.setSource(i, mapSources[mapSources.length-1]);
				}
			}
		}
		
		return true;
	}
	
	public boolean isPathEditable(TreePath path){
		Object obj = path.getLastPathComponent();
		if (obj instanceof PipelineModel)
			return true;
		return false;
	}
	
	
	/**
	 * TreeCellRenderer for {@link ProcTree}.
	 * @author saadat
	 *
	 */
	static class ProcTreeCellRenderer extends DefaultTreeCellRenderer {
		JComboBox comboBox;
		JPanel    p;
		
		public ProcTreeCellRenderer(){
			super();
			
			comboBox = new JComboBox();
			p = new JPanel(new BorderLayout());
			p.setOpaque(false);
		}
		
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {

			if (value instanceof PipelineModel){
				PipelineModel ppm = (PipelineModel)value;
				
				Component c = super.getTreeCellRendererComponent(tree, "", sel, expanded, leaf, row, hasFocus);
				comboBox.setModel(new DefaultComboBoxModel(((PipelineModel)value).getPossibleCompStages()));
				comboBox.setSelectedItem(ppm.getCompStage());
				comboBox.setEnabled(isEnabled());
				
				p.removeAll();
				p.add(c, BorderLayout.WEST);
				p.add(comboBox, BorderLayout.CENTER);
				
				return p;
			}
			Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			//since material design
			c.setBackground(((ThemePanel)GUITheme.get("panel")).getBackground());
			
			return c;
		}

	}

	/**
	 * TreeCellEditor for {@link ProcTree}. It is only suitable for the vis and plot
	 * nodes with the tree as it renders the JComboBox giving the user options to select
	 * the aggregation stage.
	 * 
	 * @author saadat
	 *
	 */
	static class ProcTreeCellEditor extends DefaultTreeCellEditor {
		public ProcTreeCellEditor(final JTree tree){
			super(tree, (DefaultTreeCellRenderer)tree.getCellRenderer(), new DefaultCellEditor(new JComboBox()));
			
			// Make sure that the comboBox displays with its popup list open.
			final JComboBox comboBox = (JComboBox)((DefaultCellEditor)this.realEditor).getComponent();
			comboBox.addHierarchyListener(new HierarchyListener(){
				public void hierarchyChanged(HierarchyEvent e) {
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							if (comboBox.isShowing()){
								// Automatically show the drop down box of a combo-box as soon as it is shown as an editor.
								comboBox.showPopup();
							}
							else {
								// Select the first child map-source node when a new vis aggregation is selected by the user.
								ProcTreeModel ptm = ((ProcTreeModel)((ProcTree)tree).getModel());
								TreePath path = ptm.getVisNodePath();
								if (ptm.getVisNode().getSourceCount() > 0){
									path = path.pathByAddingChild(ptm.getVisNode().getSource(0));
									tree.setSelectionPath(path);
								}
							}
						}
					});
				}
				
			});
		}
		
	    public Component getTreeCellEditorComponent(JTree tree, Object value,
				boolean isSelected,
				boolean expanded,
				boolean leaf, final int row) {
	    	
	    	if (value instanceof PipelineModel){
	    		JComboBox comboBox = (JComboBox)((DefaultCellEditor)this.realEditor).getComponent();
	    		comboBox.setModel(new DefaultComboBoxModel(((PipelineModel)value).getPossibleCompStages()));
	    		comboBox.setSelectedItem(((PipelineModel)value).getCompStage());
	    	}
	    	return super.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);
	    }
	}
	
	static class ProcTreeTransferHandler extends TransferHandler {
		List flavorsList = Arrays.asList(new DataFlavor[]{
			MapSourceArrTransferable.mapSrcArrDataFlavor,
		});
		
		public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
			for(int i=0; i<transferFlavors.length; i++)
				if (flavorsList.contains(transferFlavors[i]))
					return true;
			return false;
		}

		protected Transferable createTransferable(JComponent c) {
			ProcTree tree = (ProcTree)c;
			
			MapSource[] srcs = (MapSource[])tree.getMapSources();
			if (srcs == null)
				return null;
			
			return new MapSourceArrTransferable(srcs);
		}

		public int getSourceActions(JComponent c) {
			return TransferHandler.COPY;
		}

		public boolean importData(JComponent comp, Transferable t) {
			if (t == null){
				log.println("Transferable is null.");
				return false;
			}
			
			try {
				MapSource[] srcs = (MapSource[])t.getTransferData((DataFlavor)flavorsList.get(0));
				((ProcTree)comp).setMapSources(srcs);
			}
			catch(UnsupportedFlavorException ex){
				log.println(ex.getStackTrace());
				return false;
			}
			catch(IOException ex){
				log.println(ex.getStackTrace());
				return false;
			}
			catch(ClassCastException ex){
				log.println(ex.getStackTrace());
				return false;
			}

			return true;
		}
	}
}
