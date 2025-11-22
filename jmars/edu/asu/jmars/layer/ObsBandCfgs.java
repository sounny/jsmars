package edu.asu.jmars.layer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import edu.asu.jmars.util.Util;


public class ObsBandCfgs {
	public static Map<String,BandCfg> getIrBandCfgsMap()
	{
		if (irBandCfgsMap == null){
			irBandCfgsMap = new TreeMap<String,BandCfg>();

			BandCfg bcfg = new BandCfgIR("OFF", 0x0, "None/Disabled");
			irBandCfgsMap.put(bcfg.getName(), bcfg);

			bcfg = new BandCfgIR("DAY", 0x3ff, "Day time");
			irBandCfgsMap.put(bcfg.getName(), bcfg);

			bcfg = new BandCfgIR("NIGHT", 0x308, "Night time");
			irBandCfgsMap.put(bcfg.getName(), bcfg);

			bcfg = new BandCfgIR("TRIPLET", 0x3c8, "Triplet Night Time");
			irBandCfgsMap.put(bcfg.getName(), bcfg);

			//bcfg.bandValue = 0x200; //Band 10 -- as per Phil
			//bcfg.bandValue = 0x100; //Band 09 -- as of April 1st, 2003 (no it's not an April Fool's Joke!)
			bcfg = new BandCfgIR("CAL", 0x100, "Calibration Reset");
			irBandCfgsMap.put(bcfg.getName(), bcfg);

			bcfg = new BandCfgIR("IRSUM", 0x3fc, "IR Summing");
			irBandCfgsMap.put(bcfg.getName(), bcfg);
			
			bcfg = new BandCfgIR("CLOUD", 0x394, "Cloud");
			irBandCfgsMap.put(bcfg.getName(), bcfg);
		}

		// if (irBandCfgsMap == null){ irBandCfgsMap = loadBandCfgs(false); }
		return Collections.unmodifiableMap(irBandCfgsMap);
	}

	public static Map<String,BandCfg> getVisBandCfgsMap()
	{
		if (visBandCfgsMap == null){
			visBandCfgsMap = new TreeMap<String,BandCfg>();

			BandCfg bcfg = new BandCfgVis("OFF", 0x0, "None/Disabled");
			visBandCfgsMap.put(bcfg.getName(), bcfg);
			
			/*
            bcfg = new BandCfg("BLUE - 0.423", 0x2, "Blue Channel only");
            visBandCfgsMap.put(bcfg.getName(), bcfg);

            bcfg = new BandCfg("GREEN - 0.553", 0x10, "Green Channel only");
            visBandCfgsMap.put(bcfg.getName(), bcfg);

            bcfg = new BandCfg("IR1 - 0.751", 0x8, "IR-1 Channel only");
            visBandCfgsMap.put(bcfg.getName(), bcfg);

            bcfg = new BandCfg("IR2 - 0.870", 0x1, "IR-2 Channel only");
            visBandCfgsMap.put(bcfg.getName(), bcfg);
			 */

			bcfg = new BandCfgVis("RED", 0x4, "Red Channel only");
			visBandCfgsMap.put(bcfg.getName(), bcfg);

			bcfg = new BandCfgVis("2,4", 0x0a, "Atmos-1");
			visBandCfgsMap.put(bcfg.getName(), bcfg);

			bcfg = new BandCfgVis("2,3,5", 0x16, "RGB");
			visBandCfgsMap.put(bcfg.getName(), bcfg);

			bcfg = new BandCfgVis("2,4,5", 0x1a, "Atmos-2");
			visBandCfgsMap.put(bcfg.getName(), bcfg);

			bcfg = new BandCfgVis("2,3,4,5", 0x1e, "4-Color (RGB + IR1)");
			visBandCfgsMap.put(bcfg.getName(), bcfg);

			bcfg = new BandCfgVis("ALL", 0x1f, "5-Color (All)");
			visBandCfgsMap.put(bcfg.getName(), bcfg);
		}

		// if (visBandCfgsMap == null){ visBandCfgsMap = loadBandCfgs(true); }
		return Collections.unmodifiableMap(visBandCfgsMap);
	}

	private static Map<String,BandCfg> irBandCfgsMap = null;
	private static Map<String,BandCfg> visBandCfgsMap = null;

	public static String OFF = "OFF";
	
	public static void main(String[] args){
		Map<String,BandCfg> bandCfgs;
		
		bandCfgs = ObsBandCfgs.getIrBandCfgsMap();
		for(BandCfg bcfg: bandCfgs.values()){
			System.out.println(bcfg);
		}
		bandCfgs = ObsBandCfgs.getVisBandCfgsMap();
		for(BandCfg bcfg: bandCfgs.values()){
			System.out.println(bcfg);
		}
		
	}

	// ** class BandCfg
	public static class BandCfg {
		final private String   name;
		final private String   description;
		final private int      bandCfg;
		
		public BandCfg(String name, int bandCfg, String desc){
			this.name = name;
			this.bandCfg = bandCfg;
			this.description = desc;
		}

		public String getName(){
			return name;
		}
		
		public String getDescription(){
			return description;
		}
		
		public int getBandCfg(){
			return bandCfg;
		}
		               
		public int getBandCount(){
			return Integer.bitCount(bandCfg);
		}
		
		public int[] getBands(){
			
			int n = getBandCount();
			int[] bands = new int[n];
			int bandCfg = this.bandCfg;
			int b;
			
			int j=0;
			while((b = Integer.lowestOneBit(bandCfg)) > 0){
				bands[j++] = Integer.numberOfTrailingZeros(b)+1;
				bandCfg &= ~b;
			}
			
			return bands;
		}
		
		public double[] getWavelengths() {
			return null;
		}
		
		public String toString(){
			return getClass().getSimpleName()+"["+
				"name="+getName()+","+
				"bandCfg=0x"+Integer.toHexString(getBandCfg())+","+
				"desc="+getDescription()+","+
				"bands=("+Util.join(",", getBands())+"),"+
				getWavelengths() != null ? "wavelengths=("+Util.join(",", getWavelengths())+")" : "" +
				"]";
		}
	}
	
	public static class BandCfgIR extends BandCfg {
		/** Band wavelengths in micro-meters */
		public static double[] wavelengths = { 6.78, 6.78, 7.93, 8.56, 9.35, 10.21, 11.04, 11.79, 12.57, 14.88 };
		
		public BandCfgIR(String name, int bandCfg, String desc) {
			super(name, bandCfg, desc);
		}

		public double[] getWavelengths(){
			int[] bands = getBands();
			double[] wl = new double[bands.length];
			for(int i=0; i<wl.length; i++)
				wl[i] = wavelengths[bands[i]-1];
			return wl;
		}
	}
	
	public static class BandCfgVis extends BandCfg {
		/** Band wavelengths in micro-meters */
		public static double[] wavelengths = { 0.425, 0.540, 0.654, 0.749, 0.860 };
		
		public static int[] filterToBand = { 5, 1, 3, 4, 2 };
		
		public BandCfgVis(String name, int bandCfg, String desc) {
			super(name, bandCfg, desc);
		}
		
		public int[] getBands(){
			/*
			 * For Vis the default decoding of band_cfg returns filters,
			 * which are converted to bands by doing a lookup.
			 */
			int[] bands = super.getBands(); // returns filter-numbers for Vis
			for(int i=0; i<bands.length; i++){
				bands[i] = filterToBand[bands[i]-1];
			}
			Arrays.sort(bands);
			return bands;
		}
		
		public double[] getWavelengths(){
			int[] bands = getBands();
			double[] wl = new double[bands.length];
			for(int i=0; i<wl.length; i++)
				wl[i] = wavelengths[bands[i]-1];
			return wl;
		}
	}
}
