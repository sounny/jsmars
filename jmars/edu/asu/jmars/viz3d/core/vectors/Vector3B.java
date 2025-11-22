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
 A set of three booleans.
*/
public final class Vector3B implements Serializable {
	
	static final long serialVersionUID = 1L;	
	
   public static Vector3B getFalse() {
        return new Vector3B(false, false, false);
    }

    public static Vector3B getTrue() {
        return new Vector3B(true, true, true);
    }


    public Vector3B() {
    }

    public Vector3B(boolean x, boolean y, boolean z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3B(Vector2B v, boolean z) {
        this.x = v.getX();
        this.y = v.getY();
        this.z = z;
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

    public int getXInt() {
        return (x) ? 1 : 0;
    }

    public int getYInt() {
        return (y) ? 1 : 0;
    }

    public int getZInt() {
        return (z) ? 1 : 0;
    }
    
    public Vector2B getXY() {
        return new Vector2B(getX(), getY());
    }

    public boolean equals(Vector3B other) {
    	if (other == null) {
    		return false;
    	}
        return x == other.x && y == other.y && z == other.z;
    }

    public static boolean OpEquality(Vector3B left, Vector3B right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector3B left, Vector3B right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector3B) {
            return equals((Vector3B)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s, %3$s)", getX(), getY(), getZ());
    }

    public int getHashCode() {
        return (getXInt() * 4) + (getYInt() * 2) + getZInt();
    }

    public Vector3D toVector3D() {
        return new Vector3D(getXInt(), getYInt(), getZInt());
    }

    public Vector3F toVector3F() {
        return new Vector3F(getXInt(), getYInt(), getZInt());
    }

    public Vector3I toVector3I() {
        return new Vector3I(getXInt(), getYInt(), getZInt());
    }

    public Vector3H toVector3H() {
        return new Vector3H(getXInt(), getYInt(), getZInt());
    }

    private boolean x;
    private boolean y;
    private boolean z;

    public Vector3B clone() {
        Vector3B varCopy = new Vector3B();

        varCopy.x = this.x;
        varCopy.y = this.y;
        varCopy.z = this.z;

        return varCopy;
    }
}