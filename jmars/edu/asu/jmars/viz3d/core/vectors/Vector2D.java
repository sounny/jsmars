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
 double-precision (64-bit) floating point numbers.
*/
public final class Vector2D implements Serializable {
	
	static final long serialVersionUID = 1L;	
	
    public static Vector2D getZero() {
        return new Vector2D(0.0, 0.0);
    }

    public static Vector2D getUnitX() {
        return new Vector2D(1.0, 0.0);
    }

    public static Vector2D getUnitY() {
        return new Vector2D(0.0, 1.0);
    }

    public static Vector2D getUndefined() {
        return new Vector2D(Double.NaN, Double.NaN);
    }

    public Vector2D() {
    }

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getMagnitudeSquared() {
        return x * x + y * y;
    }

    public double getMagnitude() {
        return Math.sqrt(getMagnitudeSquared());
    }

    public boolean getIsUndefined() {
        return Double.isNaN(x) || Double.isNaN(y);
    }

    public Vector2D normalize(RefObject<Double> magnitude) {
        magnitude.argValue = getMagnitude();
        return divide(magnitude.argValue);
    }

    public Vector2D normalize() {
        double magnitude = 0;
        RefObject<Double> tempRef_magnitude = new RefObject<Double>(magnitude);
        Vector2D tempVar = normalize(tempRef_magnitude);
        magnitude = tempRef_magnitude.argValue;
        return tempVar;
    }

    public double dot(Vector2D other) {
        return getX() * other.getX() + getY() * other.getY();
    }

    public Vector2D add(Vector2D addend) {
        return Vector2D.OpAddition(this, addend.clone());
    }

    public Vector2D subtract(Vector2D subtrahend) {
        return Vector2D.OpSubtraction(this, subtrahend.clone());
    }

    public Vector2D multiply(double scalar) {
        return new Vector2D(this.x * scalar, this.y * scalar);
    }

    public Vector2D divide(double scalar) {
        return new Vector2D(this.x / scalar, this.y / scalar);
    }


    public Vector2D negate() {
        return multiply(-1.0);
    }

    public boolean equalsEpsilon(Vector2D other, double epsilon) {
        return (Math.abs(x - other.x) <= epsilon) && (Math.abs(y - other.y) <= epsilon);
    }

    public boolean equals(Vector2D other) {
        return (Double.compare(x, other.x) + Double.compare(y, other.y)) == 0 ? true : false;
    }

    public static Vector2D OpUnaryNegation(Vector2D vector) {
        return new Vector2D(-vector.getX(), -vector.getY());
    }

    public static Vector2D OpAddition(Vector2D left, Vector2D right) {
        return new Vector2D(left.x + right.x, left.y + right.y);
    }

    public static Vector2D OpSubtraction(Vector2D left, Vector2D right) {
        return new Vector2D(left.x - right.x, left.y - right.y);
    }

    public static Vector2D OpMultiply(Vector2D left, double right) {
        return new Vector2D(left.x * right, left.y * right);
    }

    public static Vector2D OpMultiply(double left, Vector2D right) {
        return Vector2D.OpMultiply(right.clone(), left);
    }

    public static Vector2D OpDivision(Vector2D left, double right) {
        return new Vector2D(left.x / right, left.y / right);
    }

    public static Vector2D OpDivision(Vector2D left, Vector2D right) {
        return new Vector2D(left.x / right.x, left.y / right.y);
    }

    public static boolean OpEquality(Vector2D left, Vector2D right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector2D left, Vector2D right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector2D) {
            return equals((Vector2D)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s)", getX(), getY());
    }

    public int getHashCode() {
        return (new Double(x)).hashCode() ^ (new Double(y)).hashCode();
    }

    public Vector2F toVector2F() {
        return new Vector2F((float)x, (float)y);
    }

    public Vector2H toVector2H() {
        return new Vector2H(x, y);
    }

    private double x;
    private double y;

    public Vector2D clone() {
        Vector2D varCopy = new Vector2D();

        varCopy.x = this.x;
        varCopy.y = this.y;

        return varCopy;
    }
}