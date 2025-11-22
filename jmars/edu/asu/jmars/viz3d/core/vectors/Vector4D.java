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
 A set of 4-dimensional Cartesian coordinates where the four components,
 X, Y, Z, and W
 are represented as double-precision (64-bit) floating point numbers.
*/
public final class Vector4D implements Serializable {
	
	static final long serialVersionUID = 1L;	
	
    public static Vector4D getZero() {
        return new Vector4D(0.0, 0.0, 0.0, 0.0);
    }

    public static Vector4D getUnitX() {
        return new Vector4D(1.0, 0.0, 0.0, 0.0);
    }

    public static Vector4D getUnitY() {
        return new Vector4D(0.0, 1.0, 0.0, 0.0);
    }

    public static Vector4D getUnitZ() {
        return new Vector4D(0.0, 0.0, 1.0, 0.0);
    }

    public static Vector4D getUnitW() {
        return new Vector4D(0.0, 0.0, 0.0, 1.0);
    }

    public static Vector4D getUndefined() {
        return new Vector4D(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    }

    public Vector4D() {
    }

    public Vector4D(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4D(Vector3D v, double w) {
    	this.x = v.getX();
    	this.y = v.getY();
    	this.z = v.getZ();
    	this.w = w;
    }

    public Vector4D(Vector2D v, double z, double w) {
    	this.x = v.getX();
    	this.y = v.getY();
    	this.z = z;
    	this.w = w;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getW() {
        return w;
    }

    public Vector2D getXY() {
        return new Vector2D(getX(), getY());
    }

    public Vector3D getXYZ() {
        return new Vector3D(getX(), getY(), getZ());
    }

    public double getMagnitudeSquared() {
        return x * x + y * y + z * z + w * w;
    }

    public double getMagnitude() {
        return Math.sqrt(getMagnitudeSquared());
    }

    public boolean getIsUndefined() {
        return Double.isNaN(x);
    }

    public Vector4D normalize(RefObject<Double> magnitude) {
        magnitude.argValue = getMagnitude();
        return divide(magnitude.argValue);
    }

    public Vector4D normalize() {
        double magnitude = 0;
        RefObject<Double> tempRef_magnitude = new RefObject<Double>(magnitude);
        Vector4D tempVar = normalize(tempRef_magnitude);
        magnitude = tempRef_magnitude.argValue;
        return tempVar;
    }

    public double dot(Vector4D other) {
        return getX() * other.getX() + getY() * other.getY() + getZ() * other.getZ() + getW() * other.getW();
    }

    public Vector4D add(Vector4D addend) {
        return Vector4D.OpAddition(this, addend.clone());
    }

    public Vector4D subtract(Vector4D subtrahend) {
        return Vector4D.OpSubtraction(this, subtrahend.clone());
    }

    public Vector4D multiply(double scalar) {
        return new Vector4D(this.x * scalar, this.y * scalar, this.z * scalar, this.w * scalar);
    }

    public Vector4D multiplyComponents(Vector4D scale) {
        return new Vector4D(getX() * scale.getX(), getY() * scale.getY(), getZ() * scale.getZ(), getW() * scale.getW());
    }

    public Vector4D divide(double scalar) {
        return new Vector4D(this.x / scalar, this.y / scalar, this.z / scalar, this.w / scalar);
    }

    public Vector4D getMostOrthogonalAxis() {
        double x = Math.abs(getX());
        double y = Math.abs(getY());
        double z = Math.abs(getZ());
        double w = Math.abs(getW());

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

    public Vector4D negate() {
        return multiply(-1.0);
    }

    public boolean equalsEpsilon(Vector4D other, double epsilon) {
        return (Math.abs(x - other.x) <= epsilon) && (Math.abs(y - other.y) <= epsilon) && (Math.abs(z - other.z) <= epsilon) && (Math.abs(w - other.w) <= epsilon);
    }

    public boolean equals(Vector4D other) {
    	if (other == null || (Double.compare(x, other.x) + Double.compare(y, other.y) + Double.compare(z, other.z) + Double.compare(w, other.w)) != 0) {
    		return false;
    	} else {
    		return true;
    	}
    }

    public static Vector4D OpUnaryNegation(Vector4D vector) {
        return new Vector4D(-vector.getX(), -vector.getY(), -vector.getZ(), -vector.getW());
    }

    public static Vector4D OpAddition(Vector4D left, Vector4D right) {
        return new Vector4D(left.x + right.x, left.y + right.y, left.z + right.z, left.w + right.w);
    }

    public static Vector4D OpSubtraction(Vector4D left, Vector4D right) {
        return new Vector4D(left.x - right.x, left.y - right.y, left.z - right.z, left.w - right.w);
    }

    public static Vector4D OpMultiply(Vector4D left, double right) {
        return new Vector4D(left.x * right, left.y * right, left.z * right, left.w * right);
    }

    public static Vector4D OpMultiply(double left, Vector4D right) {
        return Vector4D.OpMultiply(right.clone(), left);
    }

    public static Vector4D OpDivision(Vector4D left, double right) {
        return new Vector4D(left.x / right, left.y / right, left.z / right, left.w / right);
    }

    public static boolean OpEquality(Vector4D left, Vector4D right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector4D left, Vector4D right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector4D) {
            return equals((Vector4D)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s, %3$s, %4$s)", getX(), getY(), getZ(), getW());
    }

    public int getHashCode() {
        return (new Double(x)).hashCode() ^ (new Double(y)).hashCode() ^ (new Double(z)).hashCode() ^ (new Double(w)).hashCode();
    }

    public Vector4F toVector4F() {
        return new Vector4F((float)x, (float)y, (float)z, (float)w);
    }

    public Vector4I toVector4I() {
        return new Vector4I((int)x, (int)y, (int)z, (int)w);
    }

    public Vector4H toVector4H() {
        return new Vector4H(x, y, z, w);
    }

    public Vector4B toVector4B() {
        return new Vector4B((Double.compare(x, 0.0) == 0) ? false: true,
        		(Double.compare(y, 0.0) == 0) ? false: true,
        		(Double.compare(z, 0.0) == 0) ? false: true,
        		(Double.compare(w, 0.0) == 0) ? false: true);
    }

    private double x;
    private double y;
    private double z;
    private double w;

    public Vector4D clone() {
        Vector4D varCopy = new Vector4D();

        varCopy.x = this.x;
        varCopy.y = this.y;
        varCopy.z = this.z;
        varCopy.w = this.w;

        return varCopy;
    }
}