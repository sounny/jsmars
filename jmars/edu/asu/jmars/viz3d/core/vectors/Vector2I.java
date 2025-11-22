package edu.asu.jmars.viz3d.core.vectors;

import java.io.Serializable;

// Ported and extensively changed from the following:
//
// (C) Copyright 2010 Patrick Cozzi, Deron Ohlarik, and Kevin Ring
//
// Distributed under the MIT License.
// See License.txt or http://www.opensource.org/licenses/mit-license.php.
//


/** 
 A set of 2-dimensional cartesian coordinates where the two components,
 X and Y, are represented as 32-bit integers.
*/
public final class Vector2I implements Serializable {
	
	static final long serialVersionUID = 1L;	
	
    public static Vector2I getZero() {
        return new Vector2I(0, 0);
    }

    public static Vector2I getUnitX() {
        return new Vector2I(1, 0);
    }

    public static Vector2I getUnitY() {
        return new Vector2I(0, 1);
    }

    public Vector2I() {
    }

    public Vector2I(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Vector2I add(Vector2I addend) {
        return Vector2I.OpAddition(this, addend.clone());
    }

    public Vector2I subtract(Vector2I subtrahend) {
        return Vector2I.OpSubtraction(this, subtrahend.clone());
    }

    public Vector2I multiply(int scalar) {
        return new Vector2I(x * scalar, y * scalar);
    }

    public Vector2I negate() {
        return multiply(-1);
    }

    public boolean equals(Vector2I other) {
    	if (other == null) {
    		return false;
    	}
        return x == other.x && y == other.y;
    }

    public static Vector2I OpUnaryNegation(Vector2I vector) {
        return new Vector2I(-vector.getX(), -vector.getY());
    }

    public static Vector2I OpAddition(Vector2I left, Vector2I right) {
        return new Vector2I(left.x + right.x, left.y + right.y);
    }

    public static Vector2I OpSubtraction(Vector2I left, Vector2I right) {
        return new Vector2I(left.x - right.x, left.y - right.y);
    }

    public static Vector2I OpMultiply(Vector2I left, int right) {
        return new Vector2I(left.x * right, left.y * right);
    }

    public static Vector2I OpMultiply(int left, Vector2I right) {
        return Vector2I.OpMultiply(right.clone(), left);
    }

    public static boolean OpEquality(Vector2I left, Vector2I right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector2I left, Vector2I right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector2I) {
            return equals((Vector2I)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s)", getX(), getY());
    }

    public int getHashCode() {
        return (new Integer(x)).hashCode() ^ (new Integer(y)).hashCode();
    }

    public Vector2D toVector2D() {
        return new Vector2D(x, y);
    }

    public Vector2F toVector2F() {
        return new Vector2F(x, y);
    }

    public Vector2H toVector2H() {
        return new Vector2H(x, y);
    }

    public Vector2B toVector2B() {
        return new Vector2B((x != 0) ? true: false, (y != 0) ? true : false);
    }

    private int x;
    private int y;

    public Vector2I clone() {
        Vector2I varCopy = new Vector2I();

        varCopy.x = this.x;
        varCopy.y = this.y;

        return varCopy;
    }
}