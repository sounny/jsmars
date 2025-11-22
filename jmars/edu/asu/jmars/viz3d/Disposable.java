package edu.asu.jmars.viz3d;



/**
 * Interface for JOGL classes that need to provide a method to release any system resources.
 *
 * Primarily intended for use by JOGL renderable objects.
 *
 * not thread safe
 */
public interface Disposable
{
    /** Disposes of any internal resources allocated by the object. */
    public void dispose();
}
