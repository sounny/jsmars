package edu.asu.jmars.viz3d.core.vectors;

import java.io.Serializable;

import edu.asu.jmars.viz3d.RefObject;

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
 single-precision (32-bit) floating point numbers.
*/
public final class Vector3F implements Serializable {
	
	static final long serialVersionUID = 1L;
	
    private float x;
    private float y;
    private float z;	
	
    public static Vector3F getZero() {
        return new Vector3F(0.0f, 0.0f, 0.0f);
    }

    public static Vector3F getUnitX() {
        return new Vector3F(1.0f, 0.0f, 0.0f);
    }

    public static Vector3F getUnitY() {
        return new Vector3F(0.0f, 1.0f, 0.0f);
    }

    public static Vector3F getUnitZ() {
        return new Vector3F(0.0f, 0.0f, 1.0f);
    }

    public static Vector3F getUndefined() {
        return new Vector3F(Float.NaN, Float.NaN, Float.NaN);
    }

    public Vector3F() {
    }

    public Vector3F(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3F(Vector2F v, float z) {
        this.x = v.getX();
        this.y = v.getY();
        this.z = z;
    }

    public float getX() {
        return x;
    }
    
    public void setX(float x) {
    	this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
    	this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
    	this.z = z;
    }

    public Vector2D getXY() {
        return new Vector2D(getX(), getY());
    }

    public float getMagnitudeSquared() {
        return x * x + y * y + z * z;
    }

    public float getMagnitude() {
        return (float)Math.sqrt(getMagnitudeSquared());
    }

    public boolean getIsUndefined() {
        return Double.isNaN(x);
    }

    public Vector3F normalize(RefObject<Float> magnitude) {
        magnitude.argValue = getMagnitude();
        return divide(magnitude.argValue);
    }

    public Vector3F normalize() {
        float magnitude = 0F;
        RefObject<Float> tempRef_magnitude = new RefObject<Float>(magnitude);
        Vector3F tempVar = normalize(tempRef_magnitude);
        magnitude = tempRef_magnitude.argValue;
        return tempVar;
    }

    public Vector3F cross(Vector3F other) {
        return new Vector3F(getY() * other.getZ() - getZ() * other.getY(), getZ() * other.getX() - getX() * other.getZ(), getX() * other.getY() - getY() * other.getX());
    }

    public float dot(Vector3F other) {
        return getX() * other.getX() + getY() * other.getY() + getZ() * other.getZ();
    }

    public Vector3F add(Vector3F addend) {
        return Vector3F.OpAddition(this, addend.clone());
    }

    public Vector3F subtract(Vector3F subtrahend) {
        return Vector3F.OpSubtraction(this, subtrahend.clone());
    }

    public Vector3F multiply(float scalar) {
        return new Vector3F(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public Vector3F multiplyComponents(Vector3F scale) {
        return new Vector3F(getX() * scale.getX(), getY() * scale.getY(), getZ() * scale.getZ());
    }

    public Vector3F divide(float scalar) {
        return new Vector3F(this.x / scalar, this.y / scalar, this.z / scalar);
    }

    public Vector3F getMostOrthogonalAxis() {
        float x = Math.abs(getX());
        float y = Math.abs(getY());
        float z = Math.abs(getZ());

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

    public Vector3F negate() {
        return multiply(-1.0f);
    }

    public boolean equalsEpsilon(Vector3F other, float epsilon) {
		return (Math.abs(x - other.x) <= epsilon)
				&& (Math.abs(y - other.y) <= epsilon)
				&& (Math.abs(z - other.z) <= epsilon);
    }

    public boolean equals(Vector3F other) {
    	if (other == null || Float.compare(x, other.x) + Float.compare(y, other.y) + Float.compare(z, other.z) != 0) {
    		return false;
    	} else {
    		return true;
    	}
    }

    public static Vector3F OpUnaryNegation(Vector3F vector) {
        return new Vector3F(-vector.getX(), -vector.getY(), -vector.getZ());
    }

    public static Vector3F OpAddition(Vector3F left, Vector3F right) {
        return new Vector3F(left.x + right.x, left.y + right.y, left.z + right.z);
    }

    public static Vector3F OpSubtraction(Vector3F left, Vector3F right) {
        return new Vector3F(left.x - right.x, left.y - right.y, left.z - right.z);
    }

    public static Vector3F OpMultiply(Vector3F left, float right) {
        return new Vector3F(left.x * right, left.y * right, left.z * right);
    }

    public static Vector3F OpMultiply(float left, Vector3F right) {
        return Vector3F.OpMultiply(right.clone(), left);
    }

    public static Vector3F OpDivision(Vector3F left, float right) {
        return new Vector3F(left.x / right, left.y / right, left.z / right);
    }

    public static boolean OpEquality(Vector3F left, Vector3F right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector3F left, Vector3F right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector3F) {
            return equals((Vector3F)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s, %3$s)", getX(), getY(), getZ());
    }

    public int getHashCode() {
        return (new Float(x)).hashCode() ^ (new Float(y)).hashCode() ^ (new Float(z)).hashCode();
    }

    public Vector3D toVector3D() {
        return new Vector3D(x, y, z);
    }

    public Vector3I toVector3I() {
        return new Vector3I((int)x, (int)y, (int)z);
    }

    public Vector3H toVector3H() {
        return new Vector3H(x, y, z);
    }

    public Vector3B toVector3B() {
        return new Vector3B((Float.compare(x, 0.0f) == 0) ? false: true,
        		(Float.compare(y, 0.0f) == 0) ? false: true,
        		(Float.compare(z, 0.0f) == 0) ? false: true);
    }

    public Vector3F clone() {
        Vector3F varCopy = new Vector3F();

        varCopy.x = this.x;
        varCopy.y = this.y;
        varCopy.z = this.z;

        return varCopy;
    }
}