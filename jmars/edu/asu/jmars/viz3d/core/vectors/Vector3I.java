package edu.asu.jmars.viz3d.core.vectors;

import java.io.Serializable;

//Ported and extensively changed from the following:
//
// (C) Copyright 2010 Patrick Cozzi and Kevin Ring
//
// Distributed under the MIT License.
// See License.txt or http://www.opensource.org/licenses/mit-license.php.
//


/** 
 A set of 3-dimensional Cartesian coordinates where the three components,
 X, Y, and Z, are represented as 32-bit integers.
*/
public final class Vector3I implements Serializable {
	
	static final long serialVersionUID = 1L;
	
    public static Vector3I getZero() {
        return new Vector3I(0, 0, 0);
    }

    public static Vector3I getUnitX() {
        return new Vector3I(1, 0, 0);
    }

    public static Vector3I getUnitY() {
        return new Vector3I(0, 1, 0);
    }

    public static Vector3I getUnitZ() {
        return new Vector3I(0, 0, 1);
    }

    public Vector3I() {
    }

    public Vector3I(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3I(Vector2I v, int z) {
        this.x = v.getX();
        this.y = v.getY();
        this.z = z;
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

    public Vector2I getXY() {
        return new Vector2I(getX(), getY());
    }

    public int getMagnitudeSquared() {
        return x * x + y * y + z * z;
    }

    public double getMagnitude() {
        return Math.sqrt(getMagnitudeSquared());
    }

    public Vector3I cross(Vector3I other) {
        return new Vector3I(getY() * other.getZ() - getZ() * other.getY(), getZ() * other.getX() - getX() * other.getZ(), getX() * other.getY() - getY() * other.getX());
    }

    public int dot(Vector3I other) {
        return getX() * other.getX() + getY() * other.getY() + getZ() * other.getZ();
    }

    public Vector3I add(Vector3I addend) {
        return Vector3I.OpAddition(this, addend.clone());
    }

    public Vector3I subtract(Vector3I subtrahend) {
        return Vector3I.OpSubtraction(this, subtrahend.clone());
    }

    public Vector3I multiply(int scalar) {
        return new Vector3I(x * scalar, y * scalar, z *scalar);
    }

    public Vector3I multiplyComponents(Vector3I scale) {
        return new Vector3I(getX() * scale.getX(), getY() * scale.getY(), getZ() * scale.getZ());
    }

    public Vector3I divide(int scalar) {
        return new Vector3I(x / scalar, y / scalar, z / scalar);
    }

    public Vector3I getMostOrthogonalAxis() {
        int x = Math.abs(getX());
        int y = Math.abs(getY());
        int z = Math.abs(getZ());

        if ((x < y) && (x < z)) {
            return getUnitX();
        }
        else if ((y < x) && (y < z)) {
            return getUnitY();
        }
        else {
            return getUnitZ();
        }
    }

    public Vector3I negate() {
        return multiply(-1);
    }

    public boolean equals(Vector3I other) {
    	if (other == null) {
    		return false;
    	}
        return x == other.x && y == other.y && z == other.z;
    }

    public static Vector3I OpUnaryNegation(Vector3I vector) {
        return new Vector3I(-vector.getX(), -vector.getY(), -vector.getZ());
    }

    public static Vector3I OpAddition(Vector3I left, Vector3I right) {
        return new Vector3I(left.x + right.x, left.y + right.y, left.z + right.z);
    }

    public static Vector3I OpSubtraction(Vector3I left, Vector3I right) {
        return new Vector3I(left.x - right.x, left.y - right.y, left.z - right.z);
    }

    public static Vector3I OpMultiply(Vector3I left, int right) {
        return new Vector3I(left.x * right, left.y * right, left.z * right);
    }

    public static Vector3I OpMultiply(int left, Vector3I right) {
        return Vector3I.OpMultiply(right.clone(), left);
    }

    public static Vector3I OpDivision(Vector3I left, int right) {
        return new Vector3I(left.x / right, left.y / right, left.z / right);
    }

    public static boolean OpGreaterThan(Vector3I left, Vector3I right) {
        return (left.getX() > right.getX()) && (left.getY() > right.getY()) && (left.getZ() > right.getZ());
    }

    public static boolean OpGreaterThanOrEqual(Vector3I left, Vector3I right) {
        return (left.getX() >= right.getX()) && (left.getY() >= right.getY()) && (left.getZ() >= right.getZ());
    }

    public static boolean OpLessThan(Vector3I left, Vector3I right) {
        return (left.getX() < right.getX()) && (left.getY() < right.getY()) && (left.getZ() < right.getZ());
    }

    public static boolean OpLessThanOrEqual(Vector3I left, Vector3I right) {
        return (left.getX() <= right.getX()) && (left.getY() <= right.getY()) && (left.getZ() <= right.getZ());
    }

    public static boolean OpEquality(Vector3I left, Vector3I right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector3I left, Vector3I right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector3I) {
            return equals((Vector3I)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s, %3$s)", getX(), getY(), getZ());
    }

    public int getHashCode() {
        return (new Integer(x)).hashCode() ^ (new Integer(y)).hashCode() ^ (new Integer(z)).hashCode();
    }

    public Vector3D toVector3D() {
        return new Vector3D((double)x, (double)y, (double)z);
    }

    public Vector3F toVector3F() {
        return new Vector3F((float)x, (float)y, (float)z);
    }

    public Vector3H toVector3H() {
        return new Vector3H(x, y, z);
    }

    public Vector3B toVector3B() {
        return new Vector3B((x != 0) ? true: false, (y != 0) ? true : false, (z != 0) ? true : false);
    }

    private int x;
    private int y;
    private int z;

    public Vector3I clone() {
        Vector3I varCopy = new Vector3I();

        varCopy.x = this.x;
        varCopy.y = this.y;
        varCopy.z = this.z;

        return varCopy;
    }
}