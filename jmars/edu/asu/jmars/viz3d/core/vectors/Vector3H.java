package edu.asu.jmars.viz3d.core.vectors;

import java.io.Serializable;

import edu.asu.jmars.viz3d.core.Half;

//Ported and extensively changed from the following:
//
// (C) Copyright 2010 Patrick Cozzi, Deron Ohlarik, and Kevin Ring
//
// Distributed under the MIT License.
// See License.txt or http://www.opensource.org/licenses/mit-license.php.
//


/** 
 A set of 3-dimensional Cartesian coordinates where the three components,
 X, Y, and Z, are represented as
 half-precision (16-bit) floating point numbers.
*/
public final class Vector3H implements Serializable {
	
	static final long serialVersionUID = 1L;
	
    public static Vector3H getZero() {
        return new Vector3H(0.0, 0.0, 0.0);
    }

    public static Vector3H getUnitX() {
        return new Vector3H(1.0, 0.0, 0.0);
    }

    public static Vector3H getUnitY() {
        return new Vector3H(0.0, 1.0, 0.0);
    }

    public static Vector3H getUnitZ() {
        return new Vector3H(0.0, 0.0, 1.0);
    }

    public static Vector3H getUndefined() {
        return new Vector3H(Half.NaN, Half.NaN, Half.NaN);
    }

    public Vector3H() {
    }

    public Vector3H(Half x, Half y, Half z) {
        this.x = x.clone();
        this.y = y.clone();
        this.z = z.clone();
    }

    public Vector3H(Vector2H v, Half z) {
        this.x = v.getX().clone();
        this.y = v.getY().clone();
        this.z = z.clone();
    }

    public Vector3H(float x, float y, float z) {
        this.x = new Half(x);
        this.y = new Half(y);
        this.z = new Half(z);
    }

    public Vector3H(double x, double y, double z) {
        this.x = new Half(x);
        this.y = new Half(y);
        this.z = new Half(z);
    }

    public Half getX() {
        return x;
    }

    public Half getY() {
        return y;
    }

    public Half getZ() {
        return z;
    }

    public Vector2H getXY() {
        return new Vector2H(getX().clone(), getY().clone());
    }

    public boolean getIsUndefined() {
        return x.clone().getIsNaN();
    }

    public boolean equals(Vector3H other) {
    	if (other == null) {
    		return false;
    	}
		return Half.OpEquality(x.clone(), other.x.clone())
				&& Half.OpEquality(y.clone(), other.y.clone())
				&& Half.OpEquality(z.clone(), other.z.clone());
    }

    public static boolean OpEquality(Vector3H left, Vector3H right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector3H left, Vector3H right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector3H) {
            return equals((Vector3H)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s, %3$s)", getX().clone(), getY().clone(), getZ().clone());
    }

    public int getHashCode() {
        return x.getHashCode() ^ y.getHashCode() ^ z.getHashCode();
    }

    public Vector3D toVector3D() {
        return new Vector3D(x.clone().toDouble(), y.clone().toDouble(), z.clone().toDouble());
    }

    public Vector3I toVector3I() {
        return new Vector3I((int)x.toSingle(), (int)y.toSingle(), (int)z.toSingle());
    }

    public Vector3F toVector3F() {
        return new Vector3F(x.clone().toSingle(), y.clone().toSingle(), z.clone().toSingle());
    }

    public Vector3B toVector3B() {
        return new Vector3B((Float.compare(x.toSingle(), 0.0f) == 0) ? false: true, 
        		(Float.compare(y.toSingle(), 0.0f) == 0) ? false: true,
        		(Float.compare(z.toSingle(), 0.0f) == 0) ? false: true);
    }

    private Half x = new Half();
    private Half y = new Half();
    private Half z = new Half();

    public Vector3H clone() {
        Vector3H varCopy = new Vector3H();

        varCopy.x = this.x.clone();
        varCopy.y = this.y.clone();
        varCopy.z = this.z.clone();

        return varCopy;
    }
}