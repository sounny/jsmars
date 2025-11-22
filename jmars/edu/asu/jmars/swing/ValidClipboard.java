package edu.asu.jmars.swing;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

import edu.asu.jmars.util.DebugLog;


/**
 ** Helper class for dealing with platform-specific system clipboard issues.
 **/
public class ValidClipboard
{
    private static final DebugLog log = DebugLog.instance();

    /**
     ** Returns a {@link Clipboard} instance that is valid for the host
     ** platform and can be used to copy-and-paste data to/from an external
     ** application.
     ** <p>
     ** Starting with Java 1.4.2 (or perhaps any 1.4 version),
     ** there is a subtle platform-specific distinction between 
     ** a "system clipboard" and a "system selection".  An OS
     ** like Windows has a true system clipboard that exists
     ** globally.  Under Solaris or Linux, however, there is not
     ** truly a system clipboard, simply a "system selection"
     ** for sharing data between X applications.
     ** <p>
     ** Under Java 1.3.1 and perhaps other older Java versions,
     ** one could use the Clipboard instance returned by
     ** Toolkit.getSystemClipboard() to transfer data
     ** to external applications under Linux.  This behavior is
     ** now broken under Java 1.4.2, so we try use the "system
     ** selection" mechanism instead if available.  Otherwise,
     ** we default to the system clipboard and cross our fingers....
     ** <p>
     ** See the following relevant, if not directly on target,
     ** bug report:
     ** <p>
     ** http:**developer.java.sun.com/developer/bugParade/bugs/4496902.html
     ** <p>
     ** Also, the Toolkit.getSystemSelection() method does not exist
     ** before Java 1.4.
     ** <p>
     ** This two-solution approach has been tested on Linux (Fedora Core 1)
     ** and Windows 2000.
     **
     ** @return A system clipboard instance or equivalent; returns <code>null</code>
     ** if host is a headless system, has no GUI (in some cases), does not support a system
     ** clipboard of any kind, or there is a security access error.
     **
     ** @see Toolkit#getSystemClipboard
     ** @see Toolkit#getSystemSelection
     **/
    public static Clipboard getValidClipboard()
    {
	Toolkit toolkit = Toolkit.getDefaultToolkit();
	log.println("toolkit = " + toolkit);

	Clipboard clipboard = null;
	try {
	    try {
		clipboard = toolkit.getSystemSelection();
	    }
	    catch (NoSuchMethodError err) {
		// Occurs under Java 1.3 -- method does not exist yet. 
		log.println("old java -- no Toolkit.getSystemSelection() method");
	    }

	    if (clipboard == null) {
		clipboard = toolkit.getSystemClipboard();
		log.println("using system clipboard");
	    }
	    else
		log.println("using system selection");
	}
	catch (Exception e) {
	    log.aprintln("Could not obtain a system clipboard");
	    log.aprintln(e);
	}

	return clipboard;
    }
}
