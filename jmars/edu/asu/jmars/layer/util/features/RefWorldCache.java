package edu.asu.jmars.layer.util.features;

import java.util.Map;

import org.apache.commons.collections.ReferenceMap;

/**
 * Caches world coordinate FPath instances by mapping the FPath on the given
 * feature to a world coordinate equivalent through a weak reference collection
 * that may be cleaned when the original FPath is no longer referred to anywhere
 * in the jvm.
 */
public class RefWorldCache implements WorldCache {
	private final Map<FPath,FPath> cache = new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.SOFT);
	public synchronized FPath getWorldPath(Feature f) {
		FPath from = f.getPath();
		FPath to = cache.get(from);
		if (to == null) {
			cache.put(from, to=from.getWorld());
		}
		return to;
	}
}
