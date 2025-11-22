package edu.asu.jmars.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This class is an object-oriented implementation of the Tarjan algorithm for
 * finding strongly-connected components in a graph. The graph representation is
 * given as a Map<E,Collection<E>> where the value of each key is the objects
 * with an edge from the key.
 */
public final class Tarjan<E> {
	private static class Node<E> {
		int index, lowlink;
		E item;
		public Node(int index, E item) {
			this.index = this.lowlink = index;
			this.item = item;
		}
	}
	/** used to give nodes an index in order of reference traversal */
	private int nextIndex = 0;
	/** the mapping from user-provided instance sof E and the Node class we use locally */
	private final Map<E,Node<E>> nodes = new HashMap<E,Node<E>>();
	/** the stack of nodes we haven't allocated to an scc yet */
	private final Stack<Node<E>> S = new Stack<Node<E>>();
	/** a graph represented as a map */
	private final Map<E,Collection<E>> graph;
	/** strongly connected components */
	private final Collection<Collection<E>> sccs = new ArrayList<Collection<E>>();
	/**
	 * Construct the set of strongly connected components within the graph
	 * defined by a map from each node E to the Collection<E> to which it
	 * has edges.
	 */
	public Tarjan(Map<E,Collection<E>> graph) {
		this.graph = graph;
		for (E v: graph.keySet()) {
			if (!nodes.containsKey(v)) {
				tarjan(v);
			}
		}
	}
	private void tarjan(E v) {
		Node<E> node = new Node<E>(nextIndex++, v);
		nodes.put(v, node);
		S.push(node);
		Collection<E> edges = graph.get(v);
		if (edges != null) {
			for (E vprime: edges) {
				if (!nodes.containsKey(vprime)) {
					tarjan(vprime);
					node.lowlink = Math.min(node.lowlink, nodes.get(vprime).lowlink);
				} else if (S.contains(nodes.get(vprime))) { 
					node.lowlink = Math.min(node.lowlink, nodes.get(vprime).index);
				}
			}
		}
		if (node.lowlink == node.index) {
			List<E> scc = new ArrayList<E>();
			scc.add(node.item);
			Node<E> node2;
			while ((node2 = S.pop()) != node) {
				scc.add(node2.item);
			}
			sccs.add(scc);
		}
	}
	/** @return the list of strongly connected components. */
	public Collection<Collection<E>> getComponents() {
		return sccs;
	}
	/** @return true if the graph given to the constructor is a directed acyclic graph (DAG) as determined by all components having a size of 1 element. */
	public boolean isDAG() {
		boolean isDag = sccs.size() == nodes.size();
		if (isDag) {
			for (Collection<E> scc: sccs) {
				if (scc.size() != 1) {
					isDag = false;
					break;
				}
			}
		}
		return isDag;
	}
}

