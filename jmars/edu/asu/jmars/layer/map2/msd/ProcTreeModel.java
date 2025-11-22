package edu.asu.jmars.layer.map2.msd;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import edu.asu.jmars.layer.map2.CompStageFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.stages.composite.CompositeStage;
import edu.asu.jmars.layer.map2.stages.composite.NoComposite;
import edu.asu.jmars.layer.map2.stages.composite.NoCompositeSettings;
import edu.asu.jmars.layer.map2.stages.composite.SingleComposite;
import edu.asu.jmars.util.DebugLog;

/**
 * TreeModel for {@link ProcTree}. The tree is made up of a vis and a plot sub-node.
 * Each of these can have child MapSources attached to them. The vis sub-node allows
 * only a fixed number of children determined by its aggregation stage, while the
 * plot sub-node allows any number of nodes to be appended to it.
 * 
 * @author saadat
 *
 */
class ProcTreeModel implements TreeModel, PipelineModelListener {
	private static DebugLog log = DebugLog.instance();
	
	List treeModelListeners;
	Object root;
	PipelineModel vis;

	public ProcTreeModel(){
		treeModelListeners = new ArrayList();
		
		root = new Object(){
			public String toString(){
				return "root";
			}
		};
		
		vis = new PipelineModel(CompStageFactory.instance().getStageByName(NoCompositeSettings.stageName));
		vis.addPipelineModelListener(this);
	}
	
	public void setPipelineModel(PipelineModel newVis){
		if (newVis == null || newVis.isResizable())
			throw new IllegalArgumentException();
		
		vis.removePipelineModelListener(this);
		vis = newVis;
		fireStructureChanged(new TreePath(new Object[]{ root, vis }));
	}
	
	public void addTreeModelListener(TreeModelListener l) {
		treeModelListeners.add(l);
	}

	public void removeTreeModelListener(TreeModelListener l) {
		treeModelListeners.remove(l);
	}

	public Object getChild(Object parent, int index) {
		if (parent == root){
			switch(index){
			case 0: return vis;
			}
		}
		else if (parent instanceof PipelineModel){
			return ((PipelineModel)parent).getSource(index);
		}
		
		return null;
	}

	public int getChildCount(Object parent) {
		if (parent == root)
			return 1;
		else if (parent instanceof PipelineModel)
			return ((PipelineModel)parent).getSourceCount();
		return 0;
	}

	public int getIndexOfChild(Object parent, Object child) {
		if (parent == root){
			if (child == vis)
				return 0;
		}
		else if (parent instanceof PipelineModel){
			return ((PipelineModel)parent).getSourceIndex((WrappedMapSource)child);
		}
		return -1;
	}

	public Object getRoot() {
		return root;
	}
	
	public TreePath getVisNodePath(){
		return new TreePath(new Object[]{ root, vis });
	}
	
	public PipelineModel getVisNode(){
		return vis;
	}
	
	public boolean isLeaf(Object node) {
		if (node instanceof WrappedMapSource)
			return true;
		return false;
	}

	public void valueForPathChanged(TreePath path, Object newValue) {
		log.println("valueForPathChanged path:"+path+"  val:"+newValue);
		Object obj = path.getLastPathComponent();
		if (obj instanceof PipelineModel){
			((PipelineModel)obj).setCompStage((CompositeStage)newValue);
		}
		else if (obj instanceof WrappedMapSource){
			PipelineModel pm = (PipelineModel)path.getPathComponent(path.getPathCount()-2);
			WrappedMapSource replaced = (WrappedMapSource)obj;
			int i = pm.getSourceIndex(replaced);
			pm.setSource(i, (MapSource)newValue);
		}
		else {
			log.println("valueForPathChanged UNHANDLED path:"+path+"  val:"+newValue);
		}
	}
	
	public void fireStructureChanged(TreePath path){
		TreeModelEvent tme = new TreeModelEvent(this, path);
		for(Iterator li=treeModelListeners.iterator(); li.hasNext(); ){
			((TreeModelListener)li.next()).treeStructureChanged(tme);
		}
	}
	
	public void fireSourcesInserted(PipelineModel pm, int[] indices, Object[] sources){
		TreeModelEvent tme = new TreeModelEvent(this, new Object[]{ root, pm }, indices, sources);
		for(Iterator li=treeModelListeners.iterator(); li.hasNext(); ){
			((TreeModelListener)li.next()).treeNodesInserted(tme);
		}
	}
	
	public void fireSourcesRemoved(PipelineModel pm, int[] indices, Object[] sources){
		TreeModelEvent tme = new TreeModelEvent(this, new Object[]{ root, pm }, indices, sources);
		for(Iterator li=treeModelListeners.iterator(); li.hasNext(); ){
			((TreeModelListener)li.next()).treeNodesRemoved(tme);
		}
	}
	
	public void fireSourcesChanged(PipelineModel pm, int[] indices, Object[] sources){
		TreeModelEvent tme = new TreeModelEvent(this, new Object[]{ root, pm }, indices, sources);
		for(Iterator li=treeModelListeners.iterator(); li.hasNext(); ){
			((TreeModelListener)li.next()).treeNodesChanged(tme);
		}
	}
	
	////
	//// PipelineModelListener implementation
	////
	public void childrenAdded(PipelineModelEvent e) {
		fireSourcesInserted((PipelineModel)e.getSource(), e.getChildIndices(), e.getChildren());
	}

	public void childrenChanged(PipelineModelEvent e) {
		fireSourcesChanged((PipelineModel)e.getSource(), e.getChildIndices(), e.getChildren());
	}

	public void childrenRemoved(PipelineModelEvent e) {
		fireSourcesRemoved((PipelineModel)e.getSource(), e.getChildIndices(), e.getChildren());
	}

	public void compChanged(PipelineModelEvent e) {
		fireStructureChanged(new TreePath(new Object[]{ root, e.getSource() }));
	}

	public void forwardedEventOccurred(PipelineModelEvent e) {
		// TODO Move initial pipeline construction into the tree and enable this.
	}
}
