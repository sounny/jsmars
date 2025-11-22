package edu.asu.jmars.parsers.gis;

import edu.asu.jmars.layer.util.features.FeatureUtil;
import edu.asu.jmars.parsers.ParseException;
import edu.asu.jmars.parsers.ParseResult;
import edu.asu.jmars.parsers.Parser;
import edu.asu.jmars.parsers.simple.CharacterParser;
import edu.asu.jmars.places.Place;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;
import org.apache.commons.lang3.tuple.Pair;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CoordinatesParser extends Parser<Pair<Pair<Double, Character>, Pair<Double, Character>>> {

	public enum Ordering {
		LAT_LON("latlon") {
			@Override
			public String getOrderingLabel() {
				return "Lat, Lon";
			}

			@Override
			public String format(Point2D spatial) {
				if (spatial == null) return "<none>";
				double x = spatial.getX();
				double y = spatial.getY();
				y = Util.roundToMultiple(y, 0.001);				
				return format(x, y);
			}
			
			@Override
			public String format(double x, double y) {	
				x = formatLongitude(x); //Trent
				y = formatLatitude(y);
				return f.format(y) + degreeSymbol + defaultLat + defaultSeparator + space +
					  f.format(x)  + degreeSymbol + getLongitudeSuffix();
			}			
			

			@Override
			public StringBuffer formatCursorPos(Point2D point, DecimalFormat df) {				
				StringBuffer buff = new StringBuffer();
				String latSystemstr = Config.get(Config.CONFIG_LAT_SYSTEM, LatitudeSystem.OCENTRIC.getName());	
				String latSystem = LatitudeSystem.get(latSystemstr).getName();		
				if(point==null) return buff.append("");							
				buff.append(latSystem.toUpperCase());
				buff.append(dblspace);				
				buff.append(format(point));
				buff.append(space);
				return buff;
			}

			
			@Override
			public String formatPlace(Point2D ll, Place p, Point2D proj) {
				if(ll==null || proj==null || p==null) return "<none>";
				return MessageFormat.format(
						"Location" + space + format(ll.getX(), ll.getY()) + dblspace + 
				        "Projection" + space + format(proj.getX(), proj.getY()) + dblspace +
				        "PPD {0, number, #.###}", p.getPpd());								
			}

			@Override
			public String formatNoMath(Point2D lonlat) {
				if (lonlat == null) return "<none>";				
				return format(lonlat.getX(), lonlat.getY());
			}

			
			@Override
			public String formatPlace(Place place) {
				if (place==null) return "<none>";
				return MessageFormat.format(
						"<html><body>Location: " + format(place.getLonLat().getX(), place.getLonLat().getY()) + 
								"<br>" + "Projection: " + format(place.getProjCenterLonLat().getX(), place.getProjCenterLonLat().getY()) + 
								"<br>" + "PPD: {0}</body></html>", place.getPpd());					 					 						
			}

			@Override
			public double formatLongitude(double loncoord) {
				//Trent      
				String lonSystemstr = Config.get(Config.CONFIG_LON_SYSTEM, LongitudeSystem.EAST_360.getName());	
				LongitudeSystem lonSystem = LongitudeSystem.get(lonSystemstr);
				return lonSystem.format(loncoord);
			}

			@Override
			public char getLongitudeSuffix() {
				String lonSystemstr = Config.get(Config.CONFIG_LON_SYSTEM, LongitudeSystem.EAST_360.getName());	
				LongitudeSystem lonSystem = LongitudeSystem.get(lonSystemstr);
				return lonSystem.getSuffix();
			}

			@Override
			public double formatLatitude(double latcoord) {
				String latSystemstr = Config.get(Config.CONFIG_LAT_SYSTEM, LatitudeSystem.OCENTRIC.getName());	
				LatitudeSystem latSystem = LatitudeSystem.get(latSystemstr);
				return latSystem.format(latcoord);	
			}
		},
		
		LON_LAT("lonlat") {
			@Override
			public String getOrderingLabel() {
				return "Lon, Lat";
			}

			@Override
			public String format(Point2D spatial) {
				if (spatial==null) return "<none>";
				double x = spatial.getX();
				double y = spatial.getY();				
				y = Util.roundToMultiple(y, 0.001);			
				return format(x, y);			
			}
			
			@Override
			public String format(double x, double y) {
				x = formatLongitude(x);  //Trent
				y = formatLatitude(y);
				return f.format(x) + degreeSymbol + getLongitudeSuffix() + defaultSeparator + space + 
					   f.format(y) + degreeSymbol + defaultLat;						
			}	

			
			@Override
			public StringBuffer formatCursorPos(Point2D point, DecimalFormat df) {				
				StringBuffer buff = new StringBuffer();
				String latSystemstr = Config.get(Config.CONFIG_LAT_SYSTEM, LatitudeSystem.OCENTRIC.getName());	
				String latSystem = LatitudeSystem.get(latSystemstr).getName();						
				if (point==null) return buff.append("");			
				buff.append(latSystem.toUpperCase());
				buff.append(dblspace);				
				buff.append(format(point));
				buff.append(space);
				return buff;				
			}
			

		@Override
			public String formatPlace(Point2D ll, Place p, Point2D proj) {
				if(ll==null || proj==null || p==null) return "<none>";
				return MessageFormat.format(
					"Location" + space + format(ll.getX(), ll.getY()) + dblspace +
			        "Projection" + space + format(proj.getX(), proj.getY()) + dblspace +
			        "PPD {0, number, #.###}", p.getPpd());					
			}

		
			@Override
			public String formatNoMath(Point2D lonlat) {
				if (lonlat == null) return "<none>";
			    return format(lonlat.getX(), lonlat.getY());				
			}

			
			@Override
			public String formatPlace(Place place) {
				if (place == null) return "<none>";
				return MessageFormat.format(
						"<html><body>Location: " + format(place.getLonLat().getX(), place.getLonLat().getY()) + 
								"<br>" + "Projection: " + format(place.getProjCenterLonLat().getX(), place.getProjCenterLonLat().getY()) + 
								"<br>" + "PPD: {0}</body></html>", place.getPpd());					   
			}

			@Override
			public double formatLongitude(double loncoord) {
				//Trent
				String lonSystemstr = Config.get(Config.CONFIG_LON_SYSTEM, LongitudeSystem.EAST_360.getName());	
				LongitudeSystem lonSystem = LongitudeSystem.get(lonSystemstr);
				return lonSystem.format(loncoord);
			}

			@Override
			public char getLongitudeSuffix() {
				String lonSystemstr = Config.get(Config.CONFIG_LON_SYSTEM, LongitudeSystem.EAST_360.getName());	
				LongitudeSystem lonSystem = LongitudeSystem.get(lonSystemstr);
				return lonSystem.getSuffix();
			}

			@Override
			public double formatLatitude(double latcoord) {
				String latSystemstr = Config.get(Config.CONFIG_LAT_SYSTEM, LatitudeSystem.OCENTRIC.getName());	
				LatitudeSystem latSystem = LatitudeSystem.get(latSystemstr);
				return latSystem.format(latcoord);	
			}	
		};

		
		private String ordering = "latlon";
		public static final String degreeSymbol = Character.toString('\u00B0');
		public final static char defaultLat = 'N';
		private final static char defaultSeparator = ',';
		private final static char space = ' ';
		private final static String dblspace = "  ";
		private static Map<String, Ordering> orderings = new ConcurrentHashMap<>();
		private static final DecimalFormat f = new DecimalFormat("0.###");

		public abstract String getOrderingLabel();			
		public abstract String format(Point2D spatial);	
		public abstract String format(double x, double y);
		public abstract String formatNoMath(Point2D lonLat);
		public abstract StringBuffer formatCursorPos(Point2D point, DecimalFormat df);	
		public abstract String formatPlace(Point2D ll, Place p, Point2D proj);
		public abstract String formatPlace(Place place);
		public abstract double formatLongitude(double loncoord);
		public abstract double formatLatitude(double latcoord);
		public abstract char getLongitudeSuffix();			
		
		private Ordering(String ordering) {
			this.ordering = ordering;
		}

		public String asString() {
			return this.ordering;
		}

		static {
			Map<String, Ordering> map = new ConcurrentHashMap<>();
			for (Ordering instance : Ordering.values()) {
				map.put(instance.asString(), instance);
			}
			orderings = Collections.unmodifiableMap(map);
		}

		public static Ordering get(String orderingStr) {
			return orderings.get(orderingStr);
		}		
	};

	
	//Longitude system
	public enum LongitudeSystem {
		WEST_360("west360") {
			@Override
			public String getName() {
				return "west360";
			}

			@Override
			public double format(double coordX) {
				coordX = normalizeInto0360Range(coordX); 
				return coordX;
			}

			@Override
			public String getCoordinatesRange() {
				return WESTRANGE360;
			}

			@Override
			public char getSuffix() {
				return 'W';
			}
		},
		
		WEST_180("west180") {
			@Override
			public String getName() {
				return "west180";
			}

			@Override
			public double format(double coordX) {
				coordX = normalizeInto0360Range(coordX); 
				coordX = ((coordX+180.) % 360.) - 180.;	//from 0-360 to -180-180 Trent	
				return coordX;  
			}

			@Override
			public String getCoordinatesRange() {
				return WESTRANGE180;
			}

			@Override
			public char getSuffix() {				
				return 'W';
			}	
		},		
		EAST_360("east360") {
			@Override
			public String getName() {
				return "east360";
			}

			@Override
			public double format(double coordX) {
				coordX = normalizeInto0360Range(coordX); 
				coordX = (360 - (coordX % 360.)) % 360.;  //JMARS west lon => USER east lon				
				return coordX;
			}

			@Override
			public String getCoordinatesRange() {
				return EASTRANGE360;
			}

			@Override
			public char getSuffix() {
				return 'E';
			}	
		},
		EAST_180("east180") {
			@Override
			public String getName() {
				return "east180";
			}

			@Override
			public double format(double coordX) {
				coordX = normalizeInto0360Range(coordX); 
				coordX = (360 - (coordX % 360.)) % 360.;  //JMARS west lon => USER east lon					
				coordX = ((coordX+180) % 360) - 180;	//from 0-360 to -180-180 Trent	
				return coordX;
			}

			@Override
			public String getCoordinatesRange() {			
				return EASTRANGE180;
			}

			@Override
			public char getSuffix() {
				return 'E';
			}	
		};		

		private String system = "east360";
		public final static String EASTRANGE180 = "Positive East -180 to 180";
		public final static String EASTRANGE360 = "Positive East 0 to 360";
		public final static String WESTRANGE180 = "Positive West -180 to 180";
		public final static String WESTRANGE360 = "Positive West 0 to 360";
		private static Map<String, LongitudeSystem> lonsystems = new ConcurrentHashMap<>();
		private static final DecimalFormat f = new DecimalFormat("0.###");		
		public abstract double format(double x);	
		public abstract String getName();
		public abstract String getCoordinatesRange();
		public abstract char getSuffix();
		
		private LongitudeSystem(String system) {
			this.system = system;
		}

		public String asString() {
			return this.system;
		}
		
		public static double normalizeInto0360Range(double coord) {
			coord = FeatureUtil.lonNorm(coord); //takes care if a negative lon
			coord = coord % 360;  //normalize input into 0-360 range;
			return coord;
		}		

		static {
			Map<String, LongitudeSystem> map = new ConcurrentHashMap<>();
			for (LongitudeSystem instance : LongitudeSystem.values()) {
				map.put(instance.asString(), instance);
			}
			lonsystems = Collections.unmodifiableMap(map);
		}

		public static LongitudeSystem get(String system) {
			return lonsystems.get(system);
		}		
	};		
	
	//Latitude system
		public enum LatitudeSystem {
			OCENTRIC("Ocentric") {
				@Override
				public String getName() {
					return PLANETOCENTRIC;
				}

				@Override
				public double format(double coordY) {			
					return coordY;
				}					
			},
			
			OGRAPHIC("Ographic") {
				@Override
				public String getName() {
					return PLANETOGRAPHIC;
				}

				@Override
				public double format(double coordY) {		
					return Util.ocentric2ographic(coordY);
				}
			};		

			private String system = PLANETOCENTRIC;
			public final static String PLANETOCENTRIC = "Ocentric";  
			public final static String PLANETOGRAPHIC = "Ographic";			
			private static Map<String, LatitudeSystem> latsystems = new ConcurrentHashMap<>();
			private static final DecimalFormat f = new DecimalFormat("0.###");		
			public abstract double format(double y);	
			public abstract String getName();			
			
			private LatitudeSystem(String system) {
				this.system = system;
			}

			public String asString() {
				return this.system;
			}

			static {
				Map<String, LatitudeSystem> map = new ConcurrentHashMap<>();
				for (LatitudeSystem instance : LatitudeSystem.values()) {
					map.put(instance.asString(), instance);
				}
				latsystems = Collections.unmodifiableMap(map);
			}

			public static LatitudeSystem get(String system) {
				return latsystems.get(system);
			}		
		};		

	
	private final Ordering ordering;
	private final Parser<Pair<Double, Character>> latParser;
	private final Parser<Pair<Double, Character>> lonParser;
	private final Parser<Character> separatorParser = new CharacterParser(',').stripLeadingWhitespace();

	public CoordinatesParser(char latDefaultDirection, char lonDefaultDirection, Ordering ordering) {
		lonDefaultDirection = ordering.getLongitudeSuffix();
		final Parser<Character> latDirections = oneOf(Arrays.asList(new CharacterParser('N'), new CharacterParser('n'),
				new CharacterParser('S'), new CharacterParser('s')));

		final Parser<Character> lonDirections = oneOf(Arrays.asList(new CharacterParser('E'), new CharacterParser('e'),
				new CharacterParser('W'), new CharacterParser('w')));

		this.ordering = ordering;
		this.latParser = new CoordinateParser(latDirections, latDefaultDirection);
		this.lonParser = new CoordinateParser(lonDirections, lonDefaultDirection);
	}

	@Override
	public ParseResult<Pair<Pair<Double, Character>, Pair<Double, Character>>> parse(String s) throws ParseException {
		switch (this.ordering) {
		case LAT_LON:
			return latParser.stripLeadingWhitespace()
					.thenMaybe(separatorParser)
					.map(Pair::getLeft)
					.then(lonParser)
					.parse(s);
		case LON_LAT:
			return lonParser.stripLeadingWhitespace()
					.thenMaybe(separatorParser)
					.map(Pair::getLeft)
					.then(latParser)
					.parse(s);
		default:
			throw new IllegalArgumentException("Invalid ordering specified");
		}
	}
}
