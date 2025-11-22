package edu.asu.jmars.layer.util.features;

import java.io.File;

/**
 * FeatureProviders interface between the JMARS FeatureCollection and
 * some external data source. Sources are so often file-based that the
 * generic interface defines isFileBased(), and the load/save methods
 * require a filename, even though it will not be used in the case of
 * database adapters.
 * 
 * The typical lifecycle of a FeatureProvider begins with calling its
 * no-arg constructor, to make the properties available to the
 * FeatureProviderFactory. A call to load(filename) is then made, which
 * returns a SingleFeatureCollection to the caller. The filename is
 * remembered by the caller, and at some point, a call to save() with
 * the same or a new file name may occur.
 * 
 * A no-arg constructor is required for FeatureProviders processed by
 * the FeatureProvider factory. The load and save operations may run
 * on their own thread, since they can take such a long time to complete.
 * Since the FeatureProvider has no control over this, care must be taken
 * to synchronize over all external data changes that the calling thread
 * might also access.
 */
public interface FeatureProvider {
	/**
	 * Return the description of this feature provider (e.g. ESRI Shapefile
	 * Format.)
	 */
	public abstract String getDescription();

	/**
	 * Returns true if this adapts between files and FeatureCollections. When
	 * it's true, a file dialog may include this provider. When it's false,
	 * another mechanism will be needed to initiate loading and saving.
	 */
	public abstract boolean isFileBased();

	/**
	 * Return the file extension, if any.
	 */
	public abstract String getExtension();

	/**
	 * Return a new SingleFeatureCollection. The provider accessible through
	 * getFeatureProvider() on the returned feature collection returns a new
	 * instance of FeatureProvider with the same runtime type as the one
	 * instantiate() is called on.
	 */
	public abstract FeatureCollection load(String fileName);

	/**
	 * Writes out the specified features to the source the FeatureProvider was
	 * constructed from. Returns the number of Features saved.
	 */
	public abstract int save(FeatureCollection fc, String fileName);

	/**
	 * Returns true if the given collection can be completely represented in
	 * the source format of this provider. Many formats do not support all
	 * types of shapes, certain Field specifications, or have column restrictions
	 * such as max field length. If save() is called anyway, the provider will
	 * do what it can, but unrepresentable elements will typically be dropped.
	 */
	public abstract boolean isRepresentable(FeatureCollection fc);

	/**
	 * Returns the names of the files output by a save with the given baseName
	 * and feature collection. This is used to determine what files might be
	 * overwritten in a save, to warn the user.
	 */
	public abstract File[] getExistingSaveToFiles(FeatureCollection fc, String baseName);
	
	/**
	 * Returns true if the given collection should be set as the default feature collection. This means
	 * that immediately after loading this feature collection, the user will be able to add
	 * features, such as polygons, to this collection.
	 */
	public abstract boolean setAsDefaultFeatureCollection();
}
