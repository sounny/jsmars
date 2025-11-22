package edu.asu.jmars.viz3d.core.vectors;

import java.io.Serializable;

//
// (C) Copyright 2010 Patrick Cozzi and Kevin Ring
//
// Distributed under the MIT License.
// See License.txt or http://www.opensource.org/licenses/mit-license.php.
//

public final class EmulatedVector3D implements Comparable<EmulatedVector3D>, Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EmulatedVector3D() {
    }

    public EmulatedVector3D(Vector3D v) {
        _high = v.toVector3F().clone();
        _low = (Vector3D.OpSubtraction(v.clone(), _high.toVector3D().clone())).toVector3F();
    }

    public Vector3F getHigh() {
        return _high;
    }

    public Vector3F getLow() {
        return _low;
    }

    public boolean equals(EmulatedVector3D other) {
        return Vector3F.OpEquality(_high.clone(), other._high.clone()) && Vector3F.OpEquality(_low.clone(), other._low.clone());
    }

    public static boolean OpEquality(EmulatedVector3D left, EmulatedVector3D right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(EmulatedVector3D left, EmulatedVector3D right) {
        return !left.equals(right.clone());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EmulatedVector3D) {
            return equals((EmulatedVector3D)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%1$s, %2$s)", getHigh().clone(), getLow().clone());
    }

    public int getHashCode() {
        return _high.getHashCode() ^ _low.getHashCode();
    }

    private Vector3F _high = new Vector3F();
    private Vector3F _low = new Vector3F();

    public EmulatedVector3D clone() {
        EmulatedVector3D varCopy = new EmulatedVector3D();

        varCopy._high = this._high.clone();
        varCopy._low = this._low.clone();

        return varCopy;
    }

	@Override
	public int compareTo(EmulatedVector3D o) {
		// TODO Auto-generated method stub
		return 0;
	}
}