package edu.asu.jmars.viz3d.core.vectors;

import java.io.Serializable;

import edu.asu.jmars.viz3d.core.Half;

// Ported and extensively changed from the following:
//
// (C) Copyright 2010 Patrick Cozzi, Deron Ohlarik, and Kevin Ring
//
// Distributed under the MIT License.
// See License.txt or http://www.opensource.org/licenses/mit-license.php.
//


/** 
 A set of 2-dimensional cartesian coordinates where the two components,
 X and Y, are represented as half-precision (16-bit) floating point numbers.
*/
public final class Vector2H implements Serializable {
	
	static final long serialVersionUID = 1L;
	
    public static Vector2H getZero() {
        return new Vector2H(0.0f, 0.0f);
    }

    public static Vector2H getUnitX() {
        return new Vector2H(1.0f, 0.0f);
    }

    public static Vector2H getUnitY() {
        return new Vector2H(0.0f, 1.0f);
    }

    public static Vector2H getUndefined() {
        return new Vector2H(Half.NaN, Half.NaN);
    }

    public Vector2H() {
    }

    public Vector2H(Half x, Half y) {
        this.x = x.clone();
        this.y = y.clone();
    }

    public Vector2H(float x, float y) {
        this.x = new Half(x);
        this.y = new Half(y);
    }

    public Vector2H(double x, double y) {
        this.x = new Half(x);
        this.y = new Half(y);
    }

    public Half getX() {
        return x;
    }

    public Half getY() {
        return y;
    }

    public boolean getIsUndefined() {
        return x.getIsNaN();
    }

    public boolean equals(Vector2H other) {
        return Half.OpEquality(x.clone(), other.x.clone()) && Half.OpEquality(y.clone(), other.y.clone());
    }

    public static boolean OpEquality(Vector2H left, Vector2H right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector2H left, Vector2H right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector2H) {
            return equals((Vector2H)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s)", getX().clone().toSingle(), getY().clone().toSingle());
    }

    public int getHashCode() {
        return x.getHashCode() ^ y.getHashCode();
    }

    public Vector2D toVector2D() {
        return new Vector2D(x.clone().toDouble(), y.clone().toDouble());
    }

    public Vector2F toVector2F() {
        return new Vector2F(x.clone().toSingle(), y.clone().toSingle());
    }

    public Vector2I toVector2I() {
        return new Vector2I((int)x.toSingle(), (int)y.toSingle());
    }

    public Vector2B toVector2B() {
        return new Vector2B((Float.compare(x.toSingle(), 0.0f) == 0) ? false: true, 
        		(Float.compare(y.toSingle(), 0.0f) == 0) ? false: true);
    }

    private Half x = new Half();
    private Half y = new Half();

    public Vector2H clone() {
        Vector2H varCopy = new Vector2H();

        varCopy.x = this.x.clone();
        varCopy.y = this.y.clone();

        return varCopy;
    }
}