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
 A set of 4-dimensional Cartesian coordinates where the four components,
 X, Y, Z, and W
 are represented as half-precision (16-bit) floating point numbers.
*/
public final class Vector4H implements Serializable {
	
	static final long serialVersionUID = 1L;
	
    public static Vector4H getZero() {
        return new Vector4H(0.0, 0.0, 0.0, 0.0);
    }

    public static Vector4H getUnitX() {
        return new Vector4H(1.0, 0.0, 0.0, 0.0);
    }

    public static Vector4H getUnitY() {
        return new Vector4H(0.0, 1.0, 0.0, 0.0);
    }

    public static Vector4H getUnitZ() {
        return new Vector4H(0.0, 0.0, 1.0, 0.0);
    }

    public static Vector4H getUnitW() {
        return new Vector4H(0.0, 0.0, 0.0, 1.0);
    }

    public static Vector4H getUndefined() {
        return new Vector4H(Half.NaN, Half.NaN, Half.NaN, Half.NaN);
    }

    public Vector4H() {
    }

    public Vector4H(Half x, Half y, Half z, Half w) {
        this.x = x.clone();
        this.y = y.clone();
        this.z = z.clone();
        this.w = w.clone();
    }

    public Vector4H(Vector3H v, Half w) {
    	this.x = v.getX().clone();
    	this.y = v.getY().clone();
    	this.z = v.getZ().clone();
    	this.w = w.clone();
    }

    public Vector4H(Vector2H v, Half z, Half w) {
    	this.x = v.getX().clone();
    	this.y = v.getY().clone();
    	this.z = z.clone();
    	this.w = w.clone();
    }

    public Vector4H(float x, float y, float z, float w) {
    	this.x = new Half(x);
    	this.y = new Half(y);
    	this.z = new Half(z);
    	this.w = new Half(w);
    }

    public Vector4H(double x, double y, double z, double w) {
    	this.x = new Half(x);
    	this.y = new Half(y);
    	this.z = new Half(z);
    	this.w = new Half(w);
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

    public Half getW() {
        return w;
    }

    public Vector2H getXY() {
        return new Vector2H(getX().clone(), getY().clone());
    }

    public Vector3H getXYZ() {
        return new Vector3H(getX().clone(), getY().clone(), getZ().clone());
    }

    public boolean getIsUndefined() {
        return x.clone().getIsNaN();
    }

    public boolean equals(Vector4H other) {
    	if (other == null) {
    		return false;
    	}
        return Half.OpEquality(x.clone(), other.x.clone()) && Half.OpEquality(y.clone(), other.y.clone()) && Half.OpEquality(z.clone(), other.z.clone()) && Half.OpEquality(w.clone(), other.w.clone());
    }

    public static boolean OpEquality(Vector4H left, Vector4H right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector4H left, Vector4H right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector4H) {
            return equals((Vector4H)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s, %3$s, %4$s)", getX().clone(), getY().clone(), getZ().clone(), getW().clone());
    }

    public int getHashCode() {
        return x.getHashCode() ^ y.getHashCode() ^ z.getHashCode() ^ w.getHashCode();
    }

    public Vector4D toVector4D() {
        return new Vector4D(x.clone().toDouble(), y.clone().toDouble(), z.clone().toDouble(), w.clone().toDouble());
    }

    public Vector4F toVector4F() {
        return new Vector4F(x.clone().toSingle(), y.clone().toSingle(), z.clone().toSingle(), w.clone().toSingle());
    }

    public Vector4I toVector4I() {
        return new Vector4I((int)x.toSingle(), (int)y.toSingle(), (int)z.toSingle(), (int)w.toSingle());
    }

    public Vector4B toVector4B() {
        return new Vector4B((Float.compare(x.toSingle(), 0.0f) == 0) ? false: true, 
        		(Float.compare(y.toSingle(), 0.0f) == 0) ? false: true,
        		(Float.compare(z.toSingle(), 0.0f) == 0) ? false: true,
        		(Float.compare(w.toSingle(), 0.0f) == 0) ? false: true);
    }

    private Half x = new Half();
    private Half y = new Half();
    private Half z = new Half();
    private Half w = new Half();

    public Vector4H clone() {
        Vector4H varCopy = new Vector4H();

        varCopy.x = this.x.clone();
        varCopy.y = this.y.clone();
        varCopy.z = this.z.clone();
        varCopy.w = this.w.clone();

        return varCopy;
    }
}