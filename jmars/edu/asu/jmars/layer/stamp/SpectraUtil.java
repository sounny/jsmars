package edu.asu.jmars.layer.stamp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpectraUtil {
	
	/**
	 * 
	 * @param wave input
	 * @return channel output
	 */
	public static int waveToChannel(int wave, double[] wavenumbers) {
		int indx=-1;
		
		for (int i=0; i<wavenumbers.length; i++) {
			if (wave>wavenumbers[i]) continue;
			indx = i; 
			break;
		}
		
		// TODO: Is it closer to indx or indx+1?
		return indx;
	}
	
	public static String waveStringToChannelString(String waveString, double[] axis) {
		String regExp = "\\[([^)]+?)\\]";
		
		String indexes[]=waveString.split(regExp);
		
		for (String index : indexes) {
			System.out.println("index = " + index);
		}
		
		// Create a Pattern object
		Pattern r = Pattern.compile(regExp);

		String newStr = waveString;
		
		// Now create matcher object.
		Matcher m = r.matcher(waveString);
		while (m.find( )) {
			System.out.println("Found value: " + m.group(0) );
			System.out.println("Found value: " + m.group(1) );
//			System.out.println("Found value: " + m.group(2) );
			
			String newVal = m.group(1);
			
			if (newVal.startsWith("w")) {
				newVal = newVal.substring(1);
			}
			
			newVal = "["+waveToChannel(Integer.parseInt(newVal),axis)+"]";
			
			newStr = newStr.replace(m.group(0), newVal);

		}
				
		return newStr;
	}
	
	/**
	 * This is used to lower the index in a Java Expression by 1, because the science user's expectation is 1st based rather than 0 based
	 * @param expressionString
	 * @return
	 */
	public static String decrementIndexes(String expressionString) {
		String regExp = "\\[([^)]+?)\\]";
		
		// Create a Pattern object
		Pattern r = Pattern.compile(regExp);

		String newStr = expressionString;
		
		// Now create matcher object.
		Matcher m = r.matcher(expressionString);
		while (m.find( )) {
			String newVal = m.group(1);
			newVal = "["+(Integer.parseInt(newVal)-1)+"]";
			
			newStr = newStr.replace(m.group(0), newVal);
		}
				
		return newStr;
	}
	
	public static void main(String args[]) {
		int channel = 1;
		double[] wavenumbers = new double[]{0, 1.5, 3, 5.5, 8, 11, 15, 90, 110};
	
		
		System.out.println(decrementIndexes("(expression[35]+expression[36])/expression[37]"));
		
		System.out.println("Channel = " + channel);
		
		int wave = 100;
		
		System.out.println("wave = " + wave);;
		
		System.out.println("waveToChannel = " + waveToChannel(wave, wavenumbers));
		
		String waveString = "(l2_cal_rad[w100] + l2_cal_rad[w101]) / (l2_cal_rad[w5] + l2_cal_rad[w10:w15])";
		
		System.out.println("waveString = " + waveString);
		
		System.out.println("waveStringToChannelString = " + waveStringToChannelString(waveString, wavenumbers));
	}
	
}
