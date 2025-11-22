package edu.asu.jmars.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

/***

	<p>The DebugLog class looks like PrintStream (though it doesn't
	actually inherit from it), and can be used to conditionally output
	debug messages. The criteria for determining whether something
	prints are described below. In addition, each line output through
	DebugLog is tagged with the class and function that output the
	message, and is optionally followed by a "hilight string."
	Finally, the class also supplies methods for outputting stack
	traces.

	<p><b>=== PRINT FUNCTIONS ===</b>

	<p>The print() and println() functions operate just like their
	counterparts in PrintStream, subject to the caveats above. The
	aprint() and aprintln() functions function similarly, except that
	they ALWAYS print, unconditionally. The printStack() and
	aprintStack() functions are used to print a stack trace
	conditionally or unconditionally, respectively.

	<p><b>=== MANUAL STATE FUNCTIONS ===</b>

	<p>The set() function can be passed true/false to manually set the
	state of a DebugLog object to active/inactive. The functions
	setActive() or setInactive() take no arguments and are convenience
	methods for doing the same thing as set(true) or set(false). The
	unset() function can be used after a manual state has been set, to
	return the object back to an "unset" state, where it again has no
	manual state. Note that state can also be set manually in the
	constructor.

	<p><b>=== ABOUT CONDITIONAL PRINTING ===</b>

	<p>A conditional print will or won't print, depending on whether
	we're in an active or inactive state. The state is determined by a
	combination of checks involving the DebugLog object itself and
	lookups of the print function's caller. The "caller function" is
	the function that called the print method, and the "caller class"
	is the class that function belongs to. Classes and functions can
	be registered as active or inactive, as described further
	below. Given all of this, active/inactive is determined for a
	particular print by:

	<ol>
	<li>If the DebugLog object has had its state set manually, use
	that, else...
	<li>If the caller function is registered, use that, else...
	<li>If the caller class is registered, use that, else...
	<li>If the caller class is covered by a registered mask, use that, else...
	<li>Use the "default" status.
	</ol>

	<p><b>=== REGISTERING FUNCTIONS, CLASSES, AND MASKS ===</b>

	<p>In code, you can register something by calling the static method
	DebugLog.set(thing, status). The "thing" is a string that can be a
	function ("OuterClass.InnerClass.functionName"), or a class
	("OuterClass.InnerClass"), or a mask ("OuterClass.*"). The
	"status" is a boolean, where true/false indicates
	active/inactive. In addition, the convenience methods
	setActive(thing) and setInactive(thing) are available.

	<p>The default status (see item 5 in "about conditional printing")
	can be changed in code by a call to setDefault(), which takes a
	boolean true/false to indicate active/inactive.

	<p>However, the preferred method of registering is not to do
	registrations in code. Instead, you can store the registrations in
	a file, which will be read at runtime. This way you don't need to
	recompile in order to change which debug messages are displayed
	and which get supressed. You can register the contents of a file
	with the static call DebugLog.readFile(filename).

	<p><b>=== FILE FORMAT ===</b>

	<p>A file can be read (and its contents registered) with the static
	function DebugLog.readFile(filename). Files are composed of
	entries, each of which must be separated by a newline. Each entry
	follows the syntax:

	<pre>[+|-][whitespace][name]</pre>

	<p>The +/- symbol determines whether we're registering something as
	active or inactive. If there is no symbol, then the
	last-encountered symbol is used instead. If a name is present, it
	can be any of the following:

	<ul>
	<li>DEFAULT, a keyword that indicates we're setting the default status
	<li>a classname-qualified function name
	<li>a classname
	<li>a dot-star classname mask (as in: "Class.*")
	</ul>

	<p>Lines can be commented with "#" or "//".

***/

public class DebugLog
 {
	// Static data used to hold registrations of functions and classes.
	static private Set activeItems = new HashSet();
	static private Set inactiveItems = new HashSet();
	static private Map colorItems = new HashMap();

	// The "default state"
	static private boolean defaultActive = false;

	// Show thread name on every printed line
	static private boolean showThreads = false;

	// Optimize DebugLog instances by disabling logs that should never
	// print.
	static private boolean optimize = true;

	// Disable the stack format warning
	static private boolean showParseErrors = true;

	// Show cheesy colorized output (using ANSI escape codes)
	static private boolean showColors = false;

	static private final String ESC = "\033";

	static
	 {
		readFile(".debugrc");
	 }

	// ANSI color codes
	final private class Ansi
	 {
		final String value;
		Ansi(String codes)
		 {
			if(codes == null)
				value = "";
			else
				value = ESC + codes;
		 }
		public String toString()
		 {
			return  showColors ? value : "";
		 }
	 }

	// Basic trivial codes
	protected final Ansi NONE    = new Ansi(null);
	public final Ansi RESET   = new Ansi("[m");
	// Low-level particular colors
	private final Ansi UNDER   = new Ansi("[4m");
	// High-level color codes... for particular types of text
	public final Ansi APRINT  = new Ansi("[1m");
	private final Ansi CALLER  = new Ansi("[32m");
	private final Ansi THREAD  = new Ansi("[36m");
	private final Ansi STACK   = new Ansi("[31m");
	protected final Ansi EXCEPT  = new Ansi("[31m");
	protected final Ansi AEXCEPT = new Ansi("[1;31m");
	/**
	 ** When converted to a string, turns into (if enabled) the ANSI
	 ** code for dark text. You can include it in any println's to
	 ** display dark text programmatically. It is non-static for
	 ** implementation reasons.
	 **/
	public final Object DARK = new Ansi("[1;30m");

	private String color;
	protected void uncolor()
	 {
		pout.print(RESET);
	 }

	// Private utility function to cut off single-line comments
	static private String trimSingleLineComments(String line)
	 {
		int start = line.indexOf('#');
		if(start != -1)
			line = line.substring(0, start);

		start = line.indexOf("//");
		if(start != -1)
			line = line.substring(0, start);

		return  line;
	 }

	// Operates just like String.indexOf, finding needle in hay at
	// some point after start. BUT, if needle isn't found, we return
	// hay.length() INSTEAD of -1.
	private static int indexOf(String hay, String needle, int start)
	 {
		int found = hay.indexOf(needle, start);
		if(found == -1)
			return  hay.length();
		else
			return  found;
	 }

	// See comments at top of this file for file format.
	static public void readFile(String filename)
	 {
		try
		 {
			BufferedReader in = new BufferedReader(new FileReader(filename));

			String rawLine;
			boolean whatToDo = true; // true: active, false: inactive
			boolean inLongComment = false; // in a multi-line comment?
		 NEXT_LINE:
			while((rawLine = in.readLine()) != null)
			 {
				int rawLineLength = rawLine.length();
				StringBuffer buff = new StringBuffer(rawLineLength);
				int i = 0;
			 REMOVE_COMMENTS:
				while(i < rawLineLength)
					if(inLongComment)
					 {
						int end = rawLine.indexOf("*/", i);
						if(end == -1)
							continue NEXT_LINE;
						inLongComment = false;
						i = end + 2;
					 }
					else
					 {
						int c   = indexOf(rawLine, "/*", i);
						int sh  = indexOf(rawLine, "#",  i);
						int cpp = indexOf(rawLine, "//", i);

						int min = Math.min(c, Math.min(sh, cpp));

						// We have no more comments in the line
						if(min == rawLineLength)
						 {
							buff.append(rawLine.substring(i));
							break REMOVE_COMMENTS;
						 }

						// We start a C comment in the line
						else if(min == c)
							inLongComment = true;

						// We start a cpp or sh comment in the line
						else
						 {
							buff.append(rawLine.substring(i, min));
							break REMOVE_COMMENTS;
						 }

						buff.append(rawLine.substring(i, min));
						i = min + 2;
					 }

				// A tokenizer that contains the current line, sans comments.
				StringTokenizer tokLine = new StringTokenizer(buff.toString());

				// Empty lines (or nothing-but-comment lines)
				if(!tokLine.hasMoreTokens())
					continue;

				// Part 0: the color code
				String part0 = tokLine.nextToken();
				String part1;
				String c = null;
				if(part0.length() == 1  &&  tokLine.hasMoreTokens())
				 {
					String colorChars = "DRGYBMCW";
					int colorNum = colorChars.indexOf(part0.toUpperCase());
					if(colorNum != -1)
						c = ESC + "[1;3" + colorNum + "m";
					else
						System.out.println(
							"\n====================================" +
							"\nIllegal color character: " + part0 +
							"\nIn file: " + filename +
							"\n====================================");
					part1 = tokLine.nextToken();
				 }
				else
					part1 = part0;

				// Part 1: the +/- sign
				boolean noPart1 = false;
				if(part1.charAt(0) == '-')
					whatToDo = false;
				else if(part1.charAt(0) == '+')
					whatToDo = true;
				else
					// There's no part 1... use the last one we found
					noPart1 = true;

				// Part 2: the class/function name
				String part2;
				if(noPart1)
					part2 = part1;
				else if(part1.length() > 1)
					part2 = part1.substring(1);
				else if(tokLine.hasMoreTokens())
					part2 = tokLine.nextToken();
				else
					// There's no part 2... we only set whatToDo for the future
					continue;

				// If there's anything else on the line, something's wrong
				if(tokLine.hasMoreTokens())
					System.out.println("Problem parsing \"" + rawLine +
									   "\" in " + filename);

				// Register the specified symbol, or change the default state.
				if     (part2.equals("DEFAULT")) defaultActive = whatToDo;
				else if(part2.equals("THREADS"))   showThreads = whatToDo;
				else if(part2.equals("COLORS"))     showColors = whatToDo;
				else if(part2.equals("OPTIMIZE"))     optimize = whatToDo;
				else if(part2.equals("PARSE_ERRORS")) showParseErrors=whatToDo;
				else
				 {
					if(whatToDo)
					 {
						if(c != null)
							setColor(part2, c);
						setActive(part2);
					 }
					else
						setInactive(part2);
				 }
			 }
		 }
		catch(FileNotFoundException e)
		 {
//			System.out.println("Unable to open " + filename);
		 }
		catch(IOException e)
		 {
			System.out.println("I/O error reading " + filename);
		 }
	 }

	// If custom is true, then the object has a manual state set.
	private boolean custom;

	// The manual state, if we have one.
	private boolean active;

	// The hilight to put at the end of every printed line.
	protected String hilight;

	// Holds the caller function. This can't be passed thru the call
	// chain of print()s, so we hold it as a member variable. It's a
	// thread-local to keep this method thread-safe. All accesses to
	// theCaller ar performed in getCaller() and setCaller().
	private static final ThreadLocal theCaller = new ThreadLocal();

	// The "actual" PrintStream being used to output data.
	private static PrintStream pout = System.err;
	
	public static synchronized PrintStream getOutputStream() {
		return pout;
	}
	
	/**
	 ** Changes the stream that's ultimately used to output messages
	 ** from DebugLog. Recommended for use exclusively by test
	 ** harnesses. By default, the initial stream used is {@link
	 ** System#err}. If set to <code>null</code>, output is disabled
	 ** altogether -- <b>use this with extreme caution, as all
	 ** application errors will be silent</b>.
	 **/
    public static synchronized void setOutputStream(OutputStream out)
	 {
		if(out == null)
			pout = new PrintStream(new NullOutputStream());

		else if(out instanceof PrintStream)
			pout = (PrintStream) out;

		else
			pout = new PrintStream(out);
	 }

	/**
	 ** A {@link OutputStream} that goes nowhere.
	 **/
	private static class NullOutputStream extends OutputStream
	 {
		public void write(byte[] b, int off, int len) { }
		public void write(byte[] b) { }
		public void write(int b) { }
	 }

	// Tracks state of output... are we on a fresh line?
	private boolean newLine = true;

	// Inner class used as a filter to output in the format we want.
	private class DebugStream extends OutputStream
	 {
		final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

		public void close()
		 {
			pout.close();
		 }

		public void flush()
		 {
			pout.flush();
		 }

		public void write(byte[] b)
		 throws IOException
		 {
			write(b, 0, b.length);
		 }

		// Output the given string, printing the caller function
		// whenever we start a new line, and putting the hilight in if
		// we have one.
		public void write(byte[] b, int beg, int len)
		 throws IOException
		 {
			final int end = beg + len;

			// This loop is executed n+1 times, where n is the number
			// of newlines in b
			while(beg < end)
			 {
				// If we're on a fresh line, output the caller
				if(newLine)
				 {
					String timeStamp = dateFormatter.format(new Date());
					pout.print("" + color + timeStamp + " "+ RESET);
					
					if(showThreads)
						pout.print("" + color + THREAD + '<' +
								   Thread.currentThread().getName() +
								   '>' + RESET + '\t');
					String cf = getCaller();
//					try
//					 {
//						int i = cf.lastIndexOf('.') + 1;
//						String c = cf.substring(0, i);
//						String f = cf.substring(i);
//						cf = c + UNDER + f;
//					 }
//					catch(Throwable e)
//					 {
//					 }
					pout.print("" + color + CALLER + cf + RESET);
					pout.print('\t');

					// If there's an active hilight, dump it
					if(hilight.length() != 0)
						pout.print(hilight);

					newLine = false;
				 }
				pout.print(color);

				// Find the next eol character
				int eol = beg;
				while(b[eol] != '\n'  &&  b[eol] != '\r')
					if(++eol == end)
					 {
						// If no eol character is found, just dump everything
						pout.write(b, beg, end-beg);
						return;
					 }

				// Output up to (but excluding) the eol character we found
				if (eol > beg)
					pout.write(b, beg, eol - beg);

				// Finally output our newline. Set the flag for a fresh line
				newLine = true;
				pout.print((char)b[eol]);

				// Start the next loop just past the newline character
				beg = eol + 1;
			 }
		 }

		// Output the given byte, printing the caller function
		// if we started a new line, and putting the hilight in if
		// we have one and the byte was a newline.
		public void write(int b)
		 throws IOException
		 {
			// If we're on a fresh line, output the caller
			if(newLine)
			 {
				pout.print("" + color + CALLER + getCaller() + RESET);
				pout.print('\t');

				// Output any hilight
				if(hilight.length() != 0)
					pout.print(hilight);

				newLine = false;
			 }

			// Set the flag to indicate a fresh line
			if(b == '\n'  ||  b == '\r')
				newLine = true;

			// Finally... output the requested charater
			pout.print(color);
			pout.write(b);
		 }
	 }

	/***************** CONSTRUCTORS *****************/

	// Holds the internal print stream we use to format output, which
	// uses a DebugStream as its output.
	protected PrintStream realPS = new PrintStream(new DebugStream());

	// The "real" constructor. All the others dish out to this one
	// (which is private).
	private DebugLog(boolean _custom, boolean _active, String _hilight)
	 {
		custom = _custom;
		active = _active;
		hilight = _hilight;
	 }

	private DebugLog()
	 {
		this(false, false, "");
	 }

	private DebugLog(boolean active)
	 {
		this(true, active, "");
	 }

	private DebugLog(String hilight)
	 {
		this(false, false, hilight);
	 }

	private DebugLog(boolean active, String hilight)
	 {
		this(true, active, hilight);
	 }

	public static DebugLog instance(boolean enabled)
	 {
		return  enabled ? new DebugLog(true) : disabledLog;
	 }

	/**
	 ** The global instance of DebugLog used for all disabled logs.
	 **/
	private static final DebugLog disabledLog = new DisabledLog();

	/**
	 ** Should be called in lieu of a constructor, in order to
	 ** retrieve a DebugLog instance appropriate for the caller's
	 ** context. Returns either a "normal" DebugLog or a "disabled"
	 ** one, depending on whether anything is enabled in the .debugrc
	 ** file for the caller. This optimization can be disabled by
	 ** including "-OPTIMIZE" in the .debugrc file.
	 **/
	public static DebugLog instance()
	 {
		if(!optimize  ||  defaultActive)
			return  new DebugLog();

		DebugLog fakeLog = new DebugLog();
		fakeLog.setMode(1, null);
		String caller = fakeLog.getCaller();
		String callerTopClass = caller.substring(0, caller.indexOf("."));

		for(Iterator i=activeItems.iterator(); i.hasNext(); )
		 {
			String entry = (String) i.next();
			if(entry.startsWith(callerTopClass+".")  ||
			   entry.equals    (callerTopClass) )
			 {
				log.aprintln("ACTIVATED: " +
								   callerTopClass);
				return  new DebugLog();
			 }
		 }

		return  disabledLog;
	 }

	public static DebugLog instance(String prefixMsg)
	 {
		return  new PrefixedLog(prefixMsg);
	 }

	/***************** STATE-SETTING FUNCTIONS *****************/

	public void set(boolean newActive)
	 {
		custom = true;
		active = newActive;
	 }

	public void unset()
	 {
		custom = false;
	 }

	public void setActive()
	 {
		set(true);
	 }

	public void setInactive()
	 {
		set(false);
	 }

	/**
	 ** Externally-accessible function used to determine whether or
	 ** not a given printing is currently activated (i.e. whether or
	 ** not a println() will succeed).
	 **/
    public boolean isActive()
	 {
		setMode(1, NONE);
		return  getActive();
	 }

	/**
	 ** Internal function used to determine whether or not conditional
	 ** prints should succeed.
	 **/
	boolean getActive()
	 {
		if(custom)
			return  active;
		else
			return  getStaticActive(getCaller());
	 }

	// Internal function used to get a stack trace. The depth
	// argument specifies how deep the call to getStack() is... this
	// is used to determine how much of the stack trace to ignore as
	// internal calls. The frameCount argument is how many levels of
	// stack to return. A frame count of 0 returns the original caller
	// only, with increasing frame counts returning one more stack
	// level. A frameCount of -1 is equivalent to a frameCount of
	// infinity.
	static private StackTraceElement[] getStack(int depth, int frameCount)
	 {
		++depth;
		StackTraceElement[] stack = new Throwable().getStackTrace();

		if(frameCount == -1)
			frameCount = stack.length - depth;
		else
		 {
			++frameCount;
			if(frameCount + depth > stack.length)
				frameCount = stack.length - depth;
		 }

		if(frameCount < 0)
		 {
			pout.println(">>>> TELL MICHAEL: UNABLE TO RETRIEVE STACK! <<<<");
			pout.println(Arrays.asList(stack));
			return  new StackTraceElement[0];
		 }

		StackTraceElement[] slice = new StackTraceElement[frameCount];
		System.arraycopy(stack, depth, slice, 0, slice.length);
		return  slice;
	 }

	// Utility function to retrieve the ThreadLocal caller.
	private String getCaller()
	 {
		return  (String) theCaller.get();
	 }

	// Sets the current caller. The caller is stored as a member
	// variable because it can't be passed down the call chain in
	// print() functions. The depth parameter indicates how far the
	// call to setCaller() is nested, so that it can ignore internal
	// function calls and retrieve the original caller function.
	//
	// Now also sets the Ansi color mode (if active).
	protected void setMode(int depth, Ansi color)
	 {
		// Jump thru some hoops to get a stack trace
		StackTraceElement[] stack = new Throwable().getStackTrace();

		String caller;
		if(stack.length > depth+1)
		 {
			caller = stack[depth+1].getClassName() + "." + stack[depth+1].getMethodName();
			caller =
				caller.substring(
					1 + caller.lastIndexOf('.', caller.lastIndexOf('.')-1))
				.replace('$', '.');
		 }
		else
		 {
			if(showParseErrors)
			 {
				pout.println(">>>>> TELL MICHAEL: " +
							 "STACK PROCESSING PROBLEM IN DebugLog <<<<<");
				pout.println("(Conditional DebugLog output will be disabled)");
				pout.println("depth = " + depth);
				pout.println("RAW STACK = ");
				pout.println(Arrays.asList(stack));
				showParseErrors = false;
			 }
			caller = "???.???";
		 }

		// Is this some type of constructor or static initializer?
		// Then we need to correct it to read ".Classname" instead of
		// ".<init>" or ".<clinit>".
		int startOfSpecialFunc = caller.indexOf(".<");
		if(startOfSpecialFunc != -1)
		 {
			// Get the whole class name (excludes the dot and the
			// function, includes outer classnames)
			String fullClass = caller.substring(0, startOfSpecialFunc);

			// Get the name of the last class in the dotted chain
			int startOfLastClass = fullClass.lastIndexOf(".")+1;
			String lastClass = fullClass.substring(startOfLastClass);

			// Change the caller to reflect the constructor of the
			// last class in the chain
			caller = fullClass + "." + lastClass;
		 }

		// Finally, sets the ThreadLocal to the caller we found.
		theCaller.set(caller);

		// And finally finally, set the color state
		if(showColors  &&  color != null)
		 {
			String c = getStaticColor(caller);
			if(c == null)
				this.color = color.toString();
			else
				this.color = c;
		 }
		else
			this.color = "";
	 }


	// Internal utility method to perform the logic needed to
	// determine whether a given caller is registered as active or
	// inactive. See the comments at the top of this file for a
	// description of the algorithm.
	static synchronized private boolean getStaticActive(String caller)
	 {
		if(activeItems.contains(caller))
			return  true;
		if(inactiveItems.contains(caller))
			return  false;

		String callerClass = caller.substring(0, caller.lastIndexOf("."));

		if(activeItems.contains(callerClass))
			return  true;
		if(inactiveItems.contains(callerClass))
			return  false;

		int dot = callerClass.length();
		do
		 {
			String mask = callerClass.substring(0,dot).concat(".*");
			if(activeItems.contains(mask))
				return  true;
			if(inactiveItems.contains(mask))
				return  false;
		 }
		while((dot = callerClass.lastIndexOf(".", dot-1)) != -1);

		return  defaultActive;
	 }

	// Same thing as getStaticActive, but for colors instead of state.
	static synchronized private String getStaticColor(String caller)
	 {
		String c = null;

		c = (String) colorItems.get(caller);
		if(c != null)
			return  c;

		String callerClass = caller.substring(0, caller.lastIndexOf("."));
		c = (String) colorItems.get(callerClass);
		if(c != null)
			return  c;

		int dot = callerClass.length();
		do
		 {
			String mask = callerClass.substring(0,dot).concat(".*");
			c = (String) colorItems.get(mask);
			if(c != null)
				return  c;
		 }
		while((dot = callerClass.lastIndexOf(".", dot-1)) != -1);

		return  null;
	 }

	/***************** REGISTRATION FUNCTIONS *****************/

	// Registers something as active or inactive, depending on
	// newActive. Takes care of un-registering something if it was
	// previously registered.
	static synchronized public void set(String what,
										boolean newActive)
	 {
		unset(what);
		(newActive ? activeItems : inactiveItems).add(what);
	 }

	// Unregisters something previously registered. Has no effect if
	// the supplied item was never registered.
	static synchronized public void unset(String what)
	 {
		activeItems.remove(what);
		inactiveItems.remove(what);
	 }

	// Convenience method for set(what, true).
	static synchronized public void setActive(String what)
	 {
		set(what, true);
	 }

	// Convenience method for set(what, false).
	static synchronized public void setInactive(String what)
	 {
		set(what, false);
	 }

	// Use true/false to set the default status to active/inactive
	static public void setDefault(boolean _defaultActive)
	 {
		defaultActive = _defaultActive;
	 }

	// Sets the ANSI color code for something
	static private void setColor(String what, String c)
	 {
		colorItems.put(what, c);
	 }

	/*********** Testing stuff ***********/

	private static DebugLog log = new DebugLog();

	static
	 {
		log.println("static init of DebugLog");
	 }

	private static class Foobar
	 {
		static
		 {
			log.println("static init of Foobar");
		 }

		Foobar()
		 {
			log.println("in constructor");
		 }

		void foo()
		 {
			log.println("foo start");
			bar();
			log.println("foo end");
		 }

		void bar()
		 {
			log.println("in bar");
			log.printStack(-1);
		 }
	 }

	public static void main(String av[])
	 {
		DebugLog.readFile(".debugrc");
		Foobar fb = new Foobar();
//		log.aprintStack(1);
		fb.foo();
		fb.bar();
	 }

	/********** THREAD-PRINTING ***********/
	public synchronized void printThread()
	 {
		setMode(1, NONE);
		if(getActive())
			realPS.println(Thread.currentThread().getName());
	 }

	public synchronized void aprintThread()
	 {
		setMode(1, APRINT);
		realPS.println(Thread.currentThread().getName());
	 }

	/********** STACK-PRINTING ***********/

	// Conditionally prints a stack trace, to a given depth.
	public synchronized void printStack(int frameCount)
	 {
		setMode(1, NONE);
		if(getActive())
			realPrintStack(getStack(1, frameCount));
	 }
	// Unconditionally prints a stack trace, to a given depth.
	public synchronized void aprintStack(int frameCount)
	 {
		setMode(1, APRINT);
		realPrintStack(getStack(1, frameCount));
	 }

	// The real stack-trace workhorse.
	private void realPrintStack(StackTraceElement[] stack)
	 {
		for(int i=0; i<stack.length; i++)
			pout.println("" + color + THREAD + '<' +
						 Thread.currentThread().getName() +
						 '>' + RESET + '\t' + STACK + color +
						 formatStackLine(i==0 ? 'o' : '|', stack[i]) + RESET
				);
	 }

	// Used by realPrintStack
	private String formatStackLine(char bullet, StackTraceElement stack)
	 {
		int lineNum = stack.getLineNumber();

		return  (lineNum<0 ? ":???" : ":"+lineNum) + '\t' + bullet + ' ' +
			stack.getClassName().replace('$','.') + '.' +
			stack.getMethodName() + hilight;
	 }

	protected void printException(Throwable e)
	 {
//		realPS.println(e);

		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		realPS.print(sw.toString());
	 }

	/***********************************************************
	 ** NOTE: The following are proxying implementations of the
	 ** PrintStream.print() routines, as well as the new aprint
	 ** routines. They're all identical, we just need a bunch of
	 ** different versions of each function to handle different
	 ** argument types.
	 ***********************************************************/
	
	//prepend timestamp to messages
	private void prependTS() {
//		realPS.print(new Timestamp(new Date().getTime()).toString()+": ");
	}
	
	/********** CONDITIONAL PRINT ***********/

	public void print(boolean what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.print(what); uncolor(); } }
	public void print(   char what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.print(what); uncolor(); } }
	public void print( char[] what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.print(what); uncolor(); } }
	public void print( double what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.print(what); uncolor(); } }
	public void print(  float what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.print(what); uncolor(); } }
	public void print(    int what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.print(what); uncolor(); } }
	public void print(   long what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.print(what); uncolor(); } }
	public void print( Object what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.print(what); uncolor(); } }
	public void print( String what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.print(what); uncolor(); } }
	public void print(Throwable  e) { setMode(1, EXCEPT); if(getActive()) { prependTS(); printException(e); uncolor(); } }

	/********** CONDITIONAL PRINT WITH ENDLINE ***********/

	public void println(            ) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.println(""); uncolor(); } }
	public void println(boolean what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.println(what); uncolor(); } }
	public void println(   char what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.println(what); uncolor(); } }
	public void println( char[] what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.println(what); uncolor(); } }
	public void println( double what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.println(what); uncolor(); } }
	public void println(  float what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.println(what); uncolor(); } }
	public void println(    int what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.println(what); uncolor(); } }
	public void println(   long what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.println(what); uncolor(); } }
	public void println( Object what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.println(what); uncolor(); } }
	public void println( String what) { setMode(1, NONE); if(getActive()) { prependTS(); realPS.println(what); uncolor(); } }
	public void println(Throwable  e) { setMode(1, EXCEPT); if(getActive()) { prependTS(); printException(e); uncolor(); } }

	/********** UNCONDITIONAL PRINT ***********/

	public void aprint(boolean what) { setMode(1, APRINT); prependTS(); realPS.print(what); uncolor(); }
	public void aprint(   char what) { setMode(1, APRINT); prependTS();realPS.print(what); uncolor(); }
	public void aprint( char[] what) { setMode(1, APRINT); prependTS(); realPS.print(what); uncolor(); }
	public void aprint( double what) { setMode(1, APRINT); prependTS(); realPS.print(what); uncolor(); }
	public void aprint(  float what) { setMode(1, APRINT); prependTS(); realPS.print(what); uncolor(); }
	public void aprint(    int what) { setMode(1, APRINT); prependTS(); realPS.print(what); uncolor(); }
	public void aprint(   long what) { setMode(1, APRINT); prependTS(); realPS.print(what); uncolor(); }
	public void aprint( Object what) { setMode(1, APRINT); prependTS(); realPS.print(what); uncolor(); }
	public void aprint( String what) { setMode(1, APRINT); prependTS(); realPS.print(what); uncolor(); }
	public void aprint(Throwable e ) { setMode(1, AEXCEPT); prependTS(); printException(e); uncolor(); }

	/********** UNCONDITIONAL PRINT WITH ENDLINE ***********/

	public void aprintln(            ) { setMode(1, APRINT); prependTS(); realPS.println(""); uncolor(); }
	public void aprintln(boolean what) { setMode(1, APRINT); prependTS(); realPS.println(what); uncolor(); }
	public void aprintln(   char what) { setMode(1, APRINT); prependTS(); realPS.println(what); uncolor(); }
	public void aprintln( char[] what) { setMode(1, APRINT); prependTS(); realPS.println(what); uncolor(); }
	public void aprintln( double what) { setMode(1, APRINT); prependTS(); realPS.println(what); uncolor(); }
	public void aprintln(  float what) { setMode(1, APRINT); prependTS(); realPS.println(what); uncolor(); }
	public void aprintln(    int what) { setMode(1, APRINT); prependTS(); realPS.println(what); uncolor(); }
	public void aprintln(   long what) { setMode(1, APRINT); prependTS(); realPS.println(what); uncolor(); }
	public void aprintln( Object what) { setMode(1, APRINT); prependTS(); realPS.println(what); uncolor(); }
	public void aprintln( String what) { setMode(1, APRINT); prependTS(); realPS.println(what); uncolor(); }
	public void aprintln(Throwable e ) { setMode(1, AEXCEPT); prependTS(); printException(e); uncolor(); }

	/**
	 ** Optimized version of DebugLog for which conditional prints
	 ** never succeed.
	 **/
	private static final class DisabledLog extends DebugLog
	 {
		/********** CONDITIONAL PRINT ***********/

		public void print(boolean what) { }
		public void print(   char what) { }
		public void print( char[] what) { }
		public void print( double what) { }
		public void print(  float what) { }
		public void print(    int what) { }
		public void print(   long what) { }
		public void print( Object what) { }
		public void print( String what) { }
		public void print(Throwable  e) { }

		/********** CONDITIONAL PRINT WITH ENDLINE ***********/

		public void println(            ) { }
		public void println(boolean what) { }
		public void println(   char what) { }
		public void println( char[] what) { }
		public void println( double what) { }
		public void println(  float what) { }
		public void println(    int what) { }
		public void println(   long what) { }
		public void println( Object what) { }
		public void println( String what) { }
		public void println(Throwable  e) { }

		public void printStack(int n) { }
		public void printThread() { }
	 }

    /**
     ** Specialized version of DebugLog that spits out an initial
     ** message line just before the first print invocation. Used (for
     ** example) to precede all following messages with a filename or
     ** something, skipping the filename if no message is ever
     ** printed. Also indents every message line with an extra tab
     ** (but not the initial line).
     **/
    private static final class PrefixedLog extends DebugLog
     {
		String prefixMsg;
		boolean prefixNeeded = true;

		PrefixedLog(String prefixMsg)
		 {
			this.prefixMsg = prefixMsg;
		 }

		private void prefix()
		 {
			if(prefixNeeded)
			 {
				realPS.println(prefixMsg);
				prefixNeeded = false;
				this.hilight = "\t";
			 }
		 }

		/********** CONDITIONAL PRINT ***********/

		public void print(boolean what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.print(what); uncolor(); } }
		public void print(   char what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.print(what); uncolor(); } }
		public void print( char[] what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.print(what); uncolor(); } }
		public void print( double what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.print(what); uncolor(); } }
		public void print(  float what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.print(what); uncolor(); } }
		public void print(    int what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.print(what); uncolor(); } }
		public void print(   long what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.print(what); uncolor(); } }
		public void print( Object what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.print(what); uncolor(); } }
		public void print( String what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.print(what); uncolor(); } }
		public void print(Throwable  e) { setMode(1, EXCEPT); if(getActive()) { prefix(); printException(e); uncolor(); } }

		/********** CONDITIONAL PRINT WITH ENDLINE ***********/

		public void println(            ) { setMode(1, NONE); if(getActive()) { prefix(); realPS.println(""); uncolor(); } }
		public void println(boolean what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.println(what); uncolor(); } }
		public void println(   char what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.println(what); uncolor(); } }
		public void println( char[] what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.println(what); uncolor(); } }
		public void println( double what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.println(what); uncolor(); } }
		public void println(  float what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.println(what); uncolor(); } }
		public void println(    int what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.println(what); uncolor(); } }
		public void println(   long what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.println(what); uncolor(); } }
		public void println( Object what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.println(what); uncolor(); } }
		public void println( String what) { setMode(1, NONE); if(getActive()) { prefix(); realPS.println(what); uncolor(); } }
		public void println(Throwable  e) { setMode(1, EXCEPT); if(getActive()) { prefix(); printException(e); uncolor(); } }

		/********** UNCONDITIONAL PRINT ***********/

		public void aprint(boolean what) { setMode(1, APRINT); prefix(); realPS.print(what); uncolor(); }
		public void aprint(   char what) { setMode(1, APRINT); prefix(); realPS.print(what); uncolor(); }
		public void aprint( char[] what) { setMode(1, APRINT); prefix(); realPS.print(what); uncolor(); }
		public void aprint( double what) { setMode(1, APRINT); prefix(); realPS.print(what); uncolor(); }
		public void aprint(  float what) { setMode(1, APRINT); prefix(); realPS.print(what); uncolor(); }
		public void aprint(    int what) { setMode(1, APRINT); prefix(); realPS.print(what); uncolor(); }
		public void aprint(   long what) { setMode(1, APRINT); prefix(); realPS.print(what); uncolor(); }
		public void aprint( Object what) { setMode(1, APRINT); prefix(); realPS.print(what); uncolor(); }
		public void aprint( String what) { setMode(1, APRINT); prefix(); realPS.print(what); uncolor(); }
		public void aprint(Throwable e ) { setMode(1, AEXCEPT); prefix(); printException(e); uncolor(); }

		/********** UNCONDITIONAL PRINT WITH ENDLINE ***********/

		public void aprintln(            ) { setMode(1, APRINT); prefix(); realPS.println(""); uncolor(); }
		public void aprintln(boolean what) { setMode(1, APRINT); prefix(); realPS.println(what); uncolor(); }
		public void aprintln(   char what) { setMode(1, APRINT); prefix(); realPS.println(what); uncolor(); }
		public void aprintln( char[] what) { setMode(1, APRINT); prefix(); realPS.println(what); uncolor(); }
		public void aprintln( double what) { setMode(1, APRINT); prefix(); realPS.println(what); uncolor(); }
		public void aprintln(  float what) { setMode(1, APRINT); prefix(); realPS.println(what); uncolor(); }
		public void aprintln(    int what) { setMode(1, APRINT); prefix(); realPS.println(what); uncolor(); }
		public void aprintln(   long what) { setMode(1, APRINT); prefix(); realPS.println(what); uncolor(); }
		public void aprintln( Object what) { setMode(1, APRINT); prefix(); realPS.println(what); uncolor(); }
		public void aprintln( String what) { setMode(1, APRINT); prefix(); realPS.println(what); uncolor(); }
		public void aprintln(Throwable e ) { setMode(1, AEXCEPT); prefix(); printException(e); uncolor(); }
     }
 }
