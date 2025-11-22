package edu.asu.jmars.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import edu.asu.jmars.layer.util.ElevationSource;
import edu.asu.jmars.layer.util.InMemElevationSource;
import edu.asu.jspice.JS;
import edu.asu.jspice.JSNadirProviderCentroid;
import edu.asu.jspice.JSScanNormPerpOrbitTrack;
import edu.asu.jspice.JSpice;

/**
 * Prints the angular separation between the THEMIS groundtrack and the center
 * of the shadow of Phobos.
 * 
 * TODO: use nadir basis records to correct for THEMIS attitude changes.
 */
public class PhobosTrackSep {
	private static final int ody = -53;
	private static final int mars = 499;
	private static final int phobos = 401;
	private static final int sun = 10;
	private static JS JS;
	public static void main(String[] args) throws IOException, VicarException, URISyntaxException {
		if (args.length == 0) {
			System.out.println("Arguments:\n\tet1 et2");
			System.out.println("Results:\n\tA table of the form: et sep1 sep2 dir lt lat");
			System.out.println("Where the sep values are radian separation between the THEMIS groundtrack");
			System.out.println("and the Mars intersection of a ray from the Sun through the center of Phobos.");
			System.out.println("Sep1 uses the Mars ellipse model and sep2 uses the MOLA DEM");
			System.out.println("The dir is the absolute radian angle in degrees between the direction of\n" +
				"  the shadow trail and the THEMIS bore sight as measured from the center of the shadow.");
			System.out.println("The lt is the local time in decimal format, e.g. 22.8 is 10 pm and .8*60 seconds.");
			System.out.println("The lat is the latitude in geocentric degrees north of the equator.");
			System.exit(0);
		}
		String moons = "/tes/src/naif/data/mar022-LONG.bsp";
		try {
			JSpice.furnshc(new StringBuffer(moons));
		} catch (Exception e) {
			System.err.println("Couldn't find moon kernel at " + moons);
		}
		BufferedReader br = new BufferedReader(new FileReader("/themis/naif/state.nk"));
		String line;
		while (null != (line = br.readLine())) {
			String[] bits = line.split(":");
			if (bits[0].startsWith("#")) {
				continue;
			} else if (!bits[0].trim().equalsIgnoreCase("OPTG")) {
				try {
					JSpice.furnshc(new StringBuffer(bits[1].trim()));
				} catch (Exception e) {
					System.err.println("Failed to load " + bits[1]);
					e.printStackTrace();
				}
			}
		}
		
		// as soon as kernels are furnished, build JS
		JS = new JS(ody,mars,24.6229,new JSNadirProviderCentroid(),new JSScanNormPerpOrbitTrack());
		
		String url = Config.get("themis2.alt.file.url", "http://jmars.asu.edu/internal/mola_16ppd_topo.vic");
		ElevationSource topo = new InMemElevationSource(url, 3396d, 1/1000.0d);
		double et1 = Double.parseDouble(args[0]);
		double et2 = Double.parseDouble(args[1]);
		boolean lastValid = false;
		int hit = 0, missed = 0;
		System.out.println("et\tsep1\tsep2\tdir\tlt\tlat");
		for (double et = et1; et <= et2; et += 100) {
			HVector phobosPos = JS.spkez(et, phobos)[0];
			HVector sunPos = JS.spkez(et, sun)[0];
			HVector shadowDir = phobosPos.sub(sunPos).unit();
			HVector ellipseHit = JS.surfpt(phobosPos, shadowDir);
			HVector demHit = topo.getSurfacePoint(phobosPos, shadowDir);
			if (ellipseHit == null || demHit == null) {
				if (lastValid) {
					lastValid = false;
					System.out.println();
				}
				missed ++;
				continue;
			}
			hit ++;
			lastValid = true;
			HVector odyPos = JS.spkez(et, ody)[0];
			// compute separation values; we can use 'odyPos' directly since the
			// look vector is toward the center of the planet and therefore the
			// spot THEMIS sees is in the direction of 'odyPos'
			double sep1 = odyPos.separation(ellipseHit);
			double sep2 = odyPos.separation(demHit);
			// get the average direction Phobos moved FROM in the last 100 seconds
			HVector phobosDir = JS.spkez(et-100, phobos)[0].sub(phobosPos).unit();
			// get the direction from the current position of Phobos to the current THEMIS target
			HVector odyTarget = JS.surfpt(odyPos, odyPos.neg());
			HVector shadowToTargetDir = odyTarget.sub(ellipseHit).unit();
			// compute the 'dir' value as the separation between the direction
			// phobos has been moving and the direction from phobos shadow to
			// themis target around the surface normal at the phobos shadow.
			double dir = Math.abs(shadowToTargetDir.separationPlanar(phobosDir, JS.surfnm(ellipseHit)));
			// compute the local time at the THEMIS target
			double lt = MarsOrbitUtil.calcLocalTime((long)et, odyTarget.toLonLat(null));
			// compute the latitude at the THEMIS target
			double lat = odyTarget.latC();
			System.out.println(MessageFormat.format(
				"{0,number,#.###}\t{1,number,#.######}\t{2,number,#.######}\t{3,number,#.###}\t{4,number,#.##}\t{5,number,#.##}",
				et, sep1, sep2, dir, lt, lat));
		}
		System.err.println("Hit " + hit + ", missed " + missed + ", " + (100*hit/(hit+missed)) + "%");
	}
}
