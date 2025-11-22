package edu.asu.jmars.viz3d.core.vectors;

import java.io.Serializable;

// Ported and extensively changed from the following:
//
// (C) Copyright 2010 Patrick Cozzi and Kevin Ring
//
// Distributed under the MIT License.
// See License.txt or http://www.opensource.org/licenses/mit-license.php.
//


/** 
 A set of two booleans.
*/
public final class Vector2B implements Serializable {
	
	static final long serialVersionUID = 1L;
	
    public static Vector2B getFalse() {
        return new Vector2B(false, false);
    }

    public static Vector2B getTrue() {
        return new Vector2B(true, true);
    }

    public Vector2B() {
    }

    public Vector2B(boolean x, boolean y) {
        this.x = x;
        this.y = y;
    }

    public boolean getX() {
        return this.x;
    }

    public boolean getY() {
        return this.y;
    }

    public int getXInt() {
        return (this.x) ? 1 : 0;
    }

    public int getYInt() {
        return (this.y) ? 1 : 0;
    }

    public boolean equals(Vector2B other) {
    	if (other == null) {
    		return false;
    	}
        return this.x == other.x && this.y == other.y;
    }

    public static boolean OpEquality(Vector2B left, Vector2B right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Vector2B left, Vector2B right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector2B) {
            return equals((Vector2B)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s)", getX(), getY());
    }

    public int getHashCode() {
        return (getXInt() * 2) + getYInt();
    }

    public Vector2D toVector2D() {
        return new Vector2D(getXInt(), getYInt());
    }

    public Vector2F toVector2F() {
        return new Vector2F(getXInt(), getYInt());
    }

    public Vector2I toVector2I() {
        return new Vector2I(getXInt(), getYInt());
    }

    public Vector2H toVector2H() {
        return new Vector2H(getXInt(), getYInt());
    }

    private boolean x;
    private boolean y;

    public Vector2B clone() {
        Vector2B varCopy = new Vector2B();

        varCopy.x = this.x;
        varCopy.y = this.y;

        return varCopy;
    }
}