/**
 * 
 */
package edu.asu.jmars.viz3d.renderer.textures;

import java.awt.geom.Point2D;

import edu.asu.jmars.Main;

public class BoundingCorners {
	double minX = 0.0;
	double minY = 0.0;
	double maxX = 0.0;
	double maxY = 0.0;
	
	double newMin = 0.0;
	double newMax = 0.0;
	
//	double padPercent = 0.05;
	double padPercent = 0.1;	// TODO this needs to be calculated in the mesh dynamically for each mesh
	
	Point2D firstMin = null;
	Point2D firstMax = null;
	Point2D secondMin = null;
	Point2D secondMax = null;
		
	boolean meridian = false;
	boolean wrap = false;
	
	int pixels = 0;
	int ppd = 8;
	
	public static float meridianTol = 0.002f;
	
	public BoundingCorners(Point2D min, Point2D max) {
		minX = min.getX();
		minY = min.getY();
		maxX = max.getX();
		maxY = max.getY();
		firstMin = min;
		firstMax = max;
		
		prepBoundingBoxes();
	}
	
	public boolean crossesMeridian() {
		return meridian;
	}
	
	public int getPixelsToTrim() {
		return pixels;
	}
	
	public void pad(double xPad, double yPad) {
		double xAdder = (maxX - minX) * xPad; //0.10;
		double yAdder = (maxY - minY) * yPad; //0.10;
		
		minX -= xAdder;
		minY -= yAdder;
		maxX += xAdder;
		maxY += yAdder;
        
        firstMin = new Point2D.Double(firstMin.getX() - xAdder, firstMin.getY() - yAdder);
        if (secondMin == null && secondMax == null) {
        	firstMax = new Point2D.Double(firstMax.getX() + xAdder, firstMax.getY() + yAdder);
        } else if (secondMin != null && secondMax != null) {
        	firstMax = new Point2D.Double(firstMax.getX(), firstMax.getY() + yAdder);
            secondMin = new Point2D.Double(secondMin.getX(), secondMin.getY() - yAdder);
        	secondMax = new Point2D.Double(secondMax.getX() + xAdder, secondMax.getY() + yAdder);
        }
	}

	public boolean checkTriangleFirst(int[] t, double[] worlds, float[] tex) {
		boolean ret = false;
		
		boolean[] ptInBounds = {false, false, false};
		if (checkPointFirst(worlds[t[0] * 2], worlds[(t[0] * 2) + 1])) {
			ptInBounds[0] = true;
		}
		
		if (checkPointFirst(worlds[t[1] * 2], worlds[(t[1] * 2) + 1])) {
			ptInBounds[1] = true;
		}
		
		if (checkPointFirst(worlds[t[2] * 2], worlds[(t[2] * 2) + 1])) {
			ptInBounds[2] = true;
		}

		// check each point. if any fall within the bounding box include the entire triangle
		if (ptInBounds[0] && ptInBounds[1] && ptInBounds[2]) {
			boolean p1 = getTexCoordsFirst(worlds[t[0] * 2], worlds[(t[0] * 2) + 1], t[0], tex);
			boolean p2 = getTexCoordsFirst(worlds[t[1] * 2], worlds[(t[1] * 2) + 1], t[1], tex);
			boolean p3 = getTexCoordsFirst(worlds[t[2] * 2], worlds[(t[2] * 2) + 1], t[2], tex);
			
			if (p1 && p2 && p3) {
				ret = true;
			}
		} else if ((ptInBounds[0] || ptInBounds[1] || ptInBounds[2]) 
				&& (checkPointFirst(worlds[t[0] * 2], worlds[(t[0] * 2) + 1]) || checkPointSecond(worlds[t[0] * 2], worlds[(t[0] * 2) + 1]))
				&& (checkPointFirst(worlds[t[1] * 2], worlds[(t[1] * 2) + 1]) || checkPointSecond(worlds[t[1] * 2], worlds[(t[1] * 2) + 1]))
				&& (checkPointFirst(worlds[t[2] * 2], worlds[(t[2] * 2) + 1]) || checkPointSecond(worlds[t[2] * 2], worlds[(t[2] * 2) + 1]))) {
			boolean p1 = getTexCoordsThird(worlds[t[0] * 2], worlds[(t[0] * 2) + 1], t[0], tex);
			boolean p2 = getTexCoordsThird(worlds[t[1] * 2], worlds[(t[1] * 2) + 1], t[1], tex);
			boolean p3 = getTexCoordsThird(worlds[t[2] * 2], worlds[(t[2] * 2) + 1], t[2], tex);

			if (p1 && p2 && p3) {
				ret = true;
			}
		}
		return ret;
	}
		
	public boolean checkTriangleSecond(int[] t, double[] worlds, float[] tex) {
		boolean ret = false;
		
		if (!meridian) {
			return ret;
		}
		boolean[] ptInBounds = {false, false, false};
		if (checkPointSecond(worlds[t[0] * 2], worlds[(t[0] * 2) + 1])) {
			ptInBounds[0] = true;
		}
		
		if (checkPointSecond(worlds[t[1] * 2], worlds[(t[1] * 2) + 1])) {
			ptInBounds[1] = true;
		}
		
		if (checkPointSecond(worlds[t[2] * 2], worlds[(t[2] * 2) + 1])) {
			ptInBounds[2] = true;
		}
		// check each point. if any fall within the bounding box include the entire triangle
		if (ptInBounds[0] && ptInBounds[1] && ptInBounds[2]) {
			boolean p1 = getTexCoordsSecond(worlds[t[0] * 2], worlds[(t[0] * 2) + 1], t[0], tex);
			boolean p2 = getTexCoordsSecond(worlds[t[1] * 2], worlds[(t[1] * 2) + 1], t[1], tex);
			boolean p3 = getTexCoordsSecond(worlds[t[2] * 2], worlds[(t[2] * 2) + 1], t[2], tex);
			ret = true;
		} else if ((ptInBounds[0] || ptInBounds[1] || ptInBounds[2]) 
				&& (checkPointFirst(worlds[t[0] * 2]-360.0, worlds[(t[0] * 2) + 1]) || checkPointSecond(worlds[t[0] * 2], worlds[(t[0] * 2) + 1]))
				&& (checkPointFirst(worlds[t[1] * 2]-360.0, worlds[(t[1] * 2) + 1]) || checkPointSecond(worlds[t[1] * 2], worlds[(t[1] * 2) + 1]))
				&& (checkPointFirst(worlds[t[2] * 2]-360.0, worlds[(t[2] * 2) + 1]) || checkPointSecond(worlds[t[2] * 2], worlds[(t[2] * 2) + 1]))) {
			boolean p1 = getTexCoordsSecond(worlds[t[0] * 2], worlds[(t[0] * 2) + 1], t[0], tex);
			boolean p2 = getTexCoordsSecond(worlds[t[1] * 2], worlds[(t[1] * 2) + 1], t[1], tex);
			boolean p3 = getTexCoordsSecond(worlds[t[2] * 2], worlds[(t[2] * 2) + 1], t[2], tex);

			if (p1 && p2 && p3) {
				ret = true;
			}
		}
		return ret;
	}
	
	public boolean checkPoint(double x, double y) {
		if ((x >= firstMin.getX() && x <= firstMax.getX()
				&& y >= firstMin.getY() && y <= firstMax.getY())
				|| (meridian && x >= secondMin.getX() && x <= secondMax.getX()
						&& y >= secondMin.getY() && y <= secondMax.getY())) {
			return true;
		} else {
			return false;
		}			
	}
	
	public boolean checkPointFirst(double x, double y) {
		if ((x >= firstMin.getX() && x <= firstMax.getX()
				&& y >= firstMin.getY() && y <= firstMax.getY())) {
			return true;
		} else {
			return false;
		}			
	}
		
	public boolean checkPointSecond(double x, double y) {
		if (meridian && (x >= secondMin.getX() && x <= secondMax.getX()
				&& y >= secondMin.getY() && y <= secondMax.getY())) {
			return true;
		} else {
			return false;
		}			
	}
		
	public boolean getTexCoordsFirst(double x, double y, int idx, float[] tex) {
			// normalize and return tex coords 
			if (crossesMeridian()) {
				double tempX = x - 360.0;
				tex[idx * 2] = (float)((tempX - minX) / (maxX - minX));
				tex[(idx * 2) + 1] = (float)((y - minY) / (maxY - minY));
			} else {
				tex[idx * 2] = (float)((x - firstMin.getX()) / (firstMax.getX() - firstMin.getX()));
				tex[(idx * 2) + 1] = (float)((y - firstMin.getY()) / (firstMax.getY() - firstMin.getY()));
			}
		return true;
	}
	
	public boolean getTexCoordsSecond(double x, double y, int idx, float[] tex) {
			tex[idx * 2] = (float)((x - minX) / (maxX - minX));
			tex[(idx * 2) + 1] = (float)((y - minY) / (maxY - minY));
		return true;
	}
	
	public boolean getTexCoordsThird(double x, double y, int idx, float[] tex) {
			// normalize and return tex coords 
			if (x > 350.0) {
				double tempX = x - 360.0;
				tex[idx * 2] = (float)((tempX - minX) / (maxX - minX));
				tex[(idx * 2) + 1] = (float)((y - minY) / (maxY - minY));
			} else {
				tex[idx * 2] = (float)((x - minX) / (maxX - minX));
				tex[(idx * 2) + 1] = (float)((y - minY) / (maxY - minY));
			}
		return true;
	}

	public float[] getTexCoords(double x, double y) {
		float[] t = null;
		if (x >= firstMin.getX() && x <= firstMax.getX()
				&& y >= firstMin.getY() && y <= firstMax.getY()) {
			// normalize and return tex coords 
			t = new float[2];
			if (crossesMeridian()) {
				double tempX = x - 360.0;
				t[0] = (float)((tempX - minX) / (maxX - minX));
				t[1] = (float)((y - minY) / (maxY - minY));
			} else {
				t[0] = (float)((x - firstMin.getX()) / (firstMax.getX() - firstMin.getX()));
				t[1] = (float)((y - firstMin.getY()) / (firstMax.getY() - firstMin.getY()));
			}
		} else {
			t = new float[2];
			t[0] = (float)((x - minX) / (maxX - minX));
			t[1] = (float)((y - minY) / (maxY - minY));
		}
		return t;
	}
	
	private void prepBoundingBoxes() {
		if (minX < 0.0 && maxX > 0.0 && maxX-minX <= 360.0) {
			// need to split the bounding box along the prime meridian
			firstMin = new Point2D.Double(minX + 360.0, minY);
			firstMax = new Point2D.Double(360.0, maxY);
			secondMin = new Point2D.Double(0.0, minY);
			secondMax = new Point2D.Double(maxX, maxY);
			meridian = true;
		} else if (maxX-minX > 360.0) {
			pixels = find360BoundingBox();
			shiftTo360();
			if (newMin < 0.0 && newMax >= 0.0) {
				firstMin = new Point2D.Double(newMin + 360.0, minY);
				firstMax = new Point2D.Double(360.0, maxY);
				secondMin = new Point2D.Double(0.0, minY);
				secondMax = new Point2D.Double(newMax, maxY);
				meridian = true;
			} else {
				firstMin = new Point2D.Double(newMin, minY);
				firstMax = new Point2D.Double(newMax, maxY);
			}
		} else if (maxX - minX <= 360.0) {
			newMax = maxX;
			newMin = minX;
			shiftTo360();
			if (newMin < 0.0 && newMax >= 0.0) {
				firstMin = new Point2D.Double(newMin + 360.0, minY);
				firstMax = new Point2D.Double(360.0, maxY);
				secondMin = new Point2D.Double(0.0, minY);
				secondMax = new Point2D.Double(newMax, maxY);
				meridian = true;
			} else {
				firstMin = new Point2D.Double(newMin, minY);
				firstMax = new Point2D.Double(newMax, maxY);
			}
		}		
	}
	
	private int find360BoundingBox() {
		int pixelsToTrim = 0;
		// calculate new bounding box
		newMin = minX;
		newMax = minX + 360.0;
		
//		ppd = Main.testDriver.mainWindow.getProj().getPPD();
		pixelsToTrim = (int)(maxX-newMax) * ppd;

		return pixelsToTrim;
	}
	
	private void shiftTo360() {
		// shift up to range of -360 to 360
		while (newMin < -360.0) {
			newMin += 360.0;
			newMax += 360.0;
		}
		// shift down to range of -360 to 360
		while (newMax > 360.0) {
			newMin -= 360.0;
			newMax -= 360.0;
		}
		minX = newMin;
		maxX = newMax;
	}

	public boolean isWrap() {
		return wrap;
	}

	public void setWrap(boolean wrap) {
		this.wrap = wrap;
	}
}