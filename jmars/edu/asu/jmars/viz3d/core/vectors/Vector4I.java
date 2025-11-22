package edu.asu.jmars.viz3d.core.vectors;

import java.io.Serializable;

//Ported and extensively changed from the following:
//
// (C) Copyright 2010 Patrick Cozzi and Kevin Ring
//
// Distributed under the Boost Software License, Version 1.
// See License.txt or http://www.opensource.org/licenses/mit-license.php.
//


/** 
 A set of 4-dimensional Cartesian coordinates where the four components,
 X, Y, Z, and W
 are represented as integers.
*/
public final class Vector4I implements Serializable {
	
	static final long serialVersionUID = 1L;
	
    public static Vector4I getZero() {
        return new Vector4I(0, 0, 0, 0);
    }

    public static Vector4I getUnitX() {
        return new Vector4I(1, 0, 0, 0);
    }

    public static Vector4I getUnitY() {
        return new Vector4I(0, 1, 0, 0);
    }

    public static Vector4I getUnitZ() {
        return new Vector4I(0, 0, 1, 0);
    }

    public static Vector4I getUnitW() {
        return new Vector4I(0, 0, 0, 1);
    }

    public Vector4I() {
    }

    public Vector4I(int x, int y, int z, int w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4I(Vector3I v, int w) {
    	this.x = v.getX();
    	this.y = v.getY();
    	this.z = v.getZ();
    	this.w = w;
    }

    public Vector4I(Vector2I v, int z, int w) {
        x = v.getX();
        y = v.getY();
        this.z = z;
        this.w = w;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getW() {
        return w;
    }

    public Vector2I getXY() {
        return new Vector2I(getX(), getY());
    }

    public Vector3I getXYZ() {
        return new Vector3I(getX(), getY(), getZ());
    }

    public int getMagnitudeSquared() {
        return x * x + y * y + z * z + w * w;
    }

    public double getMagnitude() {
        return Math.sqrt(getMagnitudeSquared());
    }

    public int dot(Vector4I other) {
        return getX() * other.getX() + getY() * other.getY() + getZ() * other.getZ() + getW() * other.getW();
    }

    public Vector4I add(Vector4I addend) {
        return Vector4I.OpAddition(this, addend.clone());
    }

    public Vector4I subtract(Vector4I subtrahend) {
        return Vector4I.OpSubtraction(this, subtrahend.clone());
    }

    public Vector4I multiply(int scalar) {
        return new Vector4I(x * scalar, y * scalar, z * scalar, w * scalar);
    }

    public Vector4I multiplyComponents(Vector4I scale) {
        return new Vector4I(getX() * scale.getX(), getY() * scale.getY(), getZ() * scale.getZ(), getW() * scale.getW());
    }

    public Vector4I divide(int scalar) {
        return new Vector4I(x / scalar, y / scalar, z / scalar, w / scalar);
    }

    public Vector4I getMostOrthogonalAxis() {
        int x = Math.abs(getX());
        int y = Math.abs(getY());
        int z = Math.abs(getZ());
        int w = Math.abs(getW());

        if ((x < y) && (x < z) && (x < w)) {
            return getUnitX();
        }
        else if ((y < x) && (y < z) && (y < w)) {
            return getUnitY();
        }
        else if ((z < x) && (z < y) && (z < w)) {
            return getUnitZ();
        }
        else {
            return getUnitW();
        }
    }

    public Vector4I negate() {
        return multiply(-1);
    }

    public boolean equals(Vector4I other) {
    	if (other == null) {
    		return false;
    	}
        return x == other.x && y == other.y && z == other.z && w == other.w;
    }

    public static Vector4I OpUnaryNegation(Vector4I vector) {
        return new Vector4I(-vector.getX(), -vector.getY(), -vector.getZ(), -vector.getW());
    }

    public static Vector4I OpAddition(Vector4I left, Vector4I right) {
        return new Vector4I(left.x + right.x, left.y + right.y, left.z + right.z, left.w + right.w);
    }

    public static Vector4I OpSubtraction(Vector4I left, Vector4I right) {
        return new Vector4I(left.x - right.x, left.y - right.y, left.z - right.z, left.w - right.w);
    }

    public static Vector4I OpMultiply(Vector4I left, int right) {
        return new Vector4I(left.x * right, left.y * right, left.z * right, left.w * right);
    }

    public static Vector4I OpMultiply(int left, Vector4I right) {
        return Vector4I.OpMultiply(right.clone(), left);
    }

    public static Vector4I OpDivision(Vector4I left, int right) {
        return new Vector4I(left.x / right, left.y / right, left.z / right, left.w / right);
    }

    public static boolean OpEquality(Vector4I left, Vector4I right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector4I left, Vector4I right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector4I) {
            return equals((Vector4I)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s, %3$s, %4$s)", getX(), getY(), getZ(), getW());
    }

    public int getHashCode() {
        return (new Integer(x)).hashCode() ^ (new Integer(y)).hashCode() ^ (new Integer(z)).hashCode() ^ (new Integer(w)).hashCode();
    }

    public Vector4D toVector4D() {
        return new Vector4D((double)x, (double)y, (double)z, (double)w);
    }

    public Vector4F toVector4F() {
        return new Vector4F((float)x, (float)y, (float)z, (float)w);
    }

    public Vector4H toVector4H() {
        return new Vector4H(x, y, z, w);
    }

    public Vector4B toVector4B() {
        return new Vector4B((x != 0) ? true: false, (y != 0) ? true : false, (z != 0) ? true : false, (w != 0) ? true : false);
    }

    private int x;
    private int y;
    private int z;
    private int w;

    public Vector4I clone() {
        Vector4I varCopy = new Vector4I();

        varCopy.x = this.x;
        varCopy.y = this.y;
        varCopy.z = this.z;
        varCopy.w = this.w;

        return varCopy;
    }
}