package edu.asu.jmars.layer.stamp;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.InvestigateData;
import edu.asu.jmars.layer.stamp.focus.StampFocusPanel;
import edu.asu.jmars.layer.stamp.focus.StampTable;
import edu.asu.jmars.layer.stamp.spectra.SpectraObject;
import edu.asu.jmars.layer.stamp.focus.MultiExpressionDialog.ColumnExpression;
import edu.asu.jmars.layer.stamp.focus.OutlineFocusPanel;
import edu.asu.jmars.layer.stamp.focus.OutlineFocusPanel.StampAccessor;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.viz3d.core.geometry.Polygon;
import edu.asu.msff.StampInterface;
import gnu.jel.CompiledExpression;

public class StampShape {

	protected StampInterface myStamp;
	StampLayer stampLayer;
		
	public StampShape(StampInterface stamp, StampLayer stampLayer) {
		myStamp=stamp;
		this.stampLayer = stampLayer;
	}
	
	public String getId() {
		return myStamp.getId();
	}
	
	// TODO: Probably should NOT be public.  Need a getter
	public ArrayList<SpectraObject> spectraPoints;

	public void addSpectra(String newName, Point2D newPoint, double[] newSpectra, double[] newAxis, int xpad, int ypad, String imageId) {
		if (spectraPoints==null) {
			spectraPoints = new ArrayList<SpectraObject>();
		}
		
		String type = stampLayer.getSpectraColumns()[0];
		
		SpectraObject newImagePoint = new SpectraObject(newName, newAxis, newSpectra, type, newPoint, xpad, ypad, this);
		
		spectraPoints.add(newImagePoint);
	}

	public String getSpectraName() {
		String nameCol = stampLayer.getParam(stampLayer.SPECTRA_NAME_COLUMN);
		
		if (nameCol!=null && nameCol.length()>0) {
			return getVal(nameCol).toString();
		}
		return myStamp.getId();
	}
	
	public StampInterface getStamp() {
		return myStamp;
	}
		
	public String[] getVisibleColumns() {
		StampTable table = stampLayer.viewToUpdate.getFocusPanel().table;
		
		String columnNames[] = new String[table.getColumnCount()];
		
		for (int i=0; i<columnNames.length; i++) {
			columnNames[i] = table.getColumnName(i);
		}
 
		return columnNames;
	}
	
	
	public String getColumnName(int index) {
		return stampLayer.getColumnName(index);
	}	
	
	public Object getData(int index) {
		//if the layer has spectra or spot data, then it will have an
		// additional three columns: hide, color col, locked
		// But if it's just a regular stamp layer, it won't have
		// those columns, but can still have columnExpressions
		int maxLength = myStamp.getData().length;
		int hideIdx = -Integer.MAX_VALUE;
		int calcColIdx = -Integer.MAX_VALUE;
		int lockedIdx = -Integer.MAX_VALUE;
		int expStartIdx = maxLength;	
		
		if(stampLayer.spectraData() || stampLayer.pointShapes()){
			hideIdx = maxLength;
			calcColIdx = maxLength+1;
			lockedIdx = maxLength+2;
			expStartIdx = maxLength+3;
		}
		
		if (index>=0 && index<maxLength) {
			// TODO: Add safety
			return myStamp.getData()[index];
		} else {
			if (index==hideIdx) {
				return isHidden;
			} else if (index==calcColIdx) {
				return calculatedColor;
			} else if (index==lockedIdx) {
				return isLocked;
			}  
			//new expression
			else if (index>=expStartIdx){
				int expIdx = index - expStartIdx;
				OutlineFocusPanel ofp = stampLayer.viewToUpdate.getFocusPanel().outlinePanel;
				ColumnExpression ce = stampLayer.getColumnExpression(expIdx);
				CompiledExpression compiledExpression = ce.getCompiledExpression();
				if (compiledExpression!=null) {
					StampAccessor sa[] = new StampAccessor[1];
					sa[0]=new StampAccessor(stampLayer, this);
					
					try {
						Object o = compiledExpression.evaluate(sa);
						return Double.parseDouble(o+"");
					}
					catch(Throwable e){
						//TODO: we don't throw exceptions for the current calculated val
//						e.printStackTrace();
					}
				}
			}
			return null;
		}
	}
	
	double calculatedValue=Double.NaN;
	public void setCalculatedValue(double newVal) {
		calculatedValue = newVal;
	}
	
	public double getCalculatedValue() {
		return calculatedValue;
	}
	
	Color calculatedColor=null;
	public void setCalculatedColor(Color newColor) {
		calculatedColor=newColor;
	}
	
	public Color getCalculatedColor() {
		return calculatedColor;
	}
	
	boolean isLocked = false;
	public boolean isLocked() {
		return isLocked;
	}
	
	public void setLocked(boolean newVal) {
		isLocked = newVal;
	}
	
	boolean isHidden = false;
	public boolean isHidden() {
		return isHidden;
	}
	
	public void setHidden(boolean newVal) {
		isHidden = newVal;
	}
	
	// TODO: StampShape is probably misnamed.
	public double[] getXValues(String columnName) {	
		HashMap<String,String> axisMap = stampLayer.getSpectraAxisMap();
		HashMap<String,String> axisPostMap = stampLayer.getSpectraAxisPostMap();
			
		String xAxisName = axisMap.get(columnName.trim());
				
		String postCol = axisPostMap.get(columnName.trim());
		if (postCol!=null && postCol.length()>0) {
			xAxisName += getVal(postCol);
		} 

		double axisVals[]=stampLayer.getAxis(xAxisName.trim());
		
		if (axisVals!=null) {
			return axisVals;
		}
		
		
		Object v = getVal((xAxisName.trim()));

		if (v!=null) {
			if (v instanceof float[]) {
				float f[]=(float[])v;
				
				double d[] = new double[f.length];
				
				for (int i=0; i<f.length; i++) {
					d[i]=f[i];
				}
				
				return d;
				
			} if (v instanceof Double[]) {
				Double D[]=(Double[])v;
				
				double d[] = new double[D.length];
				
				for (int i=0; i<D.length; i++) {
					d[i]=D[i];
				}
				
				return d;
				
			} else {

				return (double[])v;
			}
		} else {
			System.out.println("Unknown value!");
			return new double[0];
		}
	}

	public String toString() {
		return myStamp.getId();
	}
	
	public Object getVal(String columnName) {
		int colNum = stampLayer.getColumnNum(columnName);

		if (colNum>getStamp().getData().length) {
			return "Nonsense";
		}
		
		if (colNum>=0) {
			return getStamp().getData()[colNum];
		}
		
		return null;
	}
	
	// -2 is our uninitialized value, because -1 represents that the column wasn't found
	private int tipIndex = -2;
	
	private void calcTooltipIndex() {
		String tipCol=stampLayer.getParam(stampLayer.TOOLTIP_COLUMN);
		
		//if this is a spectra layer, set the tip index to the color column value
		if(stampLayer.spectraData()){
			tipIndex = stampLayer.getColumnNum(stampLayer.getSettings().colorColumn);
			return;
		}
		
		if (tipCol==null || tipCol.length()==0) {
			tipIndex=-1;
		} else {
			tipIndex = stampLayer.getColumnNum(tipCol);
			if (tipIndex>=0) return;
		}
		
		tipIndex=-1;
	}
	
	public String getTooltipText() {

		String tipUnits=stampLayer.getParam(stampLayer.TOOLTIP_UNITS);
		if (tipIndex==-2) calcTooltipIndex();
				
		if (tipIndex==-1) return "ID: " + getId();
		
		String retStr = getData(tipIndex).toString();
		
		if (tipUnits!=null) {
			retStr+=" "+tipUnits;
		}
		
		return retStr;
	}
	
	
	public InvestigateData getInvestigateData(InvestigateData iData) {
		if (tipIndex==-2) calcTooltipIndex();
		
		if (stampLayer.spectraData()) {
			Object val = getData(tipIndex).toString();
			
			String name;
			StampFocusPanel fp = stampLayer.viewToUpdate.myFocus;
			if(fp.outlinePanel.getExpression() == null){
				int nameIndex = fp.outlinePanel.getColorColumn();
				name = fp.table.getTableModel().getColumnName(nameIndex);
			}else{
				name = "Calculated value";
			}
			
			
			String key = getId()+"-"+name;
			
			String str = (String) val;
			if (str!=null) {
				String vals[] = str.split(",");
				
				for (String v : vals) {
					//TODO: figure out how to calculate units?
					iData.add(key, v, "", "ItalicSmallBlue","SmallBlue", true);
				}
			} else {
				iData.add(key, val.toString(), "TBD", false);
			}

			return iData;
		}		
		else{
			String tipUnits=stampLayer.getParam(stampLayer.TOOLTIP_UNITS);
	
			if (tipIndex==-1) {
				// If there's no number for this stamp, why display it in the investigate tool?
				// Disabled 10/4/2022 - SLD
//				iData.add("ID", myStamp.getId());
				return iData;
			}
			
			Object val = getStamp().getData()[tipIndex];
			
			try {
				if (val instanceof Number) {
					iData.add(stampLayer.viewToUpdate.myFocus.table.getTableModel().getColumnName(tipIndex), ""+getStamp().getData()[tipIndex], tipUnits, true);
				}
			} catch (NumberFormatException nfe) {
				// If there's no number for this stamp, why display it in the investigate tool?
				// Disabled 10/4/2022 - SLD
//				iData.add(stampLayer.viewToUpdate.myFocus.table.getTableModel().getColumnName(tipIndex), ""+getStamp().getData()[tipIndex], tipUnits, false);			
			}
					
			return iData;
		}
	}
	
	/**
	 * Calculates and returns the center point (in lon/lat) for this stamp by averaging the points
	 */
    Point2D centerPoint=null;
    
    public Point2D getCenter() {
    	if (centerPoint==null) {
    		
    		HVector corner = new HVector();
    		
    		int validPts = 0;
    		for (int i=0; i<myStamp.getPoints().length; i=i+2) {
    			// If we have a multipolygon, there may be Double.NANs involved that will ruin our day.  Exclude them.
    			double x = myStamp.getPoints()[i];
    			double y = myStamp.getPoints()[i+1];
    			if (Double.isNaN(x) || Double.isNaN(y)) continue;
    			corner=corner.add(new HVector(new Point2D.Double(myStamp.getPoints()[i], myStamp.getPoints()[i+1])));
    			validPts++;
    		}
    		corner.div(validPts);
    		
    		centerPoint = corner.toLonLat(null);    		
    	}
    	
    	return centerPoint;
    }

	/**
	 * Returns the NW stamp corner spatial coordinates as
	 * a point in degrees: x = longitude, y = latitude.
	 */
    public Point2D getNW()
    {
    	if (nw==null) {
    		double pts[] = myStamp.getPoints();
    		nw=new Point2D.Double(pts[0], pts[1]);
    	}
    	return nw;
    }
        
    /**
     * Returns the SW stamp corner spatial coordinates as
     * a point in degrees: x = longitude, y = latitude.
     */
    
    private transient Point2D nw;
    private transient Point2D sw;
    
    public Point2D getSW()
    {
    	if (sw==null) {
    		double pts[] = myStamp.getPoints();
    		sw=new Point2D.Double(pts[pts.length-2], pts[pts.length-1]);
    	}
    	return sw;
    }
    
    private List<String> supportedTypes=null;
    
    public List<String> getSupportedTypes() {
		if (supportedTypes==null) {		
			try {
				String typeLookupStr = "ImageTypeLookup?id="+getId()+"&instrument="+stampLayer.getInstrument()+"&format=JAVA";
						
				ObjectInputStream ois =  new ObjectInputStream(StampLayer.queryServer(typeLookupStr));
				
				supportedTypes = (List<String>)ois.readObject();
				
				ois.close();
			} catch (Exception e) {
				supportedTypes=new ArrayList<String>();
				e.printStackTrace();
			}
		}
		return supportedTypes;
    }
        
    public final synchronized void clearProjectedData()
    {
        paths = null;
        fillAreas = null;
    }
    
//    private Analysis analysis = null;
    
    public synchronized Analysis analyzePoints(ProjObj po) {
    	// TODO: Cache this somehow
//		Analysis analysis = StampUtil.analyzePoints(myStamp.getPoints(), po);

		// TODO: This is dumb.  Do this as one pass somehow?
		Analysis analysis = StampUtil.analyzePoints(StampUtil.cleanedSpatialPoints(myStamp.getPoints()), po);

		if (analysis.is360wideSpatial()) {
    		Number flagVal=null;
    		int colNum = stampLayer.getColumnNum("pole_flag");
    		if (colNum!=-1) {
    			flagVal = (Number)getData(colNum);
    		}

    		if (flagVal!=null) {
    			if (flagVal.intValue()==1) {
    				// North Pole
    				analysis.spatialInteriorPoint = new Point2D.Double(180, 90);
    			} else if (flagVal.intValue()==2) {
    				// South Pole
    				analysis.spatialInteriorPoint = new Point2D.Double(180, -90);
    			}
    		} else {
//    			int nPoleNum = stampLayer.getColumnNum("has_north_pole");
//    			if (nPoleNum!=-1) {
//    				float nPole = (Float)getData(nPoleNum);
//    				if (nPole == 1) {
//    					System.out.println("north north north!");
//    					analysis.spatialInteriorPoint = new Point2D.Double(180,90);
//    				}
//    			}
    		}
		} else {
			boolean crossesMeridian = (analysis.leftCrosses + analysis.rightCrosses)>0 ? true : false;
			analysis.containsSentinalPoint = StampUtil.isPointInsidePolygon(myStamp.getPoints(), Analysis.sentinalPoint, crossesMeridian);
		}
    	
    	return analysis;
    }
    
    
    private static Area area360 = null;
    private static AffineTransform shift120 = null;
    private static AffineTransform shift360 = null;
    private static AffineTransform leftShift360 = null;
    
    private static boolean initPerformed = false;
    
    private synchronized static void init() {
    	if (!initPerformed) {
	        GeneralPath gp360 = new GeneralPath();
	        gp360.moveTo(0, -90.0);
	        gp360.lineTo(0, 90.0);
	        gp360.lineTo(360, 90.0);
	        gp360.lineTo(360, -90.0);
	        gp360.closePath();
	    
	        area360=new Area(gp360);
	
	    	shift120 = new AffineTransform();
	    	shift120.translate(120, 0);
	    	
	    	shift360 = new AffineTransform();
	    	shift360.translate(360, 0);
	    	
	    	leftShift360 = new AffineTransform();
	    	leftShift360.translate(-360, 0);
	    	initPerformed=true;
    	}
    }
    
    
    
    public static double time360a = 0;
    public static double timeNot360a = 0;
    public static double clipTime = 0;
    public static double clipTimeA = 0;
    public static double clipTimeB = 0;
    public static double clipTimeC = 0;
    public static double clipTimeD = 0;
    
    public static double clippyA = 0;
    public static double clippyB = 0;
    public static double clippyC = 0;
    public static double clippyD = 0;
    public static double clippyE = 0;
    
    ArrayList<Area> fillAreas = null;
    
    public synchronized List<Area> getFillAreas() {
    	return getFillAreas(Main.PO);    	
    }

    // TODO: Idea, cache the default projection and polar projection separately.  Cache any other data in a WeakHashMap so it can be garbage
    // collected or cleared out whenever the projection changes, etc.  Maybe clear it out as part of the clearProjectedData()
	HashMap<String, List<Area>> proj2AreasMap = new HashMap<String, List<Area>>();
	
    // TODO: Cache this, and combine this with the default method somehow
    public List<Area> getFillAreas(ProjObj po) {
    	String key = po.getCenterLon()+":"+po.getCenterLat();
    	if (proj2AreasMap.containsKey(key)) {
    		return proj2AreasMap.get(key);
    	} else {
    		// We don't want to do all of this work multiple times if we get a flurry of requests, but we also don't want to slow down
    		// multi-threaded calls later, waiting for locks... 
    		synchronized(this) {
    			if (proj2AreasMap.containsKey(key)) {
    				return proj2AreasMap.get(key);
    			}
	    		List<Area> po_fillAreas = StampUtil.getFillAreas(po, this);
	    		proj2AreasMap.put(key, po_fillAreas);
	        	return po_fillAreas;
    		}
    	}    	
    }

    private List<Area> clippedArea(Area fill, Area a1Area) {
    	ArrayList<Area> returnAreas = new ArrayList<Area>();
    	
    	long time = System.currentTimeMillis();
        fill.intersect(a1Area);
        
        clippyA += (System.currentTimeMillis()-time);
        time = System.currentTimeMillis();
        
        if (!fill.isEmpty()) {
        	Rectangle2D bounds = fill.getBounds2D();
        	
        	double minX = bounds.getMinX();
        	double maxX = bounds.getMaxX();

        	while (minX>360) {
        		fill.transform(leftShift360);

        		minX-=360;
        		maxX-=360;
        	}

            clippyB += (System.currentTimeMillis()-time);
            time = System.currentTimeMillis();

	    	while (minX<0) {
        		fill.transform(shift360);
        		
        		minX+=360;
        		maxX+=360;
        	}

	        clippyC += (System.currentTimeMillis()-time);
	        time = System.currentTimeMillis();

        	if (minX<360 && maxX>360) {
        		Area a360 = new Area(fill);

        		a360.intersect(area360);
        		
        		returnAreas.add(a360);
        		fill.subtract(area360);
        		
        		fill.transform(leftShift360);
        		
                clippyD += (System.currentTimeMillis()-time);

        	}      	
        }
        returnAreas.add(fill);
        
    	return returnAreas;
    }
    
    // TODO: Make this properly generic
    public Double[] getLonLatRadius() {
    	Object o = getData(stampLayer.getColumnNum("off_body_fp"));
    	if (o==null) return new Double[0];
    	return (Double[])o;
    }
    
    Point2D boresight = null;
    boolean boresightBad = false;
    public Point2D getBoresight() {
    	if (boresight==null && !boresightBad) {
	    	Number bx = (Number)getData(stampLayer.getColumnNum("boresight_intersection_x"));
	    	Number by = (Number)getData(stampLayer.getColumnNum("boresight_intersection_y"));
	    	Number bz = (Number)getData(stampLayer.getColumnNum("boresight_intersection_z"));
	    	
	    	if (bx==null || by==null || bz==null || bx.doubleValue()==0.0 || by.doubleValue()==0.0 || bz.doubleValue()==0.0) {
	    		Number blon = (Number)getData(stampLayer.getColumnNum("boresight_lon"));
	    		Number blat = (Number)getData(stampLayer.getColumnNum("boresight_lat"));
	    		
	    		if (blon==null || blat==null || blon.doubleValue()<-500 || blat.doubleValue()<-500) {
	    			boresightBad = true;
	    		} else {
	    			HVector bv = new HVector(360-blon.doubleValue(), blat.doubleValue());
	    			boresight = new Point2D.Double(bv.lon(),  bv.lat());
	    		}
	    		return boresight;
	    	}
	    	
	    	HVector bv = new HVector(bx.doubleValue(), by.doubleValue(), bz.doubleValue());
	    	boresight = new Point2D.Double(360-bv.lon(),  bv.lat());
    	}
    	return boresight;
    }
    
    // No longer used in the base class, still used in the sub-classes.
    // TODO: Probably should be moved away from
    ArrayList<GeneralPath> paths = null;
    
	HashMap<String, List<GeneralPath>> proj2PathsMap = new HashMap<String, List<GeneralPath>>();

    /* This method returns a path defining the image area of this stamp in WORLD COORDINATES */
    public synchronized List<GeneralPath> getPath(ProjObj po)
    {
    	String key = po.getCenterLon()+":"+po.getCenterLat();
    	if (proj2PathsMap.containsKey(key)) {
    		return proj2PathsMap.get(key);
    	} else {
    		// We don't want to do all of this work multiple times if we get a flurry of requests, but we also don't want to slow down
    		// multi-threaded calls later, waiting for locks... 
    		synchronized(this) {
    			if (proj2PathsMap.containsKey(key)) {
    				return proj2PathsMap.get(key);
    			}

		//    	System.out.println("Generating paths!");
		    	// TODO: Add caching of projection specific paths
		    	
		        ArrayList<GeneralPath> po_paths = null;
		
		    	// TODO: This routine REQUIRES that points be in the 0-360 space.  Test and do the right thing if they aren't.
		    	// TODO: Determine what that right thing is....
		    	
		//        if(paths == null)
		//        {
		        	// TODO: Expensive, probably
		        	Analysis stats = analyzePoints(po);
		
		        	po_paths = new ArrayList<GeneralPath>();
		        	
		        	double lastX=Double.NaN, lastY=Double.NaN;
		        	
		        	double firstX=Double.NaN, firstY=Double.NaN; // The original start position, that will be used when closing the path
		        	
		        	double minX=Double.NaN, maxX=Double.NaN;  // Min and Max X values, because we can't go over 180 degrees or things get confused
		        	// TODO: Possibly just break everything at world lines of 180 *AND* 360?
		        	
		        	double lastSpatialX=Double.NaN;
		        	double lastSpatialY=Double.NaN;
		        	
		            Point2D pt;
		                        
		            double pts[] = myStamp.getPoints();
		
		            pts = StampUtil.cleanedSpatialPoints(pts);
		            
		//            for (int i=0; i<pts.length; i+=2) {
		//            	System.out.println("pts["+i+"] = " + pts[i] + " , " + pts[i+1]);;
		//            }	
		            
		            boolean moveNext=true;
		            boolean closePath=true;
		            
		            GeneralPath thisPath = new GeneralPath();
		            
		            for (int i=0; i<pts.length; i=i+2) {
		            	boolean skipPt = false;
		            	
		            	
		            	double spatialX = pts[i];
		            	double spatialY = pts[i+1];
		            	
		            	
		            	
		            	if (Double.isNaN(pts[i]) || Double.isNaN(pts[i+1])) {
		            		moveNext=true;
		            		closePath=false;
		            		continue;
		            	} else {
		            		pt = po.convSpatialToWorld(pts[i], pts[i+1]);
		            	}
		
		            	double x = pt.getX();
		            	double y = pt.getY();
		            	
		            	while (x<0) {
		            		x+=360;
		            	}
		            	
		            	while (x>360) {
		            		x-=360;
		            	}
		            	
		                if (moveNext) {
		                	thisPath.moveTo(x, y);
		                	firstX = x;
		                	firstY = y;
		                	minX = x;
		                	maxX = x;
		                	
		                	moveNext=false;
		                } else {
		                    // TODO: Document.  Removing artificial points
		                	if (stats.is360wideSpatial() && lastSpatialX==0 && spatialX==0) {
		                		skipPt=true;
		                	} else if (stats.is360wideSpatial() && lastSpatialX==360 && spatialX==360) {
		                		skipPt=true;
		                	} else if (stats.is360wideSpatial() && lastSpatialX==0 && spatialX==360) {
		                		skipPt=true;
		                	} else if (stats.is360wideSpatial() && lastSpatialX==360 && spatialX==0) {
		                		skipPt=true;
		                	} 
		                	
		                	
		                	
		                	// We don't want a single path to be wider than 180 degrees, or various JMARS functions start trying to interpret things as wrapping the other
		                	// way, which is the usual safe case.  We break at 179 degrees just to avoid edge cases.
		                    		
		                	if (lastX-x>180) {
		                		// The line to 360 makes our total length larger than 179... so cut off before this segment
		                		if (Math.abs(360-minX)>179) {
		                    		double interceptY = intersectLat(lastX, lastY, x, y, minX+179); 
		                    		thisPath.lineTo(minX+179, interceptY);
		    						po_paths.add(thisPath);
		
		    						thisPath = new GeneralPath();
		    						thisPath.moveTo(minX+179, interceptY);
		    						lastX = minX+(179-minX);
		    						lastY = interceptY;
		                		}
		                		
		                		double interceptY = intersectLat(lastX,  lastY,  x,  y);
		                		thisPath.lineTo(360, interceptY);
		                		po_paths.add(thisPath);
		                		thisPath = new GeneralPath();
		                		thisPath.moveTo(0, interceptY);
		                		thisPath.lineTo(x, y);
		                    	minX = 0;
		                    	maxX = x;
		                	} else if (x-lastX>180) {
		                		if (Math.abs(maxX)>179) {
		                    		double interceptY = intersectLat(lastX, lastY, x, y, maxX-179); 
		                    		thisPath.lineTo(maxX-179, interceptY);
		    						po_paths.add(thisPath);
		
		    						thisPath = new GeneralPath();
		    						thisPath.moveTo(maxX-179, interceptY);
		    						lastX = maxX-179;
		    						lastY = interceptY;
		                		}
		                		
		                		double interceptY = intersectLat(lastX,  lastY,  x,  y);
		                		thisPath.lineTo(0, interceptY);
		                		po_paths.add(thisPath);
		                		thisPath = new GeneralPath();
		                		thisPath.moveTo(360, interceptY);
		                		thisPath.lineTo(x, y);
		                    	minX = x;
		                    	maxX = 360;
		                	} else if (Math.abs(x-minX)>179) {
		                		double interceptY = intersectLat(lastX, lastY, x, y, minX+179); 
		                		thisPath.lineTo(minX+179, interceptY);
								po_paths.add(thisPath);
								thisPath = new GeneralPath();
								thisPath.moveTo(minX+179, interceptY);
		                		thisPath.lineTo(x, y);
		                		minX = minX+179;
		                		maxX = minX;
		                	} else if (Math.abs(x-maxX)>179) {
		                		double interceptY = intersectLat(lastX, lastY, x, y, maxX-179); 
		                		thisPath.lineTo(maxX-179, interceptY);
								po_paths.add(thisPath);
								thisPath = new GeneralPath();
								thisPath.moveTo(maxX-179, interceptY);
		                		thisPath.lineTo(x, y);
		                		minX = maxX-179;
		                		maxX = minX;                		
		                	} else {
		               			thisPath.lineTo(x, y);
		                	}
		                }
		                
		            	lastX=x;
		            	lastY=y;
		
		            	if (!skipPt) {
		            	if (Double.isNaN(minX) || minX > lastX) {
		                	minX = lastX;
		                }
		                
		                if (Double.isNaN(maxX) || maxX < lastX) {
		                	maxX = lastX;
		                }
		            	}
		                lastSpatialX = spatialX;
		                lastSpatialY = spatialY;
		                
		            }
		            
		            
		            if (closePath && pts.length>0) {
						if (Math.abs(firstX-lastX)>180) {
							double interceptY = intersectLat(lastX, lastY, firstX, firstY);
		
							if (lastX>firstX) {             // need to wrap around across 360								
								thisPath.lineTo(360, interceptY);
								po_paths.add(thisPath);
								thisPath = new GeneralPath();
								thisPath.moveTo(0, interceptY);
								thisPath.lineTo(firstX,  firstY);
							} else {                            // need to wrap around across 0
								thisPath.lineTo(0, interceptY);
								po_paths.add(thisPath);
								thisPath = new GeneralPath();
								thisPath.moveTo(360, interceptY);
								thisPath.lineTo(firstX,  firstY);
							}
						} else {
							if (Math.abs(firstX-minX)>179) {
		                		double interceptY = intersectLat(lastX, lastY, firstX, firstY, minX+179); 
		                		thisPath.lineTo(minX+179, interceptY);
								po_paths.add(thisPath);
								thisPath = new GeneralPath();
								thisPath.moveTo(minX+179, interceptY);
		                		thisPath.lineTo(firstX, firstY);
		                		minX = minX+179;
		                		maxX = minX;
		                	} else if (Math.abs(firstX-maxX)>179) {
		                		double interceptY = intersectLat(lastX, lastY, firstX, firstY, maxX-179); 
		                		thisPath.lineTo(maxX-179, interceptY);
								po_paths.add(thisPath);
								thisPath = new GeneralPath();
								thisPath.moveTo(maxX-179, interceptY);
		                		thisPath.lineTo(firstX, firstY);
		                		minX = maxX-179;
		                		maxX = minX;                		
		                	}
												
							thisPath.lineTo(firstX,  firstY);
						}
		            	
		            }
		            po_paths.add(thisPath);
		//        } 
		        
		        
		    		proj2PathsMap.put(key, po_paths);
		    		
		    		return po_paths;
    		}
    	}
    }

    // Indicates whether any of this polygon contains off body points
    public boolean isOffBody() {
    	return false;
    }
    
	// Cache the 3d polygons here for performance reasons
	transient Polygon polygons3D[] = null;
	
	public Polygon[] get3DPolygon() {
		return polygons3D;
	}
    
	public void set3DPolygon(Polygon newPolygons[]) {
		polygons3D = newPolygons;
	}
    
    
	/*
	 * Given two points that are greater than 180 degrees apart, determine the latitude where they cross the prime meridian
	 */
	private double intersectLat(double lon1, double lat1, double lon2, double lat2) {
		if (lon1>lon2) {   // need to wrap around across 360
			double slope = (lat1-lat2) / (lon1 - (lon2+360));
			
			return -slope*(lon1 - 360) + lat1;			
		} else {                   // need to wrap around across 0
			double slope = (lat1-lat2) / (lon1 - (lon2-360));
			
			return -slope*(lon1) + lat1;
		}
	}

	private double intersectLat(double lon1, double lat1, double lon2, double lat2, double intersectLon) {
		double slope = (lat1-lat2) / (lon1 - lon2);
			
		return -slope*(lon1 - intersectLon) + lat1;			
	}
	
	// Seems to only be used by LineShapes - probably should be based off of fill path if used by other stamp shapes
    public boolean intersects(Rectangle2D intersectBox, Rectangle2D intersectBox2) {

    	if (stampLayer.lineShapes()) {
	        
	        double pts[] = myStamp.getPoints();
	        
	        Point2D lastPt = null;
	        Point2D curPt = null;
	        	        
	        for (int i=0; i<pts.length; i=i+2) {
	        	if (Double.isNaN(pts[i]) || Double.isNaN(pts[i+1])) {
	        		return false;
	        	} else {
	        		curPt = Main.PO.convSpatialToWorld(pts[i], pts[i+1]);
	        	}
	        	
	        	if (lastPt!=null && curPt!=null) {
	        		Shape line = normalize360(new Line2D.Double(lastPt,  curPt));
	        		if (line.intersects(intersectBox)) {
	        			return true;
	        		}
	        		if (intersectBox2!=null && line.intersects(intersectBox2)) {
	        			return true;
	        		}
	        	}
            	
        		lastPt = curPt;
	        }
    	} else {
    		List<GeneralPath> paths = getPath(Main.PO);
    		
    		for (GeneralPath path : paths) {
    			if (path.intersects(intersectBox)) return true;
    			if (intersectBox2!=null && path.intersects(intersectBox2)) return true;    			
    		}
    		
    		return false;
    	}
    	
    	
    	return false;
    }
    
    
/**
 ** Given a shape, iterates over it and performs the given
 ** coordinate modification to every point in the shape.
 **/
private static Shape modify(Shape s, CoordModifier cm)
 {
GeneralPath gp = new GeneralPath();
PathIterator iter = s.getPathIterator(null);
float[] coords = new float[6];

// NOTE: No loss of precision in coords. All of the
// GeneralPath.foobarTo() methods take FLOATS and not doubles.

while(!iter.isDone())
 {
    switch(iter.currentSegment(coords))
     {

     case PathIterator.SEG_CLOSE:
	gp.closePath();
	break;

     case PathIterator.SEG_LINETO:
	cm.modify(coords, 2);
	gp.lineTo(coords[0], coords[1]);
	break;

     case PathIterator.SEG_MOVETO:
	cm.modify(coords, 2);
	gp.moveTo(coords[0], coords[1]);
	break;

     case PathIterator.SEG_QUADTO:
	cm.modify(coords, 4);
	gp.quadTo(coords[0], coords[1],
		  coords[2], coords[3]);
	break;

     case PathIterator.SEG_CUBICTO:
	cm.modify(coords, 6);
	gp.curveTo(coords[0], coords[1],
		   coords[2], coords[3],
		   coords[4], coords[5]);
	break;

     default:
	//log.aprintln("INVALID GENERALPATH SEGMENT TYPE!");

     }
    iter.next();
 }
return	gp;
 }

/**
 ** ONLY FOR CYLINDRICAL: Given a shape in world coordinates,
 ** "normalizes" it. This ensures that its left-most x coordinate
 ** is within the x-range [0:360], and that there is no
 ** wrap-around (that is, the shape simply pushes past 360).
 **/
public static Shape normalize360(Shape s)
 {
double x = s.getBounds2D().getMinX();
if(x < 0  ||  x >= mod)
    s = modify(s, cmModulo);

if(s.getBounds2D().getWidth() >= mod/2)
    s = modify(s, cmWrapping);

return	s;
 }


// Quick hack to allow verbatim code-reuse from GraphicsWrapped.java
private static final double mod = 360;

/**
 ** Performs the modulo operation on a shape's coordinates.
 **/
private static final CoordModifier cmModulo =
new CoordModifier()
 {
    public void modify(float[] coords, int count)
     {
	for(int i=0; i<count; i+=2)
	    coords[i] -= Math.floor(coords[i]/mod)*mod;
     }
 };

    /**
     ** Takes care of wrap-around on a shape's coordinates.
     **/
    private static final CoordModifier cmWrapping =
	new CoordModifier()
	 {
	    public void modify(float[] coords, int count)
	     {
		for(int i=0; i<count; i+=2)
		    if(coords[i] < mod/2)
			coords[i] += mod;
	     }
	 };
	 
	    /**
	     ** Interface for modifying a single coordinate of a {@link
	     ** PathIterator}.
	     **/
	    private static interface CoordModifier
	     {
		/**
		 ** @param coords The coordinate array returned by a shape's
		 ** {@link PathIterator}.
		 ** @param count The number of coordinates in the array (as
		 ** determined by the point type of the {@link PathIterator}.
		 **/
		public void modify(float[] coords, int count);
	     }


}
