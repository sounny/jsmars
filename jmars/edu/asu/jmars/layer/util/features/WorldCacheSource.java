package edu.asu.jmars.layer.util.features;

import java.util.Set;

public final class WorldCacheSource implements StyleSource<FPath> {
	private static final long serialVersionUID = 1L;
	private final StyleSource<FPath> source;
	private final WorldCache cache;
	public WorldCacheSource(StyleSource<FPath> source, WorldCache cache) {
		this.source = source;
		this.cache = cache;
	}
	public Set<Field> getFields() {
		return source.getFields();
	}
	public FPath getValue(Feature f) {
		return cache.getWorldPath(f);
	}
}
