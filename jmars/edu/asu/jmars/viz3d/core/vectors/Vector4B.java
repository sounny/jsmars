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
 A set of four booleans.
*/
public final class Vector4B implements Serializable {
	
	static final long serialVersionUID = 1L;	
	
    public static Vector4B getFalse() {
        return new Vector4B(false, false, false, false);
    }

    public static Vector4B getTrue() {
        return new Vector4B(true, true, true, true);
    }

    public Vector4B() {
    }

    public Vector4B(boolean x, boolean y, boolean z, boolean w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4B(Vector3B v, boolean w) {
    	this.x = v.getX();
    	this.y = v.getY();
    	this.z = v.getZ();
    	this.w = w;
    }

    public Vector4B(Vector2B v, boolean z, boolean w) {
    	this.x = v.getX();
    	this.y = v.getY();
    	this.z = z;
    	this.w = w;
    }

    public boolean getX() {
        return x;
    }

    public boolean getY() {
        return y;
    }

    public boolean getZ() {
        return z;
    }

    public boolean getW() {
        return w;
    }

    public int getXInt() {
        return (x) ? 1 : 0;
    }

    public int getYInt() {
        return (y) ? 1 : 0;
    }

    public int getZInt() {
        return (z) ? 1 : 0;
    }
    
    public int getWInt() {
        return (w) ? 1 : 0;
    }
    
    public Vector2B getXY() {
        return new Vector2B(getX(), getY());
    }

    public Vector3B getXYZ() {
        return new Vector3B(getX(), getY(), getZ());
    }

    public boolean equals(Vector4B other) {
    	if (other == null) {
    		return false;
    	}
        return x == other.x && y == other.y && z == other.z && w == other.w;
    }

    public static boolean OpEquality(Vector4B left, Vector4B right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector4B left, Vector4B right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector4B) {
            return equals((Vector4B)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s, %3$s, %4$s)", getX(), getY(), getZ(), getW());
    }

    public int getHashCode() {
        return (getXInt() * 8) + (getYInt() * 4) + (getZInt() * 2) + getWInt();
    }

    public Vector4D toVector4D() {
        return new Vector4D((double)getXInt(), (double)getYInt(), (double)getZInt(), (double)getWInt());
    }

    public Vector4F toVector4F() {
System.err.println("x "+(float)getXInt());    	
System.err.println("y "+(float)getYInt());    	
System.err.println("z "+(float)getZInt());    	
System.err.println("w "+(float)getWInt());    	
        return new Vector4F((float)getXInt(), (float)getYInt(), (float)getZInt(), (float)getWInt());
    }

    public Vector4I toVector4I() {
        return new Vector4I(getXInt(), getYInt(), getZInt(), getWInt());
    }

    public Vector4H toVector4H() {
        return new Vector4H(getXInt(), getYInt(), getZInt(), getWInt());
    }

    private boolean x;
    private boolean y;
    private boolean z;
    private boolean w;

    public Vector4B clone() {
        Vector4B varCopy = new Vector4B();

        varCopy.x = this.x;
        varCopy.y = this.y;
        varCopy.z = this.z;
        varCopy.w = this.w;

        return varCopy;
    }
}