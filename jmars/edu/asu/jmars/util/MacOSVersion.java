package edu.asu.jmars.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Class to return the Mac OS version number in a manner compatible with Mac OS X version 11 and above
 * 
 * The solution was discovered by pwren and implemented from:
 * https://stackoverflow.com/questions/66026632/java-8-on-big-sur-reports-os-name-as-mac-os-x-and-os-version-as-10-16
 * 
 */
public class MacOSVersion {
	
	private static DebugLog log = DebugLog.instance();
	
    public static String getMacOsVersionNumber() {
        String result = "";

        List < String > command = new ArrayList<>();
        command.add("sw_vers");
        command.add("-productVersion");
        try (
                InputStream inputStream = new ProcessBuilder( command ).start().getInputStream() ;
                Scanner s = new Scanner( inputStream ).useDelimiter( "\\A" ) ;
        ) {
            result = s.hasNext() ? s.next() : "n/a";
            if (result == null) {
            	log.aprintln("Unable to determine Mac OS version number due to returned null value.");
            	return "n/a";
            }
        }
        catch (IOException e) {
        	log.aprintln("Unable to determine Mac OS version number due to IOException: "+e.getMessage());
        	return "n/a";
        }
        return result.trim();
    }
    
    public static String getMacOsName() {
        String result = "";

        List < String > command = new ArrayList<>();
        command.add("sw_vers");
        command.add("-productName");
        try (
                InputStream inputStream = new ProcessBuilder( command ).start().getInputStream() ;
                Scanner s = new Scanner( inputStream ).useDelimiter( "\\A" ) ;
        ) {
            result = s.hasNext() ? s.next() : "";
            if (result == null) {
            	log.aprintln("Unable to determine Mac OS name due to returned null value.");
            	return "n/a";
            }
        }
        catch (IOException e) {
        	log.aprintln("Unable to determine Mac OS name due to IOException: "+e.getMessage());
        	return "n/a";
        }
        return result.trim();
    }
        

        
}
