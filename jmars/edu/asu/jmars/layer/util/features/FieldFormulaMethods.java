package edu.asu.jmars.layer.util.features;

import java.awt.Color;

/**
 * This class is passed to the JEL so that defined methods can be called in expressions. 
 * This allows us to pass an int and return an Ingeter. JEL had problems with 
 * converting an int to an Integer and would always give you an error. 
 * 
 * @ since 1787
 */
public class FieldFormulaMethods {
	public static Integer convertReturnType(int exp){
		return new Integer(exp);
	}
	public static Double convertReturnType(double exp){
		return new Double(exp);
	}
	public static String convertReturnType(String exp){
		return exp;
	}
	public static Long convertReturnType(long exp){
		return new Long(exp);
	}
	public static Double convertReturnType(float exp){
		return new Double(exp);
	}
	public static Boolean convertReturnType(boolean exp){
		return new Boolean(exp);
	}
	public static Color convertReturnType(Color exp){
		return exp;
	}
	/*
	 * public static LineType convertReturnType(LineType exp){ return exp; } public
	 * static FillStyle convertReturnType(FillStyle exp){ return exp; }
	 */
}
