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
 A set of 4-dimensional cartesian coordinates where the four components,
 X, Y, Z, and W
 are represented as single-precision (32-bit) floating point numbers.
*/
public final class Vector4F implements Serializable {
	
	static final long serialVersionUID = 1L;
	
    private float x;
    private float y;
    private float z;
    private float w;
	
    public static Vector4F getZero() {
        return new Vector4F(0.0f, 0.0f, 0.0f, 0.0f);
    }

    public static Vector4F getUnitX() {
        return new Vector4F(1.0f, 0.0f, 0.0f, 0.0f);
    }

    public static Vector4F getUnitY() {
        return new Vector4F(0.0f, 1.0f, 0.0f, 0.0f);
    }

    public static Vector4F getUnitZ() {
        return new Vector4F(0.0f, 0.0f, 1.0f, 0.0f);
    }

    public static Vector4F getUnitW() {
        return new Vector4F(0.0f, 0.0f, 0.0f, 1.0f);
    }

    public static Vector4F getUndefined() {
        return new Vector4F(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
    }

    public Vector4F() {
    }

    public Vector4F(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4F(Vector3F v, float w) {
        x = v.getX();
        y = v.getY();
        z = v.getZ();
        this.w = w;
    }

    public Vector4F(Vector2F v, float z, float w) {
        x = v.getX();
        y = v.getY();
        this.z = z;
        this.w = w;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getW() {
        return w;
    }

    public Vector2F getXY() {
        return new Vector2F(getX(), getY());
    }

    public Vector3F getXYZ() {
        return new Vector3F(getX(), getY(), getZ());
    }

    public float getMagnitudeSquared() {
        return x * x + y * y + z * z + w * w;
    }

    public float getMagnitude() {
        return (float)Math.sqrt(getMagnitudeSquared());
    }

    public boolean getIsUndefined() {
        return Float.isNaN(x);
    }

    public Vector4F normalize(RefObject<Float> magnitude) {
        magnitude.argValue = getMagnitude();
        return divide(magnitude.argValue);
    }

    public Vector4F normalize() {
        float magnitude = 0F;
        RefObject<Float> tempRef_magnitude = new RefObject<Float>(magnitude);
        Vector4F tempVar = normalize(tempRef_magnitude);
        magnitude = tempRef_magnitude.argValue;
        return tempVar;
    }

    public float dot(Vector4F other) {
        return getX() * other.getX() + getY() * other.getY() + getZ() * other.getZ() + getW() * other.getW();
    }

    public Vector4F add(Vector4F addend) {
        return Vector4F.OpAddition(this, addend.clone());
    }

    public Vector4F subtract(Vector4F subtrahend) {
        return Vector4F.OpSubtraction(this, subtrahend.clone());
    }

    public Vector4F multiply(float scalar) {
        return new Vector4F(this.x * scalar, this.y * scalar, this.z * scalar, this.w * scalar);
    }

    public Vector4F multiplyComponents(Vector4F scale) {
        return new Vector4F(getX() * scale.getX(), getY() * scale.getY(), getZ() * scale.getZ(), getW() * scale.getW());
    }

    public Vector4F divide(float scalar) {
        return new Vector4F(this.x / scalar, this.y / scalar, this.z / scalar, this.w / scalar);
    }

    public Vector4F getMostOrthogonalAxis() {
        float x = Math.abs(getX());
        float y = Math.abs(getY());
        float z = Math.abs(getZ());
        float w = Math.abs(getW());

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

    public Vector4F negate() {
        return multiply(-1.0f);
    }

    public boolean equalsEpsilon(Vector4F other, float epsilon) {
		return (Math.abs(x - other.x) <= epsilon)
				&& (Math.abs(y - other.y) <= epsilon)
				&& (Math.abs(z - other.z) <= epsilon)
				&& (Math.abs(w - other.w) <= epsilon);
    }

    public boolean equals(Vector4F other) {
		if (other == null
				|| Float.compare(x, other.x) + Float.compare(y, other.y)
						+ Float.compare(z, other.z) + Float.compare(z, other.z) != 0) {
			return false;
		} else {
			return true;
		}
    }

    public static Vector4F OpUnaryNegation(Vector4F vector) {
        return new Vector4F(-vector.getX(), -vector.getY(), -vector.getZ(), -vector.getW());
    }

    public static Vector4F OpAddition(Vector4F left, Vector4F right) {
        return new Vector4F(left.x + right.x, left.y + right.y, left.z + right.z, left.w + right.w);
    }

    public static Vector4F OpSubtraction(Vector4F left, Vector4F right) {
        return new Vector4F(left.x - right.x, left.y - right.y, left.z - right.z, left.w - right.w);
    }

    public static Vector4F OpMultiply(Vector4F left, float right) {
        return new Vector4F(left.x * right, left.y * right, left.z * right, left.w * right);
    }

    public static Vector4F OpMultiply(float left, Vector4F right) {
        return Vector4F.OpMultiply(right.clone(), left);
    }

    public static Vector4F OpDivision(Vector4F left, float right) {
        return new Vector4F(left.x / right, left.y / right, left.z / right, left.w / right);
    }

    public static boolean OpEquality(Vector4F left, Vector4F right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector4F left, Vector4F right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector4F) {
            return equals((Vector4F)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s, %3$s, %4$s)", getX(), getY(), getZ(), getW());
    }

    public int getHashCode() {
        return (new Float(x)).hashCode() ^ (new Float(y)).hashCode() ^ (new Float(z)).hashCode() ^ (new Float(w)).hashCode();
    }

    public Vector4D toVector4D() {
        return new Vector4D(x, y, z, w);
    }

    public Vector4I toVector4I() {
        return new Vector4I((int)x, (int)y, (int)z, (int)w);
    }

    public Vector4H toVector4H() {
        return new Vector4H(x, y, z, w);
    }

    public Vector4B toVector4B() {
        return new Vector4B((Float.compare(x, 0.0f) == 0) ? false: true,
        		(Float.compare(y, 0.0f) == 0) ? false: true,
        		(Float.compare(z, 0.0f) == 0) ? false: true,
        		(Float.compare(w, 0.0f) == 0) ? false: true);
    }

    public Vector4F clone() {
        Vector4F varCopy = new Vector4F();

        varCopy.x = this.x;
        varCopy.y = this.y;
        varCopy.z = this.z;
        varCopy.w = this.w;

        return varCopy;
    }
}