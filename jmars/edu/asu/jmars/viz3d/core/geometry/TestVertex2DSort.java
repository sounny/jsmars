package edu.asu.jmars.viz3d.core.geometry;

import java.util.ArrayList;
import java.util.Collections;

public class TestVertex2DSort {
	
	static final int X = 0;
	static final int Y = 1;

	public static void main(String[] args) {
		float[] start = new float[] {2.5563254f, -0.93114907f};
		
		float[][] points = new float[][] {{2.5921848f, -0.82436424f},
										{2.5743942f, -0.87734073f},
										{2.556325f, -0.9311491f},
										{2.5743942f, -0.8773407f}};
										
		int[] sorted = sort2DPointsCCW(points, start);
		
		for (int i : sorted ) {
			System.out.println(i);
		}
		
		

	}

	public static int[] sort2DPointsCCW(float[][] in, float[] centroid) {
		ArrayList<Vertex2D> list = new ArrayList<>();
		for (int j=0; j<in.length; j++) {
			list.add(new Vertex2D(in[j][X], in[j][Y], j));		
		}

		
		Collections.sort(list, new Vertex2DAngleComparator(centroid));
		int[] sorted = new int[in.length];
		int idx = 0;
		for (Vertex2D va : list) {
			sorted[idx++] = va.index;
		}
		
		return sorted;
	}

}
