package edu.asu.jmars.viz3d.core.vectors;

import java.io.Serializable;

import edu.asu.jmars.viz3d.RefObject;

// Ported and extensively changed from the following:
//
// (C) Copyright 2010 Patrick Cozzi, Deron Ohlarik, and Kevin Ring
//
// Distributed under the MIT License.
// See License.txt or http://www.opensource.org/licenses/mit-license.php.
//


/** 
 A set of 2-dimensional cartesian coordinates where the two components,
 X and Y, are represented as
 single-precision (32-bit) floating point numbers.
*/
public final class Vector2F implements Serializable {
	
	static final long serialVersionUID = 1L;	
	
    public static Vector2F getZero() {
        return new Vector2F(0.0f, 0.0f);
    }

    public static Vector2F getUnitX() {
        return new Vector2F(1.0f, 0.0f);
    }

    public static Vector2F getUnitY() {
        return new Vector2F(0.0f, 1.0f);
    }

    public static Vector2F getUndefined() {
        return new Vector2F(Float.NaN, Float.NaN);
    }

    public Vector2F() {
    }

    public Vector2F(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getMagnitudeSquared() {
        return x * x + y * y;
    }

    public float getMagnitude() {
        return (float)Math.sqrt(getMagnitudeSquared());
    }

    public boolean getIsUndefined() {
        return Float.isNaN(x) || Float.isNaN(y);
    }

    public Vector2F normalize(RefObject<Float> magnitude) {
        magnitude.argValue = getMagnitude();
        return divide(magnitude.argValue);
    }

    public Vector2F normalize() {
        float magnitude = 0F;
        RefObject<Float> tempRef_magnitude = new RefObject<Float>(magnitude);
        Vector2F tempVar = normalize(tempRef_magnitude);
        magnitude = tempRef_magnitude.argValue;
        return tempVar;
    }

    public float dot(Vector2F other) {
        return getX() * other.getX() + getY() * other.getY();
    }

    public Vector2F add(Vector2F addend) {
        return Vector2F.OpAddition(this, addend.clone());
    }

    public Vector2F subtract(Vector2F subtrahend) {
        return Vector2F.OpSubtraction(this, subtrahend.clone());
    }

    public Vector2F multiply(float scalar) {
        return new Vector2F(this.x * scalar, this.y * scalar);
    }

    public Vector2F divide(float scalar) {
        return new Vector2F(this.x / scalar, this.y / scalar);
    }

    public Vector2F negate() {
        return multiply(-1.0f);
    }

    public boolean equalsEpsilon(Vector2F other, float epsilon) {
        return (Math.abs(x - other.x) <= epsilon) && (Math.abs(y - other.y) <= epsilon);
    }

    public boolean equals(Vector2F other) {
    	if (other == null) {
    		return false;
    	}
    	return (Float.compare(x, other.x) + Float.compare(y, other.y)) == 0 ? true : false;    	
    }

    public static Vector2F OpUnaryNegation(Vector2F vector) {
        return new Vector2F(-vector.getX(), -vector.getY());
    }

    public static Vector2F OpAddition(Vector2F left, Vector2F right) {
        return new Vector2F(left.x + right.x, left.y + right.y);
    }

    public static Vector2F OpSubtraction(Vector2F left, Vector2F right) {
        return new Vector2F(left.x - right.x, left.y - right.y);
    }

    public static Vector2F OpMultiply(Vector2F left, float right) {
        return new Vector2F(left.x * right, left.y * right);
    }

    public static Vector2F OpMultiply(float left, Vector2F right) {
        return Vector2F.OpMultiply(right.clone(), left);
    }

    public static Vector2F OpDivision(Vector2F left, float right) {
        return new Vector2F(left.x / right, left.y / right);
    }

    public static boolean OpEquality(Vector2F left, Vector2F right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector2F left, Vector2F right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector2F) {
            return equals((Vector2F)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s)", getX(), getY());
    }

    public int getHashCode() {
        return (new Float(x)).hashCode() ^ (new Float(y)).hashCode();
    }

    public Vector2D toVector2D() {
        return new Vector2D(x, y);
    }

    public Vector2H toVector2H() {
        return new Vector2H(x, y);
    }

    private float x;
    private float y;

    public Vector2F clone() {
        Vector2F varCopy = new Vector2F();

        varCopy.x = this.x;
        varCopy.y = this.y;

        return varCopy;
    }
}