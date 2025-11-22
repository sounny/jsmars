package edu.asu.jmars.layer.stamp;

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.Util;
import edu.asu.msff.DataField;

public class FilterParams {

    private HashMap<DataField, JComponent[]> dataMap = new HashMap<DataField, JComponent[]>();
    private String instrument = null;

    public ArrayList<GeneralPath> paths = null;
    
    
    public FilterParams(String instrument, HashMap<DataField, JComponent[]> map) {
    	dataMap=map;
    	this.instrument=instrument;
    }
    
    public String getInstrument() {
    	return instrument;
    }
    
    /**
     ** Should return the sql query appropriate to satisfy the
     ** data the user has entered into the container.
     **/
    public String getSql() {
       	String sql = "StampFetcher?limit=2000000&instrument="+getInstrument();

        String sqlAppend = "";

        try {
        for (DataField df : dataMap.keySet()){
        	JComponent tc[] = dataMap.get(df);
        	
        	String val="";
        	String val2="";        	
        	
        	if (df.getFieldName().equalsIgnoreCase("intersect") && paths!=null) {
    			StringBuffer sb = new StringBuffer(paths.size()*40);
    			int area=1;
    			int point=1;
    			int line=1;

			    DecimalFormat formatter = new DecimalFormat("0.000");

    			ArrayList<ArrayList<Point2D>> pathLists = new ArrayList<ArrayList<Point2D>>();
    			
    			for (GeneralPath path : paths) {
        			GeneralPath poly = Main.PO.convWorldToSpatial(path);
        			
        			pathLists.addAll(PathUtil.generalPathToPoints(poly, true, true));        			
    			}
    			    			
    			// TODO: Handle the case where this crosses the prime meridian?
				
    			for (ArrayList<Point2D> pathPoints : pathLists) {
    				if (pathPoints.size()==1) {                                                   // Point
    					sb.append("&viewPoint"+point++ +"=");
    					Point2D p = pathPoints.get(0);
						sb.append(formatter.format(p.getX())+","+formatter.format(p.getY()));        					
    				} else if (pathPoints.get(0).equals(pathPoints.get(pathPoints.size()-1))) {          // Closed polygon
				    	if (area==1) {
				    		sb.append("&viewArea=");
				    	} else {
				    		sb.append("&viewArea"+area+"=");
				    	}
				    	area++;
				    	
    					for (Point2D p : pathPoints) {
    						sb.append(formatter.format(p.getX())+","+formatter.format(p.getY()));
    						sb.append(",");
    					}		    					        					
    				} else {                                                                      // Polyline
    					sb.append("&viewLine"+line++ +"=");
    					for (Point2D p : pathPoints) {
    						sb.append(formatter.format(p.getX())+","+formatter.format(p.getY()));
    						sb.append(",");
    					}		    					        					        					
    				}
    			}

    			sqlAppend+=sb.toString();
			    val = "";
    		} else if (df.isMultiSelect()) {
        		if (tc[0] instanceof JCheckBox) {
               		JCheckBox cb[] = (JCheckBox[])tc;
               		
               		for (JCheckBox c : cb) {
               			if (c.isSelected()) {
               				val+=c.getText()+",";
               			}
               		}	
                	if (val.length()>0) {
                	   	String fieldName = df.getFieldName();
                        sqlAppend+="&min"+fieldName+"="+val;
                	}                   		            			
        		}
        	} else if (df.isRange()) {
            	if (tc[0] instanceof JTextField) {
            		JTextField tf[] = (JTextField[])tc;
	            	val=tf[0].getText().trim();
	            	val2 = tf[1].getText().trim();                		
            	} else if (tc[0] instanceof JComboBox) {
            		JComboBox combo[] = (JComboBox[])tc;                		
            		val = (String)combo[0].getSelectedItem();
            		val2 = (String)combo[1].getSelectedItem();
            	}	
            	            	
            	if (df.getFieldName().equalsIgnoreCase("longitude")) {
            		// The "MainView" feature attempts to create a request that represents the area currently visible to the
            		// user in the JMARS MainView.  This is complicated by the shape of the view not necessarily resembling anything
            		// like the shape of the area needed to correctly query the database.  The logic below isn't pretty, but
            		// seems to work.  More straight forward implementations all failed in one case or another.
            		if (val.equalsIgnoreCase("MainView")) {
    					Rectangle2D extent = Main.testDriver.mainWindow.getProj().getWorldWindow();

    					{
	    					double x = extent.getMinX();
	    					double y = extent.getMinY();
	    					double w = extent.getWidth();
	    					double h = extent.getHeight();
	    					
	    					if (y<-90) {
	    						extent.setRect(x, -90, w, h-(-90-y));
	    					}

	    					if (y+h>89.999) {
	    						extent.setRect(x, y, w, 89.999-y);
	    					}	    					
	    					
	    					if (w>180) {
    							Util.showMessageDialog("MainView filtering not supported with current view.  Lat/Lon filter will be skipped for this query.",
    									"Unsupported Filter",
    									JOptionPane.INFORMATION_MESSAGE
    							);
    							continue;
	    					}
	    					
    					}
    					
    					float x = (float)extent.getMinX();
    					float y = (float)extent.getMinY();    					
    					float width = (float)extent.getWidth();    		
    					float height = (float)extent.getHeight();
    					
    					ArrayList<Point2D> pts = new ArrayList<Point2D>();

    					// This next section of code walks each side of the MainView and counts how many times we cross the
    					// prime meridian and what the min and max latitudes are.  These values are then used to detect whether
    					// the view contains a pole, and if so, which pole is in view.
    					int pmCrosses=0; 

    					float minLat=99f;
    					float maxLat=-99f;
    					float n = 1000.0f;
    					int cnt =0;    					
    					
    					float lastLon = -999f;
    					
    					for (float i=x; i<x+width; i=x+(width*cnt++/n)) {
    						Point2D pt = Main.PO.convWorldToSpatial(i, y);
    						float newLon = (float)pt.getX();
    						float newLat = (float)pt.getY();

    						if (lastLon!=-999f) {
    							if (Math.abs(newLon-lastLon)>300) {
    								pmCrosses++;
    							}
    						}
    						if (newLat<minLat) minLat=newLat;
    						if (newLat>maxLat) maxLat=newLat;

    						lastLon = newLon;
    					}
    					
    					cnt=0;
    					lastLon = -999f;
    					for (float i=y; i<y+height; i=y+(height*cnt++/n)) {
    						Point2D pt = Main.PO.convWorldToSpatial(x + width, i);
    						float newLon = (float)pt.getX();
    						float newLat = (float)pt.getY();

    						if (lastLon!=-999f) {
    							if (Math.abs(newLon-lastLon)>300) {
    								pmCrosses++;
    							}
    						}
    						if (newLat<minLat) minLat=newLat;
    						if (newLat>maxLat) maxLat=newLat;

    						lastLon = newLon;
    					}

    					cnt = 0;
    					lastLon = -999f;
    					for (float i=x+width; i>x; i=x+width-(width*cnt++/n)) {
    						Point2D pt = Main.PO.convWorldToSpatial(i, y+height);
    						float newLon = (float)pt.getX();
    						float newLat = (float)pt.getY();

    						if (lastLon!=-999f) {
    							if (Math.abs(newLon-lastLon)>300) {
    								pmCrosses++;
    							}
    						}
    						if (newLat<minLat) minLat=newLat;
    						if (newLat>maxLat) maxLat=newLat;

    						lastLon = newLon;
    					}
    					
    					cnt=0;
    					lastLon = -999f;
    					for (float i=y+height; i>y; i=y+height-(height*cnt++/n)) {
    						Point2D pt = Main.PO.convWorldToSpatial(x, i);
    						float newLon = (float)pt.getX();
    						float newLat = (float)pt.getY();

    						if (lastLon!=-999f) {
    							if (Math.abs(newLon-lastLon)>300) {
    								pmCrosses++;
    							}
    						}
    						
    						if (newLat<minLat) minLat=newLat;
    						if (newLat>maxLat) maxLat=newLat;
    						
    						lastLon = newLon;
    					}
            			
    					boolean containsNorthPole=false;
    					boolean containsSouthPole=false;
    					boolean unsupportedCase=false;
    					
    					// If neither pole is in view, we should have crossed the prime meridian 0 or 2 times.
    					// If a pole is in view, we should have only crossed it once.
    					if (pmCrosses%2==1) {						
    						if (minLat>0) {
    							containsNorthPole=true;
    						} else if (maxLat<0) {
    							containsSouthPole=true;
    						} else {
    							// If we make it to this case, the user has a 'strange' view that includes both the pole and a spot
    							// on the opposite side of the planet.  There are some legitimate cases where this might occur
    							// (and lots of just plain stupid ones) but trying to intelligently determine what the user is doing
    							// and limit the search results appropriately for these cases isn't worth the effort - so we just
    							// display a message and ignore this restriction for this query.
    							unsupportedCase=true;
    							
    							Util.showMessageDialog("MainView filtering not supported with current view.  Lat/Lon filter will be skipped for this query.",
    									"Unsupported Filter",
    									JOptionPane.INFORMATION_MESSAGE
    							);    							
    						}
    						    			
    						// Now that we know our view contains one and only one pole, and we know which one, we will once again
    						// walk around the edges of the MainView, but this time we will keep track of the points so we can build
    						// the appropriate database query.  We will take 10 points per side in an attempt to strike a reasonable
    						// balance between accuracy of our shape and how much resources we consume in obtaining our answer.
	    					n = 10.0f;
	    					lastLon = -999f;

	    					// We store each point in one of two ArrayLists, one set contains the points, in order, before we cross
	    					// the prime meridian, and the other cotnains the remaining points, in order, after the cross.
	    					// By doing this, we can reassemble the two groups of points into one set, in order, entirely contained
	    					// in the 0-360 longitude space.
	    					boolean crossed=false;
	    					ArrayList<Point2D> ptsBefore = new ArrayList<Point2D>();
	    					ArrayList<Point2D> ptsAfter = new ArrayList<Point2D>();
	    					
	    					for (float i=x; i<x+width; i+=(width/n)) {
	    						Point2D pt=Main.PO.convWorldToSpatial(i, y);
	    						
	    						if (!crossed) {
		    						float newLon = (float)pt.getX();
		    						if (lastLon!=-999f) {
		    							if (Math.abs(newLon-lastLon)>300) {
		    								crossed=true;
		    							}	    							
		    						}
		    						lastLon=newLon;
	    						} 
	    						
	    						if (crossed) {
	    							ptsAfter.add(pt);
	    						} else {
	    							ptsBefore.add(pt);
	    						}
	    						
	    					}
	
	    					for (float i=y; i<y+height; i+=(height/n)) {
	    						Point2D pt=Main.PO.convWorldToSpatial(x + width, i);

	    						if (!crossed) {
		    						float newLon = (float)pt.getX();
		    						if (lastLon!=-999f) {
		    							if (Math.abs(newLon-lastLon)>300) {
		    								crossed=true;
		    							}	    							
		    						}
		    						lastLon=newLon;
	    						} 
	    						
	    						if (crossed) {
	    							ptsAfter.add(pt);
	    						} else {
	    							ptsBefore.add(pt);
	    						}

	    					}
	
	    					for (float i=x+width; i>x; i-=(width/n)) {
	    						Point2D pt=Main.PO.convWorldToSpatial(i, y+height);
	    						
	    						if (!crossed) {
		    						float newLon = (float)pt.getX();
		    						if (lastLon!=-999f) {
		    							if (Math.abs(newLon-lastLon)>300) {
		    								crossed=true;
		    							}	    							
		    						}
		    						lastLon=newLon;
	    						} 
	    						
	    						if (crossed) {
	    							ptsAfter.add(pt);
	    						} else {
	    							ptsBefore.add(pt);
	    						}
	    						
	    					}
	
	    					for (float i=y+height; i>y; i-=(height/n)) {
	    						Point2D pt=Main.PO.convWorldToSpatial(x, i);
	    						
	    						if (!crossed) {
		    						float newLon = (float)pt.getX();
		    						if (lastLon!=-999f) {
		    							if (Math.abs(newLon-lastLon)>300) {
		    								crossed=true;
		    							}	    							
		    						}
		    						lastLon=newLon;
	    						} 
	    						
	    						if (crossed) {
	    							ptsAfter.add(pt);
	    						} else {
	    							ptsBefore.add(pt);
	    						}    						
	    					}
    							    				
	    					// Here we merge the two sets of points back into one set.
	    					pts.addAll(ptsAfter);
	    					pts.addAll(ptsBefore);
    							    					
	    					int poleLat = 90;
	    					
	    					if (containsNorthPole) {
	    						poleLat=90;
	    					} else {
	    						poleLat=-90;
	    					}

	    					// The edge points we've collected look more like a line than an Area when viewed in the 360x180 space,
	    					// so we have to explicitly add some additional points to expand the coverage to contain the pole and the 
	    					// area in between.
	    					Point2D startPt = pts.get(0);
	    					Point2D endPt = pts.get(pts.size()-1);

	    					// Determine if we're going from 0-360 or 360-0
	    					if (startPt.getX()<endPt.getX()) {
	    						pts.add(new Point2D.Double(360,endPt.getY()));
		    					pts.add(new Point2D.Double(360,poleLat));
		    					pts.add(new Point2D.Double(0,poleLat));
		    					pts.add(0,new Point2D.Double(0,startPt.getY()));
		    					pts.add(0,new Point2D.Double(0,poleLat));	    						
	    					} else {	    						
	    						pts.add(new Point2D.Double(0,endPt.getY()));
		    					pts.add(new Point2D.Double(0,poleLat));
		    					pts.add(new Point2D.Double(360,poleLat));
		    					pts.add(0,new Point2D.Double(360,startPt.getY()));
		    					pts.add(0,new Point2D.Double(360,poleLat));	    							    						
	    					}	    					
    					} else {
    						// If the MainView doesn't contain the pole, we just iterate around each side and collect point
    						// values.
	    					n = 10.0f;
	    					
	    					for (float i=x; i<x+width; i+=(width/n)) {
	    						pts.add(Main.PO.convWorldToSpatial(i, y));
	    					}
	
	    					for (float i=y; i<y+height; i+=(height/n)) {
	    						pts.add(Main.PO.convWorldToSpatial(x + width, i));
	    					}
	
	    					for (float i=x+width; i>x; i-=(width/n)) {
	    						pts.add(Main.PO.convWorldToSpatial(i, y+height));
	    					}
	
	    					for (float i=y+height; i>y; i-=(height/n)) {
	    						pts.add(Main.PO.convWorldToSpatial(x, i));
	    					}
	    				
	    					// Once we have all of the points, we have to force them to connect to each other in the proper order.
	    					// The adjustPoints method ensures that any two neighboring points are numerically close to each other
	    					// in longitude, adding or subtracting 360 if need be.
	    					for (int i=0; i<pts.size()-1; i++) {    					
	    						adjustPoints(pts.get(i), pts.get(i+1));
	    					}
    					}

    					// Now that we have our edge points, we create a closed path through them.
    					GeneralPath mainView = new GeneralPath();
    					
    					mainView.moveTo((float)(360-pts.get(0).getX()), (float)pts.get(0).getY());

    					for (int i=1; i<pts.size(); i++) {    					
        					mainView.lineTo((float)(360-pts.get(i).getX()), (float)pts.get(i).getY());
    					}
    					mainView.closePath();
    					    					    					
    				    DecimalFormat formatter = new DecimalFormat("0.000");
    				    // Force the use of . as the decimal symbol, so that the backend knows how to interpret these values
    				    formatter.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));

    				    if (!unsupportedCase) {
	    					if (pmCrosses%2==1) {
	    						// If our view contains a pole, we're done at this point.  Just write out the points.
		    				    if (pts.size()>3) {
			    					sqlAppend+="&viewArea=";
			    					
			    					for (Point2D point : pts) {
			    						sqlAppend+=formatter.format(point.getX())+","+formatter.format(point.getY());
			    						sqlAppend+=",";
			    					}
		    				    }    						
	    					} else {
	    						// If our view doesn't contain a pole, we have to put our area back into 0-360 space, breaking it
	    						// into multiple pieces if it crossed the prime meridian.
		    					Area view1 = new Area(mainView);    					
		    					Area view2 = new Area(mainView);
		    					Area view3 = new Area(mainView);
		    					
								Rectangle2D area1 = new Rectangle2D.Double(0.01, -90, 360.0, 180);
								Rectangle2D area2 = new Rectangle2D.Double(360.01, -90, 360.0, 180);
								Rectangle2D area3 = new Rectangle2D.Double(-359.01, -90, 360, 180);
								
								view1.intersect(new Area(area1));
								view2.intersect(new Area(area2));
								view3.intersect(new Area(area3));
		
								// Convert our areas back into sets of points which have each been normalized back into the 0-360 space.
		    					ArrayList<Point2D> points1=getPointsFromArea(view1);
		    					ArrayList<Point2D> points2=getPointsFromArea(view2);
		    					ArrayList<Point2D> points3=getPointsFromArea(view3);
		    					
		    					// In the likely scenario that only 1 or 2 of the areas contain data, toss out the empty sets
		    					// and renumber the rest - primarily to make life easier when parsing this request on the server
		    					// side of things.
		    				    if (points1.size()<3) {
		    				    	points1=points2;
		    				    	points2=points3;
		    				    }
		
		    				    if (points2.size()<3) {
		    				    	points2=points3;
		    				    	points3=new ArrayList<Point2D>();
		    				    }
		
		    				    if (points1.size()>3) {
			    					sqlAppend+="&viewArea=";
			    					
			    					for (Point2D point : points1) {
			    						sqlAppend+=formatter.format(point.getX())+","+formatter.format(point.getY());
			    						sqlAppend+=",";
			    					}		    					
		    				    }
		
		    				    if (points2.size()>3) {
			    					sqlAppend+="&viewArea2=";
			    					
			    					for (Point2D point : points2) {
			    						sqlAppend+=formatter.format(point.getX())+","+formatter.format(point.getY());
			    						sqlAppend+=",";
			    					}
		    				    }
		
		    				    if (points3.size()>3) {
			    					sqlAppend+="&viewArea3=";
			    					
			    					for (Point2D point : points3) {
			    						sqlAppend+=formatter.format(point.getX())+","+formatter.format(point.getY());
			    						sqlAppend+=",";
			    					}
		    				    }
	    					}
    				    }
    					
    					val="";

            		}
            	}

            	if (val.length()>0) {
            	   	String fieldName = df.getFieldName();
                    sqlAppend+="&min"+fieldName+"="+val;
            	}

            	if (val2.length()>0) {
            	   	String fieldName = df.getFieldName();
                    sqlAppend+="&max"+fieldName+"="+val2;
            	}
            	                	
        	} else {
            	if (tc[0] instanceof JTextField) {
            		JTextField tf[] = (JTextField[])tc;
	            	val=tf[0].getText().trim();
            	} else if (tc[0] instanceof JComboBox) {
            		JComboBox combo[] = (JComboBox[])tc;                		
            		val = (String)combo[0].getSelectedItem();
            	} else if (tc[0] instanceof JTextArea) {
            		JTextArea area[] = (JTextArea[])tc;
            		val = (String)area[0].getText().trim();
	            	
	            	if (df.getFieldName().equals("idList")) {
	            		val = val.replaceAll("\\s", ",");
	            	}
            	}
            	
        		if (val!=null && val.trim().length()>0) {
            	   	String fieldName = df.getFieldName();
                    sqlAppend+="&"+fieldName+"="+val;                       
        		}
        	}
        	            	
                    	
        }
        } catch (Exception e) {
        	// We're in bad shape if the URLEncoder throws an exception.
        	return "Unable to generate request: " + e.getMessage();
        }
        return  sql+sqlAppend;
    }

    private void adjustPoints(Point2D pt1, Point2D pt2) {
    	double x1 = pt1.getX();
    	double x2 = pt2.getX();
    	
    	if (!within180(x1, x2)) {
    		double deltas[]={360.0, -360.0, 720.0, -720.0};

    		for (int i=0; i<deltas.length; i++) {	    			
	    		if (within180(x1, x2-deltas[i])) {
	    			pt2.setLocation(x2-deltas[i], pt2.getY());
	    			return;
	    		} 	
    		}
    	}
    }
 
    private boolean within180(double x1, double x2) {
    	return (Math.abs(x2 - x1) < 180);
    }
    
    private ArrayList<Point2D> getPointsFromArea(Area area) {
		PathIterator extent1=Util.normalize360(area).getPathIterator(null);
		
		ArrayList<Point2D> points = new ArrayList<Point2D>();
		
		while(!extent1.isDone()) {
			double d[]=new double[6];
			int v=extent1.currentSegment(d);
			
			Point2D point = new Point2D.Double(d[0], d[1]);
			extent1.next();
			
			if (v!=4) {
				points.add(point);
			}
		}

    	return points;
    }
}
