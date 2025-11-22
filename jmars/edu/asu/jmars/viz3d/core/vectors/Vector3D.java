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
 X, Y", and Z, are represented as
 double-precision (64-bit) floating point numbers.
*/
public final class Vector3D implements Serializable {
	
	static final long serialVersionUID = 1L;	
	
    private double x;
    private double y;
    private double z;

    public static Vector3D getZero() {
        return new Vector3D(0.0, 0.0, 0.0);
    }

    public static Vector3D getUnitX() {
        return new Vector3D(1.0, 0.0, 0.0);
    }

    public static Vector3D getUnitY() {
        return new Vector3D(0.0, 1.0, 0.0);
    }

    public static Vector3D getUnitZ() {
        return new Vector3D(0.0, 0.0, 1.0);
    }

    public static Vector3D getUndefined() {
        return new Vector3D(Double.NaN, Double.NaN, Double.NaN);
    }

    public Vector3D() {
    }

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D(Vector2D v, double z) {
        this.x = v.getX();
        this.y = v.getY();
        this.z = z;
    }
    
    public Vector3D(float[] a) {
    	if (a == null || a.length != 3) {
    		x = 0;
    		y = 0;
    		z = 0;
    	} else {
    		x = a[0];
    		y = a[1];
    		z = a[2];
    	}
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

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

   public Vector2D getXY() {
        return new Vector2D(getX(), getY());
    }

    public double getMagnitudeSquared() {
        return x * x + y * y + z * z;
    }

    public double getMagnitude() {
        return Math.sqrt(getMagnitudeSquared());
    }

    public boolean getIsUndefined() {
        return Double.isNaN(x);
    }

    public Vector3D normalize(RefObject<Double> magnitude) {
        magnitude.argValue = getMagnitude();
        return divide(magnitude.argValue);
    }

    public Vector3D normalize() {
        double magnitude = 0;
        RefObject<Double> tempRef_magnitude = new RefObject<>(magnitude);
        Vector3D tempVar = normalize(tempRef_magnitude);
        magnitude = tempRef_magnitude.argValue;
        return tempVar;
    }

    public Vector3D cross(Vector3D other) {
        return new Vector3D(getY() * other.getZ() - getZ() * other.getY(), getZ() * other.getX() - getX() * other.getZ(), getX() * other.getY() - getY() * other.getX());
    }

    public double dot(Vector3D other) {
        return getX() * other.getX() + getY() * other.getY() + getZ() * other.getZ();
    }

    public Vector3D add(Vector3D addend) {
        return Vector3D.OpAddition(this, addend.clone());
    }

    public Vector3D subtract(Vector3D subtrahend) {
        return Vector3D.OpSubtraction(this, subtrahend.clone());
    }

    public Vector3D multiply(double scalar) {
        return new Vector3D(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public Vector3D multiplyComponents(Vector3D scale) {
        return new Vector3D(getX() * scale.getX(), getY() * scale.getY(), getZ() * scale.getZ());
    }

    public Vector3D divide(double scalar) {
        return new Vector3D(this.x / scalar, this.y / scalar, this.z / scalar);
    }

    public double getMaximumComponent() {
        return Math.max(Math.max(x, y), z);
    }

    public double getMinimumComponent() {
        return Math.min(Math.min(x, y), z);
    }

    public Vector3D getMostOrthogonalAxis() {
        double x = Math.abs(getX());
        double y = Math.abs(getY());
        double z = Math.abs(getZ());

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

    public double angleBetween(Vector3D other) {
        return Math.acos(normalize().dot(other.normalize().clone()));
    }

    public Vector3D rotateAroundAxis(Vector3D axis, double theta) {
        double u = axis.getX();
        double v = axis.getY();
        double w = axis.getZ();

        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);

        double ms = axis.getMagnitudeSquared();
        double m = Math.sqrt(ms);

		return new Vector3D(
				((u * (u * x + v * y + w * z))
						+ (((x * (v * v + w * w)) - (u * (v * y + w * z))) * cosTheta) + (m
						* ((-w * y) + (v * z)) * sinTheta))
						/ ms,
				((v * (u * x + v * y + w * z))
						+ (((y * (u * u + w * w)) - (v * (u * x + w * z))) * cosTheta) + (m
						* ((w * x) - (u * z)) * sinTheta))
						/ ms,
				((w * (u * x + v * y + w * z))
						+ (((z * (u * u + v * v)) - (w * (u * x + v * y))) * cosTheta) + (m
						* (-(v * x) + (u * y)) * sinTheta))
						/ ms);
    }

    public Vector3D negate() {
        return multiply(-1.0);
    }

    public boolean equalsEpsilon(Vector3D other, double epsilon) {
        return (Math.abs(x - other.x) <= epsilon) && (Math.abs(y - other.y) <= epsilon) && (Math.abs(z - other.z) <= epsilon);
    }

    public boolean equals(Vector3D other) {
    	if (other == null || (Double.compare(x, other.x) + Double.compare(y, other.y) + Double.compare(z, other.z)) != 0) {
    		return false;
    	} else {
    		return true;
    	}
    }

    public static Vector3D OpUnaryNegation(Vector3D vector) {
        return new Vector3D(-vector.getX(), -vector.getY(), -vector.getZ());
    }

    public static Vector3D OpAddition(Vector3D left, Vector3D right) {
        return new Vector3D(left.x + right.x, left.y + right.y, left.z + right.z);
    }

    public static Vector3D OpSubtraction(Vector3D left, Vector3D right) {
        return new Vector3D(left.x - right.x, left.y - right.y, left.z - right.z);
    }

    public static Vector3D OpMultiply(Vector3D left, double right) {
        return new Vector3D(left.x * right, left.y * right, left.z * right);
    }

    public static Vector3D OpMultiply(double left, Vector3D right) {
        return Vector3D.OpMultiply(right.clone(), left);
    }

    public static Vector3D OpDivision(Vector3D left, double right) {
        return new Vector3D(left.x / right, left.y / right, left.z / right);
    }

    public static boolean OpGreaterThan(Vector3D left, Vector3D right) {
        return (left.getX() > right.getX()) && (left.getY() > right.getY()) && (left.getZ() > right.getZ());
    }

    public static boolean OpGreaterThanOrEqual(Vector3D left, Vector3D right) {
        return (left.getX() >= right.getX()) && (left.getY() >= right.getY()) && (left.getZ() >= right.getZ());
    }

    public static boolean OpLessThan(Vector3D left, Vector3D right) {
        return (left.getX() < right.getX()) && (left.getY() < right.getY()) && (left.getZ() < right.getZ());
    }

    public static boolean OpLessThanOrEqual(Vector3D left, Vector3D right) {
        return (left.getX() <= right.getX()) && (left.getY() <= right.getY()) && (left.getZ() <= right.getZ());
    }

    public static boolean OpEquality(Vector3D left, Vector3D right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector3D left, Vector3D right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector3D) {
            return equals((Vector3D)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s, %3$s)", getX(), getY(), getZ());
    }

    public int getHashCode() {
        return (new Double(x)).hashCode() ^ (new Double(y)).hashCode() ^ (new Double(z)).hashCode();
    }

    public Vector3F toVector3F() {
        return new Vector3F((float)x, (float)y, (float)z);
    }

    public Vector3I toVector3I() {
        return new Vector3I((int)x, (int)y, (int)z);
    }

    public Vector3H toVector3H() {
        return new Vector3H(x, y, z);
    }

    public Vector3B toVector3B() {    	
        return new Vector3B((Double.compare(x, 0.0) == 0) ? false: true,
        		(Double.compare(y, 0.0) == 0) ? false: true,
        		(Double.compare(z, 0.0) == 0) ? false: true);
    }

    public Vector3D clone() {
        Vector3D varCopy = new Vector3D();

        varCopy.x = this.x;
        varCopy.y = this.y;
        varCopy.z = this.z;

        return varCopy;
    }
    
    public double[] toDoubleArray() {
    	double[] v = {this.x, this.y, this.z};
    	return v;   	
    }
    
    public float[] toFloatArray() {
    	float[] v = {(float)this.x, (float)this.y, (float)this.z};
    	return v;   	
    }

}