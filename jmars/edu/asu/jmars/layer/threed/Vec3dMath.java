package edu.asu.jmars.layer.threed;

/**
 * ©2010, David J. Eck.
 * This work is licensed under a Creative Commons Attribution-ShareAlike 3.0 License. 
 * (This license allows you to redistribute this book in unmodified form. 
 * It allows you to make and distribute modified versions, as long as you include an 
 * attribution to the original author, clearly describe the modifications that you 
 * have made, and distribute the modified work under the same license as the original. 
 * See the license for full details.)

 * The most recent version of this work is available, at no charge, for downloading 
 * and for on-line use at the Web address: http://math.hws.edu/graphicsnotes/
 */

/**
 * This version of David Eck's code requires all vectors to be defined as type float.\
 * The original source can be found at http://math.hws.edu/graphicsnotes/source/glutil/Vec3fMath.java
 * One additinal method has been added:    public static float[] normalFromPoints(float[] p1, int offset1,
	      float[] p2, int offset2, float[] p3, int offset3)
 *	which is a modification of the unitFromPoints() method.       
 */

/**
 * A class that provides some vector operations as static methods,
 * treating vectors as arrays of type float[], or as 3 consecutive
 * elements in such an array.
 */
public class Vec3dMath {
   
   //---- Methods where vectors are represented as the first three items in a float[] array ------
   
   /**
    * Compute the length of the vector (v[0],v[1],v[2]).
    */
   public static float length(float[] v) {
      return length(v,0);
   }
   
   /**
    * Modify the vector (v[0],v[1],v[2]) to a vector pointing in
    * the same direction, but with length 1.  No change is made if
    * the current length is not greater than zero.
    */
   public static void normalize(float[] v) {
      normalize(v,0);
   }
   
   /**
    * Return a unit (length 1) vector that points in the same
    * direction as the vector (v[0],v[1],v[2]).  If the length
    * of this vector is not greater than 0, then a copy of the
    * original vector is returned.
    */
   public static float[] normalized(float[] v) {
      return normalized(v,0);
   }
   
   /**
    * Return a unit vector pointing in the same direction as the cross product
    * of (p3-p2) and (p1-p2), except that if the length of the cross product is
    * not greater than 0, then null is returned.  If p1, p2, p3 are consecutive
    * vertices of a polygon, then the return value (if non-null) is a vector
    * perpendicular to the polygon, pointing out of the front face of the polygon
    * if p1, p2, p3 are in counterclockwise order as seen from the front.  (The
    * points are given by the first three elements of the arrays p1, p2, p3.)
    */
   public static float[] unitNormalFromPoints(float[] p1, float[] p2, float[] p3) {
      return unitNormalFromPoints(p1, 0, p2, 0, p3, 0);
   }
   
   /**
    * Return the dot product of (v1[0],v1[1],v2[1]) and (v2[0],v2[1],v3[1])
    */
   public static float dot(float[] v1, float[] v2) {
      return dot(v1,0,v2,0);
   }
      
   /**
    * Return the cross product of (v1[0],v1[1],v2[1]) and (v2[0],v2[1],v3[1])
    */
   public static float[] cross(float[] v1, float[] v2) {
      float[] crossProduct = new float[3];
      cross(v1,0,v2,0,crossProduct,0);
      return crossProduct;
   }
   
   /**
    * Add the vector (vec[0],vec[1],vec[2]) to (accumulator[0],accumulator[1],accumulator[2]),
    * storing the answer back into accumulator.
    */
   public static void addInto(float[] vec, float[] accumulator) {
      addInto(vec,0,accumulator,0);
   }   

   /**
    * Multiply the vector (accumulator[0],accumulator[1],accumulator[2]) by factor,
    * storing the result back into accumulator.
    */
   public static void multiplyInto(float factor, float[] accumulator) {
      multiplyInto(factor, accumulator, 0);
   }

   
   //------- Methods where vectors are represented as float[] array plus offset into that array ------
   //------- These are similar to the preceding methods, except that the vectors and points ---------
   //------- are given in the form (A[offset],A[offset+1],A[offset+2]) instead of as the ------------
   //------- first three elements of the array. -----------------------------------------------------
   
   /**
    * Returns the length of the vector (v[offset],v[offset+1],v[offset+2]).
    */
   public static float length(float[] v, int offset) {
      return (float) Math.sqrt(v[offset]*v[offset] + v[offset+1]*v[offset+1] + v[offset+2]*v[offset+2]);
   }

   /**
    * Modifies the vector (v[offset],v[offset+1],v[offset+2]), replacing it with a
    * unit vector that points in the same direction.  If the length of the vector
    * is not positive, then the original vector is not modified.
    */
   public static void normalize(float[] v, int offset) {
      float length = length(v,offset);
      if (length > 0) {
         v[offset] /= length;
         v[offset+1] /= length;
         v[offset+2] /= length;
      }
   }
   
   /**
    * Returns a unit vector that points in the same direction as the vector
    * (v[offset],v[offset+1],v[offset+2]), except that if the length of the
    * original vector is not positive, then a copy of the original vector is
    * returned.
    */
   public static float[] normalized(float[] v, int offset) {
      float[] copy = { v[offset], v[offset+1], v[offset+2] };
      normalize(copy,0);
      return copy;
    }
   
   /**
    * Return a unit vector pointing in the same direction as the cross product
    * of (p3-p2) and (p1-p2), except that if the length of the cross product is
    * not greater than 0, then null is returned.  If p1, p2, p3 are consecutive
    * vertices of a polygon, then the return value (if non-null) is a vector
    * perpendicular to the polygon, pointing out of the front face of the polygon
    * if p1, p2, p3 are in counterclockwise order as seen from the front.  (The
    * points are given by three elements of the arrays p1, p2, p3, starting at
    * the respective offsets.)
    */
   public static float[] unitNormalFromPoints(float[] p1, int offset1,
         float[] p2, int offset2, float[] p3, int offset3) {
      float[] v1 = { p3[offset3]-p2[offset2], p3[offset3+1]-p2[offset2+1], p3[offset3+2]-p2[offset2+2] };
      float[] v2 = { p1[offset1]-p2[offset2], p1[offset1+1]-p2[offset2+1], p1[offset1+2]-p2[offset2+2] };
      float[] v3= new float[3];
      cross(v1,0,v2,0,v3,0);
      float length = length(v3,0);
      if (length > 0) {
         v3[0] /= length;
         v3[1] /= length;
         v3[2] /= length;
         return v3;
      }
      return null;
   }

   /**
    * Return a normal vector pointing in the same direction as the cross product
    * of (p3-p2) and (p1-p2), except that if the length of the cross product is
    * not greater than 0, then null is returned.  If p1, p2, p3 are consecutive
    * vertices of a polygon, then the return value (if non-null) is a vector
    * perpendicular to the polygon, pointing out of the front face of the polygon
    * if p1, p2, p3 are in counterclockwise order as seen from the front.  (The
    * points are given by three elements of the arrays p1, p2, p3, starting at
    * the respective offsets.)
    */
   public static float[] normalFromPoints(float[] p1, int offset1,
	      float[] p2, int offset2, float[] p3, int offset3) {
	      float[] v1 = { p3[offset3]-p2[offset2], p3[offset3+1]-p2[offset2+1], p3[offset3+2]-p2[offset2+2] };
	      float[] v2 = { p1[offset1]-p2[offset2], p1[offset1+1]-p2[offset2+1], p1[offset1+2]-p2[offset2+2] };
	      float[] v3= new float[3];
	      cross(v1,0,v2,0,v3,0);
          return v3;
	   }

   
   /**
    * Return the dot product of the vectors (v1[offset1],v1[offset1+1],v1[offset1+2]) and
    * (v2[offset2],v2[offset2+1],v2[offset2+2])
    */
   public static float dot(float[] v1, int offset1, float[] v2, int offset2) {
      return v1[offset1]*v2[offset2] + v1[offset1+1]*v2[offset2+1] + v1[offset1+2]*v2[offset2+2];
   }
   
   /**
    * Computes the cross product of the vectors (v1[offset1],v1[offset1+1],v1[offset1+2]) and
    * (v2[offset2],v2[offset2+1],v2[offset2+2]), storing the result into the crossProduct
    * array starting at index offsetCross.
    */
   public static void cross(float[] v1, int offset1, float[] v2, int offset2,
                                           float[] crossProduct, int offsetCross) {
      crossProduct[offsetCross] = v1[offset1+1]*v2[offset2+2] - v2[offset2+1]*v1[offset1+2];
      crossProduct[offsetCross+1] = v1[offset1+2]*v2[offset2] - v2[offset2+2]*v1[offset1];
      crossProduct[offsetCross+2] = v1[offset1]*v2[offset2+1] - v2[offset2]*v1[offset1+1];
   }
   
   /**
    * Adds the vector (vec[offsetVec],vec[offsetVec+1],vec[offsetVec+1]) into the vector
    * (accumulator[offsetAccumulator],accumulator[offsetAccumulator+1],accumulator[offsetAccumulator+2]),
    * storing the result back into accumulator.
    */
   public static void addInto(float[] vec, int offsetVec, float[] accumulator, int offsetAccumulator) {
      accumulator[offsetAccumulator] += vec[offsetVec];
      accumulator[offsetAccumulator+1] += vec[offsetVec+1];
      accumulator[offsetAccumulator+2] += vec[offsetVec+2];
   }
   
   /**
    * Multiplies the vector
    * (accumulator[offsetAccumulator],accumulator[offsetAccumulator+1],accumulator[offsetAccumulator+2])
    * by factor, storing the result back into accumulator.
    */
   public static void multiplyInto(float factor, float[] accumulator, int offsetAccumulator) {
      accumulator[offsetAccumulator] *= factor;
      accumulator[offsetAccumulator+1] *= factor;
      accumulator[offsetAccumulator+2] *= factor;
   }

   /**
    * Subtracts the subtrahend from the minuend on an element by element basis
    * If the minuend and subtrahend are not of the same length, null is returned
    * @param minuend
    * @param subtrahend
    * @return difference
    */
   public static float[] subtract(float[] minuend, float[] subtrahend) {
	   if (minuend.length != subtrahend.length) {
		   return null;
	   }
	   float[] diff = new float[minuend.length];
	   for (int i=0; i<minuend.length; i++) {
		   diff[i] = minuend[i] - subtrahend[i];
	   }
	   return diff;
   }

   /**
    * Adds the addend to the augend on an element by element basis
    * If the augend and addend are not of the same length, null is returned
    * @param augend
    * @param addend
    * @return sum
    */
   public static float[] add(float[] augend, float[] addend) {
	   if (augend.length != addend.length) {
		   return null;
	   }
	   float[] sum = new float[augend.length];
	   for (int i=0; i<augend.length; i++) {
		   sum[i] = augend[i] + addend[i];
	   }
	   return sum;
   }
   
   /**
    * Multiplies the multiplicand by the multiplier on an element by element basis
    * @param multiplier
    * @param multiplicand
    * @return product
    */
   public static float[] multiply(float multiplier, float[] multiplicand) {
	   float[] product = new float[multiplicand.length];
	   for (int i=0; i<multiplicand.length; i++) {
		   product[i] = multiplier * multiplicand[i];
	   }
	   return product;
   }

}
