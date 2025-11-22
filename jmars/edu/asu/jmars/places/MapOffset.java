package edu.asu.jmars.places;

import java.awt.geom.Point2D;

/** Captures the nudge offset for a named source in a named layer. */
public class MapOffset {
	/** The name of the server as defined by {@link edu.asu.jmars.layer.map2.MapServer#getName()}. */
	public final String serverName;
	/** The name of the source in its server as defined by {@link edu.asu.jmars.layer.map2.MapSource#getName(). */
	public final String mapName;
	/** The map offset in world coordinates, must be paired with a projection to be meaningful */
	public final Point2D worldDelta;
	public MapOffset(String serverName, String mapName, Point2D worldDelta) {
		this.serverName = serverName;
		this.mapName = mapName;
		this.worldDelta = worldDelta;
	}
}
