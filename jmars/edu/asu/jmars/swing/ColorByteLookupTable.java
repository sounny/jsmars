package edu.asu.jmars.swing;

import java.awt.image.LookupTable;

public class ColorByteLookupTable extends LookupTable{

	private int alphaIndex;
	private byte[][] data;
	
	
	/**
	 * A byte look up table that looks up the using the index/indices
	 * of the color bands, instead of the index of the input alpha.
	 * @param data The band data that each input value should map to.
	 * @param alphaIndex The index of the alpha channel
	 */
	public ColorByteLookupTable(byte[][] data, int alphaIndex) {
		super(0, data.length);
		this.alphaIndex = alphaIndex;
		this.data = data;
	}

	
    public int[] lookupPixel(int[] src, int[] dst){
        if (dst == null) {
            // Need to alloc a new destination array
            dst = new int[src.length];
        }

        boolean isGrayscale = true;
        //check if input is grayscale (color bands have the same values)
        //if the size is 2 then it is grayscale, otherwise, check all bands 
        //and confirm they are of the same value (ex. 128,128,128)
        if(src.length>2){
	        int[] colorData = new int[data.length];
	        //create a copy of the source contents, minus the alpha channel
	        for(int i=0; i<src.length; i++){
	        	if(i!=alphaIndex){
	        		colorData[i] = src[i];
	        	}
	        }
	        //check that all the remaining color contents are the same
	        for(int i=0; i<colorData.length-1; i++){
	        	if(colorData[i]!=colorData[i+1]){
	        		isGrayscale = false;
	        	}
	        }
        }
        
        //if the input is grayscale, look up the same index for all bands
        if(isGrayscale){
        	int index = src[0];
        	for(int i=0; i < src.length; i++){
        		//use the same index for all bands, including alpha
        		dst[i] = (int) data[i][index];
        	}
        }else {
        	//keep track of each band's index
        	int indexSum = 0;
        	
            for (int i=0; i < src.length; i++) {
            	//skip the alpha channel
            	if(i == alphaIndex){
            		continue;
            	}
            	
            	//get the index for the channel
                int s = src[i];
                if (s < 0) {
                    throw new ArrayIndexOutOfBoundsException("src["+i+"] is less than zero");
                }
                
                indexSum+=s;
                
                //use the channel and index for the new value
                dst[i] = (int) data[i][s];
            }
            
            //set the alpha (use the average of the color indices as
            // the index for the alpha and set the destination of the
            // value from that index)
            int aveIndex = Math.round(indexSum/(src.length-1));
            dst[alphaIndex] = (int) data[alphaIndex][aveIndex];
        }
        return dst;
    }
}
