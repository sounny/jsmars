package edu.asu.jmars.viz3d.renderer.gl;

// Ported from
// (C) Copyright 2009 Patrick Cozzi and Deron Ohlarik
//
// Distributed under the MIT License.
// See License.txt or http://www.opensource.org/licenses/mit-license.php.
//

/**
 * Class represents a mapping of realized JOGL buffer handles as well as an enumeration of
 * their buffer types 
 *
 * thread-safe
 */
public class ClearBuffers {
    public static final ClearBuffers ColorBuffer = new ClearBuffers(1);
    public static final ClearBuffers DepthBuffer = new ClearBuffers(2);
    public static final ClearBuffers StencilBuffer = new ClearBuffers(4);
    public static final ClearBuffers ColorAndDepthBuffer = new ClearBuffers(1 | 2);
    public static final ClearBuffers All = new ClearBuffers(1 | 2 | 4);

    private int intValue;
    private static java.util.HashMap<Integer, ClearBuffers> mappings;
    private static java.util.HashMap<Integer, ClearBuffers> getMappings() {
        if (mappings == null) {
            synchronized (ClearBuffers.class) {
                if (mappings == null) {
                    mappings = new java.util.HashMap<Integer, ClearBuffers>();
                }
            }
        }
        return mappings;
    }

    private ClearBuffers(int value) {
        intValue = value;
        synchronized (ClearBuffers.class) {
            getMappings().put(value, this);
        }
    }

	/**
	 * Returns the ordinal value of the Enumerated type
	 *
	 * @return the value of the type
	 *
	 * thread-safe
	 */
    public int getValue() {
        return intValue;
    }

	/**
	 * Returns the Enumerated type that maps to the input ordinal value
	 *
	 * @param value
	 * @return Enumerated Type
	 *
	 * thread-safe
	 */
    public static ClearBuffers forValue(int value) {
        synchronized (ClearBuffers.class) {
            ClearBuffers enumObj = getMappings().get(value);
            if (enumObj == null) {
                return new ClearBuffers(value);
            }
            else {
                return enumObj;
            }
        }
    }
}