package edu.asu.jmars.viz3d.renderer.input.files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


/**
 * Class for generating a tri-axial ellipsoid as an OBJ file 
 */
public class PrintStandardObjEllipsoid {

	public static final float DEGS_TO_RAD = 3.14159f / 180.0f;
	public static int numVertices = 0; // Tallies the number of vertex points added.
	public static int numFacets = 0; // Tallies the number of facet points added.
	public static int vertexNum = 1;
	public static int facetNum = 1;

	public static float X_AXIS_RADIUS = 1f;
	public static float Y_AXIS_RADIUS = 1f;
	public static float Z_AXIS_RADIUS = 1f;

	/**
	 * Prints a tri-axial ellipsoid as a triangular mesh
	 * with the specified number of latitude (nLatitude) and longitude (nLongitude)
	 * lines and writes the results to the specified output file.
	 * 
	 * @param pt
	 *            - center point of symmetry of the ellipsoid, i.e. the intersection
	 *            of all three axes
	 * @param xRadius
	 *            - radius of the X axis of symmetry
	 * @param yRadius
	 *            - radius of the Y axis of symmetry
	 * @param zRadius
	 *            - radius of the Z axis of symmetry
	 * @param nLatitude
	 *            - number of latitude lines
	 * @param nLongitude
	 *            - number of longitude lines
	 * @param fout
	 *            - output OBJ text file
	 * @throws IOException
	 */

	void printStandardEllipsoid(Point pt, float xRadius, float yRadius, float zRadius, int nLatitude, int nLongitude,
			File fout) throws IOException {
		int p, s, i, j;
		float x, y, z;
		int nPitch = nLongitude + 1;

		float pitchInc = (180.f / (float) nPitch) * DEGS_TO_RAD;
		float rotInc = (360.f / (float) nLatitude) * DEGS_TO_RAD;

		// ## write vertices:

		BufferedWriter bw = new BufferedWriter(new FileWriter(fout));

		bw.write("v" + " " + pt.x + " " + pt.y + " " + (pt.z + zRadius) + "\n"); // Top vertex
		bw.write("v" + " " + pt.x + " " + pt.y + " " + (pt.z - zRadius) + "\n"); // Bottom vertex
		numVertices += 2;

		int fVert = numVertices; // Record the first vertex index for intermediate vertices.
		for (p = 1; p < nPitch; p++) // Generate all "intermediate vertices":
		{
			z = zRadius * (float) Math.cos(p * pitchInc);
			System.out.println("nPitch = " + nPitch); // bottom vertex
			System.out.println("zRadius = " + zRadius); // bottom vertex
			for (s = 0; s < nLatitude; s++) {
				x = xRadius * (float) Math.cos(s * rotInc) * (float) Math.sin((float) p * pitchInc);
				y = yRadius * (float) Math.sin(s * rotInc) * (float) Math.sin((float) p * pitchInc);

				bw.write("v" + " " + (x + pt.x) + " " + (y + pt.y) + " " + (z + pt.z) + "\n"); // intermediate vertices
				numVertices++;
			}
		}

		// ## write facets between intermediate points:

		for (p = 1; p < nPitch - 1; p++) {
			for (s = 0; s < nLatitude; s++) {
				i = p * nLatitude + s;
				j = (s == nLatitude - 1) ? i - nLatitude : i;
				bw.write("f " + ((j + 2) + fVert) + " " + ((j + 2 - nLatitude) + fVert) + " "
						+ ((i + 1 - nLatitude) + fVert) + "\n");
				bw.write("f " + ((i + 1) + fVert) + " " + ((j + 2) + fVert) + " " + ((i + 1 - nLatitude) + fVert)
						+ "\n");
				numFacets += 2;
			}
		}

		// ## write triangle facets connecting to top and bottom vertex:

		int offLastVerts = fVert + (nLatitude * (nLongitude - 1));
		for (s = 0; s < nLatitude; s++) {
			j = (s == nLatitude - 1) ? -1 : s;
			bw.write("f " + ((s + 1) + fVert) + " " + ((j + 2) + fVert) + " " + (fVert - 1) + "\n");
			bw.write("f " + ((j + 2) + offLastVerts) + " " + ((s + 1) + offLastVerts) + " " + fVert + "\n");

			numFacets += 2;
		}
		bw.close();
	}

	/**
	 * Sample main...read the comments!!!
	 */
	public static void main(String[] args) {
		//read config file from the 
		File config = new File("dsk_config.txt");
		if (!config.exists()) {
			System.out.println("The config file: dsk_config.txt was not found. Program exiting.");
			System.exit(1);
		}
		String[] configArr = null;
		PrintStandardObjEllipsoid printEll = new PrintStandardObjEllipsoid();
		try {
			BufferedReader br = new BufferedReader(new FileReader(config));
			while (br.ready()) {
				String line = br.readLine();
				if (line.indexOf("--") == 0) {
				    //ignore lines that start with --
				    continue;
				}
				line = line.trim();
				configArr = line.split(" ",3);
				if (configArr != null && configArr.length == 3) {
				    printEll.process(configArr[0], configArr[1], configArr[2]);
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

	private void process(String filename, String polar, String equatorial) {
		try {
	//		String filename = args[0];
			filename = "UnitSphere_"+filename;
			File f = new File(filename);
			
			float polarRadius = Float.parseFloat(polar);
			float eqRadius = Float.parseFloat(equatorial);
			System.err.println("Attempting to process file: " + filename);
			// NOTE: the following two values produce the smallest size ellipsoid that
			// renders smoothly in OpenGL
//			int nLatitude = 256; // Number vertical lines.
			
			// doubled resolution to test for shading performance
			int nLatitude = 512; // Number vertical lines.
			
			int nLongitude = nLatitude / 2; // Number horizontal lines.
			// NOTE: for a good ellipsoid use ~half the number of longitude lines than
			// latitude.
			PrintStandardObjEllipsoid pss = new PrintStandardObjEllipsoid();
			Point centerPt = new Point(); // Position the center of output ellipsoid at (0,0,0).
	
			if (filename == null) {
				System.err.println("Must enter: './program name outputfile.obj'\n");
				return;
			}
	
			
			try {
				f.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return;
			}
	
			if (!f.canRead()) {
				System.err.println("Couldn't open output file " + filename);
				return;
			}
			try {
				// Print ellipsoid into the file.
				// hard-coded example of a normalized ellipsoid for Mars which is normalized to the largest
				// axis of symmetry
				pss.printStandardEllipsoid(centerPt, 1f, 1f, polarRadius / eqRadius, nLatitude, nLongitude, f);
				// random test ellipsoid that varies in all three axes
				// pss.printStandardEllipsoid(centerPt, 1.5f, 1.25f, 1f, nLatitude, nLongitude, f);
				
				ObjToPackedBinGz obj = new ObjToPackedBinGz();
				obj.processFile(filename);
				
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("# vertices: " + numVertices);
				System.err.println("# facets: " + numFacets);
				return;
			}
		} catch (Exception eee) {
			eee.printStackTrace();
		}
	}
	/**
	 * Convenience point class (Cartesian (X, Y, Z) )
	 */
	class Point {
		float x;
		float y;
		float z;
	}
}
