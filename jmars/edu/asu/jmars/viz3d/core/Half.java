package edu.asu.jmars.viz3d.core;

import java.io.Serializable;

import edu.asu.jmars.viz3d.RefObject;

// Ported and extensively modified from the following:

// (C) Copyright 2010 Patrick Cozzi, Deron Ohlarik, and Kevin Ring
//
// Distributed under the MIT License.
// See License.txt or http://www.opensource.org/licenses/mit-license.php.
//

// This type is based on OpenTK.Half in the Open Toolkit Library and is
// governed by the following license:

// Copyright (c) 2006 - 2008 The Open Toolkit library.

// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this software and associated documentation files (the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
// of the Software, and to permit persons to whom the Software is furnished to do
// so, subject to the following conditions:

// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

// The conversion functions are derived from OpenEXR's implementation and are
// governed by the following license:

// Copyright (c) 2002, Industrial Light & Magic, a division of Lucas
// Digital Ltd. LLC

// All rights reserved.

// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
// *       Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// *       Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
// *       Neither the name of Industrial Light & Magic nor the names of
// its contributors may be used to endorse or promote products derived
// from this software without specific prior written permission. 

// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


/** 
 A half-precision (16-bit) floating-point number.  This format has
 1 sign bit, 5 exponent bits, and 11 significand bits (10 explicitly
 stored).  It conforms to the IEEE 754-2008 binary16 format. 
*/
public final class Half implements java.lang.Comparable<Half>, Serializable {
	
	static final long serialVersionUID = 1L;
    /** 
     Represents a value that is not-a-number (NaN).
    */
    public static final Half NaN = new Half(0x7C01);;

    /** 
     Represents positive infinity.
    */
    public static final Half positiveInfinity = new Half(31744);

    /** 
     Represents negative infinity.
    */
    public static final Half negativeInfinity = new Half(64512);

    /** 
     Determines if this instance is zero.  It returns true
     if this instance is either positive or negative zero.
    */
    public boolean getIsZero() {
        return (bits == 0) || (bits == 0x8000);
    }

    /** 
     Determines if this instance is Not-A-Number (NaN).
    */
    public boolean getIsNaN() {
        return (((bits & 0x7C00) == 0x7C00) && (bits & 0x03FF) != 0x0000);
    }

    /** 
     Determines if this instance represents positive infinity.
    */
    public boolean getIsPositiveInfinity() {
        return (bits == 31744);
    }

    /** 
     Determines if this instance represents negative infinity.
    */
    public boolean getIsNegativeInfinity() {
        return (bits == 64512);
    }

    /** 
     Determines if this instance represents either positive or negative infinity.
    */
    public boolean getIsInfinity() {
        return (bits & 31744) == 31744;
    }

    /** 
     Initializes a new instance from a 32-bit single-precision floating-point number.
     
     @param value A 32-bit, single-precision floating-point number.
    */
    public Half() {
    }

    public Half(float value) {
        bits = doubleToHalf(Double.doubleToLongBits(value));
    }

    /** 
     Initializes a new instance from a 64-bit double-precision floating-point number.
     
     @param value A 64-bit, double-precision floating-point number.
    */
    public Half(double value) {
        bits = doubleToHalf(Double.doubleToLongBits(value));
    }

    private Half(char value) {
        bits = value;
    }

    private Half(int value) {
        bits = (char)value;
    }
    
    /** 
     Converts this instance to a 32-bit, single-precision floating-point number.
     @return The 32-bit, single-precision floating-point number.
    */
    public float toSingle() {
    	return Float.intBitsToFloat(halfToFloat(bits));
    }

    /** 
     Converts this instance to a 64-bit, double-precision floating-point number.
     @return The 64-bit, double-precision floating-point number.
    */
    public double toDouble() {
    	return Float.intBitsToFloat(halfToFloat(bits));
    }

    private static char doubleToHalf(long bits) {
        // Our double-precision floating point number, F, is represented by the bit pattern in long i.
        // Disassemble that bit pattern into the sign, S, the exponent, E, and the significand, M.
        // Shift S into the position where it will go in in the resulting half number.
        // Adjust E, accounting for the different exponent bias of double and half (1023 versus 15).

        int sign = (int)((bits >> 48) & 0x00008000);
        int exponent = (int)(((bits >> 52) & 0x7FF) - (1023 - 15));
        long mantissa = bits & 0xFFFFFFFFFFFFFL;
        					    

        // Now reassemble S, E and M into a half:

        if (exponent <= 0) {
            if (exponent < -10) {
                // E is less than -10. The absolute value of F is less than Half.MinValue
                // (F may be a small normalized float, a denormalized float or a zero).
                //
                // We convert F to a half zero with the same sign as F.

                return (char)sign;
            }

            // E is between -10 and 0. F is a normalized double whose magnitude is less than Half.MinNormalizedValue.
            //
            // We convert F to a denormalized half.

            // Add an explicit leading 1 to the significand.

            mantissa = mantissa | 0x10000000000000L;

            // Round to M to the nearest (10+E)-bit value (with E between -10 and 0); in case of a tie, round to the nearest even value.
            //
            // Rounding may cause the significand to overflow and make our number normalized. Because of the way a half's bits
            // are laid out, we don't have to treat this case separately; the code below will handle it correctly.

            int t = 43 - exponent;
            long a = (1L << (t - 1)) - 1;
            long b = (mantissa >> t) & 1;

            mantissa = (mantissa + a + b) >> t;

            // Assemble the half from S, E (==zero) and M.

            return (char)(sign | (int)mantissa);
        }
        else if (exponent == 0x7ff - (1023 - 15)) {
            if (mantissa == 0) {
                // F is an infinity; convert F to a half infinity with the same sign as F.

                return (char)(sign | 0x7c00);
            }
            else {
                // F is a NAN; we produce a half NAN that preserves the sign bit and the 10 leftmost bits of the
                // significand of F, with one exception: If the 10 leftmost bits are all zero, the NAN would turn 
                // into an infinity, so we have to set at least one bit in the significand.

                int mantissa32 = (int)(mantissa >> 42);
                return (char)(sign | 0x7c00 | mantissa32 | ((mantissa == 0) ? 1 : 0));
            }
        }
        else {
            // E is greater than zero.  F is a normalized double. We try to convert F to a normalized half.

            // Round to M to the nearest 10-bit value. In case of a tie, round to the nearest even value.

            mantissa = mantissa + 0x1FFFFFFFFFFL + ((mantissa >> 42) & 1);

            if ((mantissa & 0x10000000000000L) != 0) {
                mantissa = 0; // overflow in significand,
                exponent += 1; // adjust exponent
            }

            // exponent overflow
            if (exponent > 30) {
                throw new ArithmeticException("Half: Hardware floating-point overflow.");
            }

            // Assemble the half from S, E and M.

            return (char)(sign | (exponent << 10) | (int)(mantissa >> 42));
        }
    }

    /** Ported from OpenEXR's IlmBase 1.0.1
    */
    private static int halfToFloat(char ui16) {
        int sign = (ui16 >>> 15) & 0x00000001;
        int exponent = (ui16 >>> 10) & 0x0000001f;
        int mantissa = ui16 & 0x000003ff;

        if (exponent == 0) {
            if (mantissa == 0) {
                // Plus or minus zero

                return sign << 31;
            }
            else {
                // Denormalized number -- renormalize it

                while ((mantissa & 0x00000400) == 0) {
                    mantissa <<= 1;
                    exponent -= 1;
                }

                exponent += 1;
                mantissa &= ~0x00000400;
            }
        }
        else if (exponent == 31) {
            if (mantissa == 0) {
                // Positive or negative infinity

                return (sign << 31) | 0x7f800000;
            }
            else {
                // Nan -- preserve sign and significand bits

                return (sign << 31) | 0x7f800000 | (mantissa << 13);
            }
        }

        // Normalized number

        exponent = exponent + (127 - 15);
        mantissa = mantissa << 13;

        // Assemble S, E and M.

        return (sign << 31) | (exponent << 23) | mantissa;
    }

    /** 
     The smallest positive Half.
    */
    public static final float minValue = 5.96046448e-08f;

    /** 
     The smallest positive normalized Half.
    */
    public static final float minNormalizedValue = 6.10351562e-05f;

    /** 
     The largest positive Half.
    */
    public static final float maxValue = 65504.0f;

    /** 
     Smallest positive value e for which Half(1.0 + e) != Half(1.0).
    */
    public static final float epsilon = 0.00097656f;

    /** 
     Returns a value indicating whether this instance is equal to another
     instance.
     
     @param other The other instance to which to compare this instance.
     @return 
     */
    public boolean equals(Half other) {
    	if (Float.compare(toSingle(), other.toSingle()) == 0) {
    		return true;
    	} else {
    		return false;
    	}
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Half) {
            return equals((Half)obj);
        }
        else {
            return false;
        }
    }

    public int getHashCode() {
        return (new Character(bits)).hashCode();
    }

    public static boolean OpEquality(Half left, Half right) {
        return left.equals(right.clone());
    }

    public static boolean OpInequality(Half left, Half right) {
        return !left.equals(right.clone());
    }

    public static boolean OpLessThan(Half left, Half right) {
        return left.compareTo(right.clone()) < 0;
    }

    public static boolean OpLessThanOrEqual(Half left, Half right) {
        return left.compareTo(right.clone()) <= 0;
    }

    public static boolean OpGreaterThan(Half left, Half right) {
        return left.compareTo(right.clone()) > 0;
    }

    public static boolean OpGreaterThanOrEqual(Half left, Half right) {
        return left.compareTo(right.clone()) >= 0;
    }

    /** 
     Compares this instance to a specified half-precision floating-point number
     and returns an integer that indicates whether the value of this instance
     is less than, equal to, or greater than the value of the specified half-precision
     floating-point number. 
     
     @param other A half-precision floating-point number to compare.
     @return 
     A signed number indicating the relative values of this instance and value. If the number is:
     <p>
     Less than zero, then this instance is less than other, or this instance is not a number
     (NaN) and other is a number.
     </p>
     <p>
     Zero: this instance is equal to value, or both this instance and other
     are not a number (NaN), PositiveInfinity, or
     NegativeInfinity.
     </p>
     <p>
     Greater than zero: this instance is greater than others, or this instance is a number
     and other is not a number (NaN).
     </p>
     
    */
    public int compareTo(Half other) {
        return Float.compare(toSingle(), other.toSingle());
    }

    /** Converts this instance into a human-legible string representation.
     @return The string representation of this instance.
    */
    @Override
    public String toString() {
        return (new Float(toSingle())).toString();
    }
    
    /** Converts the string representation of a number to a half-precision floating-point equivalent.
     @param s String representation of the number to convert.
     @return A new Half instance.
    */
    public static Half parse(String s) {
        return new Half(Double.parseDouble(s));
    }

    /** Converts the string representation of a number to a half-precision floating-point equivalent.
     @param s String representation of the number to convert.
     @param result The Half instance to write to.
     @return true if parsing succeeded; otherwise false.
    */
    public static boolean tryParse(String s, RefObject<Half> result) {
    	Half f = null;
    	try {
    		f = parse(s);
    	} catch (NumberFormatException nfe) {
    		return false;
    	}
    	
        result.argValue = f;
        return true;
    }

    private char bits;

    public Half clone() {
        Half varCopy = new Half();

        varCopy.bits = this.bits;

        return varCopy;
    }    
}