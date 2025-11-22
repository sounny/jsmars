package edu.asu.jmars.viz3d.core.geometry;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.math.geom.Frustum;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.viz3d.core.math.Util3D;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;
import edu.asu.jmars.viz3d.spatial.IntersectResultData;
import edu.asu.jmars.viz3d.spatial.Node;
import edu.asu.jmars.viz3d.spatial.RayWithEpsilon;
import edu.asu.jmars.viz3d.spatial.SpatialRenderable;

/**
 * Implementation of a 3D cuboid
 *
 */
public class Cuboid implements GLRenderable, SpatialRenderable {
	
	float[][] points;
	float[][] frustum;
	float[] color;

	float[] center;
	float[] size;
	
	boolean asNormalVectorPoint;
	
	private boolean isScalable = true;
	private boolean hasBeenScaled = false;
	private float alpha = 1.0f;
    private Float displayAlpha;
    private boolean renderAsPlane = false;

	private static DebugLog log = DebugLog.instance();
	
	
	/**
	 * @param eightCornerpoints points that define the cuboid. First four points (xyz) represent
	 * 	the "near" side of the cuboid. Last four points represent the "far"side. Both should in in CCW order
	 * 	from the perspective of the near side.
	 * @param color
	 * @param renderAsPlaneEquation if true renders from the plane equations else renders as 
	 * 	a line drawing based on the corner points
	 */
	public Cuboid (float[][] eightCornerPoints, float[] color, boolean renderAsPlaneEquation) {
		points = eightCornerPoints;
		this.color = color;
		renderAsPlane = renderAsPlaneEquation;
		frustum = Util3D.getFrustumFromCube(points);
		calculateCenter();
		calculateSize();
	}

	@Override
	public void execute(GL2 gl) {
		if (!renderAsPlane) {
			drawCube(gl, points);
		} else {
			drawFrustum(gl, frustum);
		}		
	}

	@Override
	public void preRender(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postRender(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public float getAlpha() {
		return alpha;
	}

	@Override
	public float getDisplayAlpha() {
		if(displayAlpha == null){
			return alpha;
		}
		return displayAlpha;
	}

	@Override
	public void setDisplayAlpha(float alpha) {
		displayAlpha = alpha;
	}

	@Override
	public boolean isScalable() {
		if (points != null && points.length == 2) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void scaleByDivision(float scalar) {
		if (Float.compare(scalar, 0f) == 0) {
			log.aprintln("Attempting to scale a GLRenderable by dividing by zero.");
			return;
		}
		if (hasBeenScaled) {
			//NOP
			return;
		}
		float scaleFactor = 1f / scalar;
		if (isScalable() && points != null) {
			for (int i=0; i<points.length; i++) {
				for (int j=0; j<points[i].length; j++) {
						points[i][j] *= scaleFactor;
				}
			}
			calculateCenter();
			calculateSize();
			hasBeenScaled = true;
		}
	}

	@Override
	public void scaleToShapeModel(boolean canScale) {
		isScalable = canScale;		
	}

	@Override
	public boolean isScaled() {
		return hasBeenScaled;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public float[][] getPoints() {
		if (points != null) {
			return points;
		} else {
			return null;
		}
	}

	@Override
	public float[] getCenter() {
		return center;
	}

	@Override
	public float getRadius() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Node<SpatialRenderable> getNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setNode(Node<?> t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void render(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IntersectResultData<SpatialRenderable> intersectRay(RayWithEpsilon ray) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean intersectFrustum(float[] normal, float[][] frustum) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean intersectJoglFrustum(float[] normal, Frustum frustum) {
		// TODO Auto-generated method stub
		return false;
	}
	
	private void calculateSize() {
		if (center == null) {
			calculateCenter();
		}
		if (size == null) {
			size = new float[3];
		}
		size[0] = center[0] - points[0][0];
		size[1] = center[1] - points[1][1];
		size[2] = center[2] - points[1][2];
	}
	
	private void calculateCenter() {
		if (center == null) {
			center = new float[3];
		}
		if (points != null) {
			
			center[0] = VectorUtil.mid(points[0][0], points[1][0]);
			center[1] = VectorUtil.mid(points[1][1], points[5][1]);
			center[2] = VectorUtil.mid(points[1][2], points[2][2]);
		}
	}
	
	private void drawFrustum(GL2 gl, float[][] frustum) {
		{
			int right=0, left=1, bottom=2, top=3, far=4, near=5;
			HVector nearTopLeft = Util3D.intersect3(Util3D.floatToDoubleArray(frustum[near]),Util3D.floatToDoubleArray(frustum[top]),Util3D.floatToDoubleArray(frustum[left]));
			HVector nearTopRight = Util3D.intersect3(Util3D.floatToDoubleArray(frustum[near]),Util3D.floatToDoubleArray(frustum[top]),Util3D.floatToDoubleArray(frustum[right]));
			HVector nearBotLeft = Util3D.intersect3(Util3D.floatToDoubleArray(frustum[near]),Util3D.floatToDoubleArray(frustum[bottom]),Util3D.floatToDoubleArray(frustum[left]));
			HVector nearBotRight = Util3D.intersect3(Util3D.floatToDoubleArray(frustum[near]),Util3D.floatToDoubleArray(frustum[bottom]),Util3D.floatToDoubleArray(frustum[right]));
			HVector farTopLeft = Util3D.intersect3(Util3D.floatToDoubleArray(frustum[far]),Util3D.floatToDoubleArray(frustum[top]),Util3D.floatToDoubleArray(frustum[left]));
			HVector farTopRight = Util3D.intersect3(Util3D.floatToDoubleArray(frustum[far]),Util3D.floatToDoubleArray(frustum[top]),Util3D.floatToDoubleArray(frustum[right]));
			HVector farBotLeft = Util3D.intersect3(Util3D.floatToDoubleArray(frustum[far]),Util3D.floatToDoubleArray(frustum[bottom]),Util3D.floatToDoubleArray(frustum[left]));
			HVector farBotRight = Util3D.intersect3(Util3D.floatToDoubleArray(frustum[far]),Util3D.floatToDoubleArray(frustum[bottom]),Util3D.floatToDoubleArray(frustum[right]));

			gl.glColor3f(color[0], color[1], color[2]);
			gl.glLineWidth(3);
			gl.glBegin(GL2.GL_LINES);

			// near
			gl.glVertex3d(nearTopLeft.x, nearTopLeft.y, nearTopLeft.z);
			gl.glVertex3d(nearTopRight.x, nearTopRight.y, nearTopRight.z);

			gl.glVertex3d(nearTopRight.x, nearTopRight.y, nearTopRight.z);
			gl.glVertex3d(nearBotRight.x, nearBotRight.y, nearBotRight.z);

			gl.glVertex3d(nearBotRight.x, nearBotRight.y, nearBotRight.z);
			gl.glVertex3d(nearBotLeft.x, nearBotLeft.y, nearBotLeft.z);

			gl.glVertex3d(nearBotLeft.x, nearBotLeft.y, nearBotLeft.z);
			gl.glVertex3d(nearTopLeft.x, nearTopLeft.y, nearTopLeft.z);

			// far
			gl.glVertex3d(farTopLeft.x, farTopLeft.y, farTopLeft.z);
			gl.glVertex3d(farTopRight.x, farTopRight.y, farTopRight.z);

			gl.glVertex3d(farTopRight.x, farTopRight.y, farTopRight.z);
			gl.glVertex3d(farBotRight.x, farBotRight.y, farBotRight.z);

			gl.glVertex3d(farBotRight.x, farBotRight.y, farBotRight.z);
			gl.glVertex3d(farBotLeft.x, farBotLeft.y, farBotLeft.z);

			gl.glVertex3d(farBotLeft.x, farBotLeft.y, farBotLeft.z);
			gl.glVertex3d(farTopLeft.x, farTopLeft.y, farTopLeft.z);

			// sides
			gl.glVertex3d(nearTopLeft.x, nearTopLeft.y, nearTopLeft.z);
			gl.glVertex3d(farTopLeft.x, farTopLeft.y, farTopLeft.z);

			gl.glVertex3d(nearTopRight.x, nearTopRight.y, nearTopRight.z);
			gl.glVertex3d(farTopRight.x, farTopRight.y, farTopRight.z);

			gl.glVertex3d(nearBotLeft.x, nearBotLeft.y, nearBotLeft.z);
			gl.glVertex3d(farBotLeft.x, farBotLeft.y, farBotLeft.z);

			gl.glVertex3d(nearBotRight.x, nearBotRight.y, nearBotRight.z);
			gl.glVertex3d(farBotRight.x, farBotRight.y, farBotRight.z);

			gl.glEnd();
		}


		gl.glColor3f(1f, 1f, 1f);
	}
	
	private void drawCube(GL2 gl, float[][] cube) {
		{
			gl.glColor3f(color[0], color[1], color[2]);
			gl.glLineWidth(3);
			gl.glBegin(GL2.GL_LINES);
	
			// 0 to 1
			gl.glVertex3d(cube[0][0], cube[0][1], cube[0][2]);
			gl.glVertex3d(cube[1][0], cube[1][1], cube[1][2]);
	
			// 1 to 2
			gl.glVertex3d(cube[1][0], cube[1][1], cube[1][2]);
			gl.glVertex3d(cube[2][0], cube[2][1], cube[2][2]);
	
			// 2 to 3
			gl.glVertex3d(cube[2][0], cube[2][1], cube[2][2]);
			gl.glVertex3d(cube[3][0], cube[3][1], cube[3][2]);
			
			// 3 to 0
			gl.glVertex3d(cube[3][0], cube[3][1], cube[3][2]);
			gl.glVertex3d(cube[0][0], cube[0][1], cube[0][2]);
	
			// 4 to 5
			gl.glVertex3d(cube[4][0], cube[4][1], cube[4][2]);
			gl.glVertex3d(cube[5][0], cube[5][1], cube[5][2]);
			
			// 5 to 6
			gl.glVertex3d(cube[5][0], cube[5][1], cube[5][2]);
			gl.glVertex3d(cube[6][0], cube[6][1], cube[6][2]);
					
			// 6 to 7
			gl.glVertex3d(cube[6][0], cube[6][1], cube[6][2]);
			gl.glVertex3d(cube[7][0], cube[7][1], cube[7][2]);
			
			// 7 to 4
			gl.glVertex3d(cube[7][0], cube[7][1], cube[7][2]);
			gl.glVertex3d(cube[4][0], cube[4][1], cube[4][2]);
	
			// 0 to 4
			gl.glVertex3d(cube[0][0], cube[0][1], cube[0][2]);
			gl.glVertex3d(cube[4][0], cube[4][1], cube[4][2]);
	
			// 1 to 5
			gl.glVertex3d(cube[1][0], cube[1][1], cube[1][2]);
			gl.glVertex3d(cube[5][0], cube[5][1], cube[5][2]);
	
			// 2 to 6
			gl.glVertex3d(cube[2][0], cube[2][1], cube[2][2]);
			gl.glVertex3d(cube[6][0], cube[6][1], cube[6][2]);
	
			// 3 to 7
			gl.glVertex3d(cube[3][0], cube[3][1], cube[3][2]);
			gl.glVertex3d(cube[7][0], cube[7][1], cube[7][2]);
	
			gl.glEnd();
		}

		gl.glColor3f(1f, 1f, 1f);

	}
	
	public void rotate(Quaternion quat) {
		float[][] rotPoints = new float[points.length][];
		int idx = 0;
		for (float[] pt : points) {
			rotPoints[idx++] = quat.rotateVector(new float[3], 0, pt, 0);
		}
		points = rotPoints;
		if (frustum != null) {
			frustum = Util3D.getFrustumFromCube(points);
		}
	}
	
	public float[][] getFrustum() {
		return frustum;
	}

	public boolean isAsNormalVectorPoint() {
		return asNormalVectorPoint;
	}

	public void setAsNormalVectorPoint(boolean asNormalVectorPoint) {
		this.asNormalVectorPoint = asNormalVectorPoint;
	}

	public void setPoints(float[][] points) {
		this.points = points;
	}
		
	public float[] getSize() {
		return size;
	}

	@Override
	public void setColor(float[] color) {
		if (color != null && color.length == 3) {
			this.color = color;
		}		
	}

	@Override
	public float[] getColor() {
		return color;
	}
	
	@Override
	public void finalize() {
		points = null;
		frustum = null;
		color = null;

		center = null;
		size = null;
		displayAlpha = null;
	}


}
