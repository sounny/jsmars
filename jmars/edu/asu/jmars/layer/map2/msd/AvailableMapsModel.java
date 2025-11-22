package edu.asu.jmars.layer.map2.msd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerListener;
import edu.asu.jmars.layer.map2.MapSource;

// TODO: cannot handle custom maps with categories at the moment!

/** Adapts between MapServerFactory/MapServer state and a DefaultTreeModel */
public class AvailableMapsModel extends DefaultTreeModel implements MapServerListener {
	private DefaultMutableTreeNode root;
	
	/**
	 * Copies references into this tree model, storing MapServer and MapSource
	 * references as 'user objects' as defined by the TreeNode interface, and
	 * attaches listeners to the observable elements to update the tree model as
	 * changes occur.
	 */
	public AvailableMapsModel() {
		super(new DefaultMutableTreeNode(null));
		root = (DefaultMutableTreeNode)getRoot();
	}
	
	// TODO: when we enable add/remove server, this needs to catch it
	public void serverChanged(MapServer server, boolean adding) {
	}
	
	public void mapChanged(MapSource source, Type changeType) {
		if (changeType == MapServerListener.Type.ADDED) {
			add(source);
		} else if (changeType == MapServerListener.Type.REMOVED) {
			remove(source);
		} else if (changeType == MapServerListener.Type.UPDATED) {
			update(source);
		}
	}
	
	/** Inserts this map source into each of its categories */
	void add(MapSource source) {
		for (String[] category: source.getCategories()) {
			add(source, category);
		}
	}
	
	private void add(MapSource source, String[] category) {
		// find or insert server
		DefaultMutableTreeNode serverNode;
		int pos = find(root, source.getServer());
		if (pos < 0) {
			serverNode = new DefaultMutableTreeNode(source.getServer());
			insertNodeInto(serverNode, root, -pos-1);
		} else {
			serverNode = (DefaultMutableTreeNode)root.getChildAt(pos);
		}
		// find or insert each category
		DefaultMutableTreeNode lastCatNode = serverNode;
		for (String cat: category) {
			DefaultMutableTreeNode catNode;
			pos = find(lastCatNode, cat);
			if (pos < 0) {
				catNode = new DefaultMutableTreeNode(cat);
				insertNodeInto(catNode, lastCatNode, -pos-1);
			} else {
				catNode = (DefaultMutableTreeNode)lastCatNode.getChildAt(pos);
			}
			lastCatNode = catNode;
		}
		// find or insert source
		DefaultMutableTreeNode sourceNode;
		pos = find(lastCatNode, source);
		if (pos < 0) {
			sourceNode = new DefaultMutableTreeNode(source);
			insertNodeInto(sourceNode, lastCatNode, -pos-1);
		} else {
			sourceNode = (DefaultMutableTreeNode)lastCatNode.getChildAt(pos);
		}
	}
	
	private void remove(MapSource source) {
		// find and remove source
		for (Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration(); e.hasMoreElements(); ) {
			DefaultMutableTreeNode node = e.nextElement();
			if (node == root) {
				// never remove the root, so do nothing
			} else if (node.getUserObject().equals(source)) {
				DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
				removeNodeFromParent(node);
				// find and remove each empty category above source
				while (parent != null &&
						parent.getUserObject() instanceof String &&
						parent.getChildCount() == 0) {
					node = parent;
					parent = (DefaultMutableTreeNode)node.getParent();
					removeNodeFromParent(node);
				}
				return;
			}
		}
		// didn't find source, complain loudly
		throw new IllegalArgumentException("Source not in tree model: " + source);
	}
	
	private void update(MapSource source) {
		remove(source);
		add(source);
	}

	/**
	 * Locates a userObject in the tree and returns a path from root to
	 * the node containing that object.
	 * @param userObject A non-null user object.
	 * @return Path (from root) to the node containing the object or <code>null</code>
	 *         if no such object was found anywhere in the tree.
	 */
	public TreePath findUserObject(Object userObject){
		return findUserObject(root, userObject);
	}
	
	private TreePath findUserObject(DefaultMutableTreeNode root, Object userObject){
		if (root.getUserObject() != null && root.getUserObject().equals(userObject))
			return new TreePath(root.getPath());
		
		TreePath found = null;
		for(int i=0; i<root.getChildCount() && found == null; i++)
			found = findUserObject((DefaultMutableTreeNode)root.getChildAt(i), userObject);
		
		return found;
	}
	
	private int find(DefaultMutableTreeNode parent, Object childUserObject) {
		return Collections.binarySearch(
			getChildUserObjects(parent),
			childUserObject,
			userObjectComparator);
	}
	
	/** Returns a list of user objects for each child of this node */
	private List<Object> getChildUserObjects(DefaultMutableTreeNode node) {
		List<Object> out = new ArrayList<Object>();
		for (int i = 0; i < node.getChildCount(); i++)
			out.add(((DefaultMutableTreeNode)node.getChildAt(i)).getUserObject());
		return out;
	}
	
	/**
	 * Compares user objects to sort by these keys:
	 * isServer,
	 * isCategory,
	 * isSource, and
	 * results of toString
	 */
	private Comparator<Object> userObjectComparator  = new Comparator<Object>() {
		public int compare(Object o1, Object o2) {
			if (o1 instanceof MapServer != o2 instanceof MapServer)
				return o1 instanceof MapServer ? -1 : 1;
			if (o1 instanceof String != o2 instanceof String)
				return o1 instanceof String ? -1 : 1;
			if (o1 instanceof MapSource != o2 instanceof MapSource)
				return o1 instanceof MapSource ? -1 : 1;
			return o1.toString().compareTo(o2.toString());
		}
	};
}
