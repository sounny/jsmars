package edu.asu.jmars.viz3d.renderer.input.files;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.GZIPOutputStream;


public class ObjToPackedBinGz {

	public static void main(String[] args) throws IOException {
		ObjToPackedBinGz obj = new ObjToPackedBinGz();
		obj.processFile(args[0]);
	}

	public void processFile(String filename) throws IOException {
		String objFileName = filename;
		String outFileName = objFileName.replaceAll("\\.obj$", ".bin.gz");
		
		long t0, t1;
	
		File objFile = new File(objFileName);
		t0 = System.currentTimeMillis();
		Obj obj = new Obj(objFile, objFile.getName());
		System.out.format("Obj file=%s triangles=%d vertices=%d\n", objFileName, obj.getTriangles().length/3, obj.getVertices().length/3);
		t1 = System.currentTimeMillis();
		System.out.format("Time to read %s: %dms (%gs)\n", objFileName, (t1-t0), (t1-t0)/1000.0);

		t0 = System.currentTimeMillis();
		GZIPOutputStream os = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(outFileName)));
		ByteBuffer bbuf = ByteBuffer.allocate(2*Integer.BYTES);
		bbuf.order(ByteOrder.BIG_ENDIAN);
		bbuf.putInt(obj.getVertices().length/3); // vertex count
		bbuf.putInt(obj.getTriangles().length/3); // plate count
		os.write(bbuf.array());
		
		bbuf = ByteBuffer.allocate(obj.getVertices().length*Float.BYTES);
		bbuf.order(ByteOrder.BIG_ENDIAN);
		for(float f: obj.getVertices()) {
			bbuf.putFloat(f);
		}
		os.write(bbuf.array());
		
		bbuf = ByteBuffer.allocate(obj.getTriangles().length*Integer.BYTES);
		bbuf.order(ByteOrder.BIG_ENDIAN);
		for(int t: obj.getTriangles()) {
			bbuf.putInt(t); // triangle vertex indices appear to be 1-based in our input 
		}
		//System.out.println("limit: "+bbuf.limit()/(Integer.BYTES*3));
		os.write(bbuf.array());
		
		os.close();
		t1 = System.currentTimeMillis();
		System.out.format("Time to write %s: %dms (%gs)\n", outFileName, (t1-t0), (t1-t0)/1000.0);
	}
}
