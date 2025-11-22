package edu.asu.jmars.layer.util.features;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class FeatureProviderOmega implements FeatureProvider {
	private static class Record {
		private static final Field fsunmin = new Field("MinSunAngle", Double.class);
		private static final Field fsunmax = new Field("MaxSunAngle", Double.class);
		private static final Field futc = new Field("UTC", String.class);
		private static DateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date utc;
		List<Point2D> points = new ArrayList<Point2D>();
		double minsun = Double.POSITIVE_INFINITY;
		double maxsun = Double.NEGATIVE_INFINITY;
		public Feature getFeature() {
			Feature f = new Feature();
			f.setAttribute(futc, outFormat.format(utc));
			f.setAttribute(fsunmin, minsun);
			f.setAttribute(fsunmax, maxsun);
			f.setPath(new FPath(points.toArray(new Point2D[points.size()]), FPath.SPATIAL_EAST, true));
			return f;
		}
	}
	public String getDescription() {
		return "Omega Polar Campaign Shapes";
	}
	/**
	 * If the file does not end with the file type extention, it should
	 * be added to the end of the file name.
	 */
	private File filterFile(String fileName) {
		if (!fileName.endsWith(getExtension()))
			fileName += getExtension();
		return new File( fileName);
	}
	public File[] getExistingSaveToFiles(FeatureCollection fc, String fileName) {
		File file = filterFile(fileName);
		if (file.exists())
			return new File[]{file};
		return new File[]{};
	}
	public String getExtension() {
		return ".txt";
	}
	public boolean isFileBased() {
		return true;
	}
	public boolean isRepresentable(FeatureCollection fc) {
		return false;
	}
	public FeatureCollection load(String fileName) {
		try {
			return _loadImpl(fileName);
		} catch (Exception e) {
			throw new RuntimeException("Unable to load file", e);
		}
	}
	private FeatureCollection _loadImpl(String fileName) throws IOException {
		FeatureCollection fc = new SingleFeatureCollection();
		List<Record> records = new ArrayList<Record>();
		DateFormat dateParser = new SimpleDateFormat("dd-MMM-yyyy_HH:mm:ss");
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		Record omega = new Record();
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			try {
				omega.utc = dateParser.parse(line);
			} catch (Exception e) {
				// not the start of a points array, skip this line
				continue;
			}
			int numpoints;
			try {
				numpoints = Integer.parseInt(br.readLine().trim());
			} catch (Exception e) {
				// didn't find the number of points right afterward, report it and start new instance
				System.err.println("Didn't find count of points after date");
				omega = new Record();
				continue;
			}
			try {
				while (numpoints-- > 0 && (line = br.readLine()) != null) {
					String[] parts = line.trim().split(" +");
					double lon = Double.parseDouble(parts[0]);
					double lat = Double.parseDouble(parts[1]);
					double sunangle = Double.parseDouble(parts[2]);
					omega.minsun = Math.min(omega.minsun, sunangle);
					omega.maxsun = Math.max(omega.maxsun, sunangle);
					omega.points.add(new Point2D.Double(lon,lat));
				}
			} catch (Exception e) {
				System.err.println("Unable to parse all points");
				omega = new Record();
				continue;
			}
			records.add(omega);
			omega = new Record();
		}
		br.close();
		for (Record r: records) {
			fc.addFeature(r.getFeature());
		}
		return fc;
	}

	public int save(FeatureCollection fc, String fileName) {
		throw new UnsupportedOperationException("Unable to save to this format");
	}

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
}
