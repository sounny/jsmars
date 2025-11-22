package edu.asu.jmars.swing;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.*;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.text.Document;

/**
 ** A textfield whose contents will be a time string. Use {@link
 ** #getEt} to extract the et value of the text field, regardless of
 ** what format the user entered their text in.
 **
 ** <p>The following time formats are assumed/understood from the
 ** given BNF, where runs of '#' represent digits, runs of '_' denote
 ** alphabetical characters, square brackets denote optional text, and
 ** everything else is literal. All formats may be optionally preceded
 ** with an instrument, as in "ODY:". In the absence of an explicit
 ** instrument, the current "default instrument" is assumed.
 **
 ** <pre>
 ** <b>ET</b>
 ** [et:]########
 ** 
 ** <b>SCLK</b> <i>(For SCLK values, the epoch is parsed but ignored)</i>
 ** #/########
 ** [#/]########/
 ** sclk:[#/]########[/]
 **
 **
 ** <b>ORBIT</b>
 ** #####+[[#:[##:]]##]
 ** orbit:#####[+[[#:[##:]]##]]
 **
 ** <b>UTC</b>
 ** [utc:]####-###T##:##:##      <i>(year and day-of-year syntax)</i>
 ** [utc:]####-### // ##:##:##   <i>(year and day-of-year syntax)</i>
 ** [utc:]####/##/##-##:##:##    <i>(year/month/day syntax)</i>
 ** [utc:]#### ___ ## ##:##:##   <i>(textual month syntax)</i>
 ** </pre>
 **
 ** <p>Technically speaking, the actual grammar is slightly looser
 ** than the BNF above; for brevity, only the important distinctions
 ** were enumerated. In all cases, seconds values may contain decimals.
 ** All literals are case-insensitive.
 **/
public class TimeField
 extends PasteField
 {
	private static DebugLog log = DebugLog.instance();

	public TimeField()
	 {
		super();
		addMouseListener(new MyMouse());
	 }

	public TimeField(Document doc, String text, int columns)
	 {
		super(doc, text, columns);
		addMouseListener(new MyMouse());
	 }

	public TimeField(int columns)
	 {
		super(columns);
		addMouseListener(new MyMouse());
	 }

	public TimeField(String text)
	 {
		super(text);
		addMouseListener(new MyMouse());
	 }

	public TimeField(String text, int columns)
	 {
		super(text, columns);
		addMouseListener(new MyMouse());
	 }

	/**
	 ** Regardless of the input format, parses the text in this
	 ** JTextField and returns it as an ET value.
	 **/
	public double getEt()
	 throws TimeException
	 {
		return  parseTimeToEt(getText());
	 }

	/**
	 ** Sets the text of the control to the default formatting of the
	 ** given ET value.
	 **/
	public void setEt(double et)
	 throws TimeException
	 {
		setText(etToDefault(et));
	 }
	
	/**
	 * Determine the spacecraft from the given time string.
	 * @param txt
	 * @return
	 */
	public static String getCraft(String txt){
		txt = txt.trim().toUpperCase();
		
		String craft = null;
		for(String craftTag: TimeCacheFactory.instance().getCraftIdMap().keySet()){
			if(txt.startsWith(craftTag + ":")){
				craft = craftTag;
				break;
			}
		}

		return craft;
	}

	/**
	 ** Given a time in one of the accepted formats described at the
	 ** top of this page, parses it and/or converts it to ET.
	 **/
	public static double parseTimeToEt(String txt)
	 throws TimeException
	 {
		txt = txt.trim().toUpperCase();

		//// Explicit craft prefix

		log.println("Taking craft from " + txt);

		String craft = getCraft(txt);
		if (craft != null){
			txt = txt.substring(craft.length() + 1);
		}

		log.println("Found craft " + craft);

		//// Explicit time prefix

		log.println("Taking prefix from " + txt);

		if(txt.startsWith("ET:"   ))
			return  etFromEt   ( txt.substring("ET:"   .length())        );

		if(txt.startsWith("SCLK:" ))
			return  etFromSclk ( txt.substring("SCLK:" .length()), craft );

		if(txt.startsWith("ORBIT:"))
			return  etFromOrbit( txt.substring("ORBIT:".length()), craft );

		if(txt.startsWith("UTC:"  ))
			return  etFromUtc  ( txt.substring("UTC:"  .length()), craft );

		//// Infer from separator characters (ORDER OF CONDITIONS MATTER!)

		log.println("Inferring from " + txt);

		if(txt.indexOf('+') != -1) return  etFromOrbit( txt, craft );
		if(txt.indexOf(':') != -1) return  etFromUtc  ( txt, craft );
		if(txt.indexOf('/') != -1) return  etFromSclk ( txt, craft );
		/* Assume ET by default */ return  etFromEt   ( txt        );
	 }

	/**
	 * See if the given time string is parsable via the {@link #parseTimeToEt(String)}
	 * method. If so, return <code>true</code> otherwise, return <code>false</code>.
	 */
	public static boolean parsable(String timeString){
		try {
			parseTimeToEt(timeString);
			return true;
		}
		catch(TimeException ex){
			return false;
		}
	}
	
	public static class TO_ET
	 {
		public static void main(String[] av)
		 throws Throwable
		 {
			log.aprintln(parseTimeToEt(av[0]));
			System.exit(0);
		 }
	 }

	/**
	 ** Given a time in one of the accepted formats described at the
	 ** top of this page, parses it and returns a canonical format
	 ** string appropriate for use as a format prefix. Returns null
	 ** for an empty string input.
	 **/
	public static String parseTimeToFormat(String txt)
	 {
		txt = txt.trim().toUpperCase();

		//// Explicit craft prefix

		String craft = getCraft(txt);
		if (craft != null){
			txt = txt.substring(craft.length() + 1);
		}

		//// Explicit time prefix

		if(txt.startsWith("ET:"   )) return  "et:";
		if(txt.startsWith("SCLK:" )) return  craft + ":sclk:";
		if(txt.startsWith("ORBIT:")) return  craft + ":orbit:";
		if(txt.startsWith("UTC:"  )) return  "utc:";

		//// Infer from separator characters (ORDER OF CONDITIONS MATTER!)

		if(txt.indexOf('+') != -1) return  craft + ":orbit:";
		if(txt.indexOf(':') != -1) return  "utc:";
		if(txt.indexOf('/') != -1) return  craft + ":sclk:";
		if(txt.matches("\\d+")    ) return  "et:";
		return  null;
	 }

	/**
	 ** Parses a floating-point value as-is.
	 **/
	private static double etFromEt(String rawTxt)
	 throws TimeException
	 {
		String txt = rawTxt;
		try
		 {
			if(txt.equals(""))
				throw  new TimeException(
					"Couldn't parse time (et:), empty value!");

			return  Double.parseDouble(txt);
		 }
		catch(TimeException e)
		 {
			throw  e;
		 }
		catch(Throwable e)
		 {
			log.println(e);
			throw  new TimeException("Couldn't parse time (et:" + rawTxt +")",
									 e);
		 }
	 }

	/**
	 ** Parses "[[#]/]######[.###][/]" as a SCLK.
	 **/
	private static double etFromSclk(String rawTxt, String craft)
	 throws TimeException
	 {
		if(craft == null)
			craft = timeCraft;

		String txt = rawTxt;
		try
		 {
			if(txt.endsWith("/"))
				txt = txt.substring(0, txt.length()-1);
			
			int slash = txt.indexOf('/');
			if(slash != -1)
				txt = txt.substring(slash+1);

			if(txt.equals(""))
				throw  new TimeException(
					"Couldn't parse time (sclk:), empty value!");

			double sclk = Double.parseDouble(txt);

			return  TimeCacheFactory.instance().getTimeCacheInstance(craft).sclkf2et(sclk);
		 }
		catch(TimeException e)
		 {
			throw  e;
		 }
		catch(Throwable e)
		 {
			log.println(e);
			throw  new TimeException(
				"Couldn't parse time (sclk:" + rawTxt + ")", e);
		 }
	 }

	/**
	 ** Parses "#####[+[[#:[##:]]##[.###]]]" as an ORBIT and OFFSET.
	 **/
	public static double etFromOrbit(String rawTxt, String craft)
	 throws TimeException
	 {
		if(craft == null)
			craft = timeCraft;

		String txt = rawTxt;
		try
		 {
			if(txt.equals(""))
				throw  new TimeException(
					"Couldn't parse time (orbit:), empty value!");

			int plus = txt.indexOf('+');
			int orbit;
			double offset = 0;
			if(plus == -1)
				orbit = Integer.parseInt(txt);
			else
			 {
				orbit = Integer.parseInt(txt.substring(0, plus));
				if(plus+1 != txt.length())
				 {
					StringTokenizer tok =
						new StringTokenizer(txt.substring(plus + 1), " :");
					int colons = 0;
					while(tok.hasMoreTokens())
					 {
						if(colons++ == 3)
							throw  new TimeException(
								"Couldn't parse time (orbit:" + rawTxt +
								"), due to too many colons");
						offset = offset * 60
							+ Double.parseDouble(tok.nextToken());
					 }
				 }
			 }

				return  TimeCacheFactory.instance().getTimeCacheInstance(craft).orbit2et(orbit, offset);
		 }
		catch(TimeException e)
		 {
			throw  e;
		 }
		catch(Throwable e)
		 {
			log.println(e);
			throw  new TimeException(
				"Couldn't parse time (orbit:" + rawTxt + ")", e);
		 }
	 }

	/**
	 ** Parses a UTC into an et.
	 **/
	private static double etFromUtc(String rawTxt, String craft)
	 throws TimeException
	 {
		// Reasonable default for UTC... there are microscopically
		// better things to do than this solution, but this will work
		// more than well enough.
		if(craft == null)
			craft = timeCraft;
		log.println(craft + " ----> " + rawTxt);
		try
		 {
			String txt = rawTxt;

			if(txt.equals(""))
				throw  new TimeException(
					"Couldn't parse time (utc:), empty value!");

			int dot = txt.indexOf('.');

			// For various reasons, it's easier to get rid of a
			// trailing dot with no millisecond value than to accept
			// that dot during parsing.
			if(dot+1 == txt.length())
			 {
				txt = txt.substring(0, dot);
				dot = -1;
			 }

			SimpleDateFormat[] utcFormats =
				dot == -1
				? utcFormatsNoDot
				: utcFormatsWithDot;

			for(int i=0; i<utcFormats.length; i++)
			 {
				ParsePosition pp = new ParsePosition(0);
				Date utc = utcFormats[i].parse(txt, pp);
				if(utc != null  &&  pp.getIndex() == txt.length())
						return  TimeCacheFactory.instance().getTimeCacheInstance(craft)
							.millis2et( utc.getTime() );
			 }

			throw  new TimeException("Couldn't parse time (utc:" + rawTxt +
									 "), invalid UTC format");
		 }
		catch(TimeException e)
		 {
			throw  e;
		 }
		catch(Throwable e)
		 {
			log.println(e);
			throw new TimeException(
				"Couldn't parse time (utc:" + rawTxt + ")", e);
		 }
	 }
	private static final SimpleDateFormat utcFormatsNoDot[];
	private static final SimpleDateFormat utcFormatsWithDot[];
	static
	 {
		String forms[] = { "yyyy-DDD'T'hh:mm:ss",
						   "yyyy-DDD // hh:mm:ss",
						   "yyyy-DDD//hh:mm:ss",
						   "yyyy/MM/dd-hh:mm:ss",
						   "yyyy MMM dd hh:mm:ss" };
		int count = forms.length;
		utcFormatsNoDot = new SimpleDateFormat[count];
		utcFormatsWithDot = new SimpleDateFormat[count];
		for(int i=0; i<count; i++)
		 {
			utcFormatsNoDot  [i] = new SimpleDateFormat(forms[i]         );
			utcFormatsWithDot[i] = new SimpleDateFormat(forms[i] + ".SSS");
		 }
	 }

	/**
	 ** Sets the default time format for input and display.
	 **
	 ** @param newFormat One of 'E', 'S', 'O', 'U'... the starting
	 ** letters of each TimeField-supported time type.
	 **
	 ** @throws IllegalArgumentException An invalid format was specified.
	 **/
	public static void setDefaultFormat(char newFormat)
	 {
		if("ESOU".indexOf(timeFormat) == -1)
			throw  new IllegalArgumentException(
				"Invalid time format requested ('" + newFormat + "')");
		TimeFormatEvent e = new TimeFormatEvent(timeFormat, newFormat);
		timeFormat = newFormat;
		fireTimeFormatChangeEvent(e);
	 }
	
	/**
	 * Returns the current default time format in use. The returned
	 * time format is encoded as a character.
	 * @see {@link #setDefaultFormat(char)}
	 * @return Current default time format encoded as a character.
	 */
	public static char getDefaultFormat(){
		return timeFormat;
	}
	
	/**
	 * Returns the string name corresponding to the default time format
	 * in use. If the time format is unknown, <code>INVALID</code>
	 * is returned.
	 */
	public static String getDefaultFormatStr(){
		switch(timeFormat){
			case 'E': return "ET";
			case 'S': return "SCLK";
			case 'O': return "ORBIT";
			case 'U': return "UTC";
		}
		return "INVALID";
	}
	
	public static String getDefaultCraft() {
		return timeCraft;
	}

	/**
	 * Listeners subsribed to the TimeFormatChangeEvent.
	 */
	private static List listeners = new ArrayList();
	/**
	 * Registers a listener to listen to TimeFormatChangeEvents.
	 * @param l A TimeFormatChangeListener.
	 */
	public static boolean addTimeFormatListener(TimeFormatChangeListener l){
		return listeners.add(l);
	}
	/**
	 * Unregisters a listener from listening to TimeFormatChangeEvents.
	 * @param l The TimeFormatChangeListener to remove.
	 * @return
	 */
	public static boolean removeTimeFormatListener(TimeFormatChangeListener l){
		return listeners.remove(l);
	}
	/**
	 * Notifies the TimeFormatChangeListeners of a TimeFormatChangeEvent.
	 * @param e TimeFormatChangeEvent to send.
	 */
	public static void fireTimeFormatChangeEvent(TimeFormatEvent e) {
		for(Iterator li=listeners.iterator(); li.hasNext(); ){
			((TimeFormatChangeListener)li.next()).timeFormatChanged(e);
		}
	}

	/**
	 ** Sets the default spacecraft used for spacecraft-dependent time
	 ** formats (orbit and sclk).
	 **
	 ** @param newCraft One of "MGS", "ODY", "MRO", or "MEX"
	 ** (case-insensitive).
	 **
	 ** @throws IllegalArgumentException An invalid craft was specified.
	 **/
	public static void setDefaultCraft(String newCraft)
	 {
		newCraft = newCraft.toUpperCase().intern();
		if(!TimeCacheFactory.instance().getCraftIdMap().keySet().contains(newCraft))
			throw  new IllegalArgumentException("Invalid craft specified ('"+ newCraft + "')");
		timeCraft = newCraft;
	 }
	
	/**
	 ** The default time format. Currently only changeable using the
	 ** {@link #setDefaultFormat} call. Defaults to 'E' at startup.
	 **
	 ** Current legal values are 'E', 'S', 'O', 'U', the starting
	 ** letters of each TimeField-supported time type.
	 **/
	private static char timeFormat = Config.get("default.time.format", TimeCacheFactory.FMT_TAG_ET).charAt(0);

	/**
	 ** The default spacecraft. Currently only changeable using the
	 ** {@link #setDefaultCraft} call. Defaults to the value of "time.db.default_key"
	 ** if defined in the jmars.config or the first craft in the key list "time.db.craft_name.*"
	 **/
	private static String timeCraft = null;
	static {
		final String TIME_CRAFT_KEY = "time.db.default_craft";
		try {
			Set<String> availableCrafts = TimeCacheFactory.instance().getCraftIdMap().keySet();
			if (availableCrafts.isEmpty())
				log.aprintln("No crafts available.");
			else
				timeCraft = Config.get(TIME_CRAFT_KEY, availableCrafts.iterator().next());
		} catch (Exception e) {
			throw new Error("Unable to retrieve a default value for "+TIME_CRAFT_KEY+" from the config file.", e);
		}
	}

	private static DecimalFormat sclkDecFormat = new DecimalFormat("0.000");
	/**
	 ** Converts from et into the current "default" time format.
	 **/
	public static String etToDefault(double et)
	 {
		return  etToFormat(et, timeCraft, timeFormat);
	 }

	
	/*
	 * offsetOrbit takes an ET and adds a number of orbits to it, then returns the 
	 * new ET.  If there is a problem with the time conversion, NaN will be returned.
	 */
	public static double offsetOrbit(double et, int numOrbits) {
		double newET = Double.NaN;
		try {
			String orbit = etToFormat(et, "orbit:");
			
			orbit = orbit
					.substring(orbit.indexOf("orbit:") + "orbit:".length());

			int plusLoc = orbit.indexOf("+");

			int orbitNum = 0;
			String offset;
			
			// Check for the case where we're at the very start of an orbit
			if (plusLoc==-1) {
				orbitNum = Integer.parseInt(orbit);
				offset = "";				
			} else {
				orbitNum = Integer.parseInt(orbit.substring(0, plusLoc));
				offset = orbit.substring(plusLoc);				
			}
			
			String newOrbit = (orbitNum + numOrbits) + offset;

			newET = TimeField.etFromOrbit(newOrbit, null);
		} catch (TimeException e) {
			// no op
		}
		return newET;
	}
	
	public static String etToFormat(double et, String format)
	 {
		String[] parts = format.split(":");
		log.println("Split into " + Arrays.asList(parts));
		if(parts.length == 1)
			return
				etToFormat(et, timeCraft, parts[0].toUpperCase().charAt(0));
		else
			return
				etToFormat(et, parts[0], parts[1].toUpperCase().charAt(0));
	 }

	public static String etToFormat(double et,
									String timeCraft,
									char timeFormat)
	 {
		if(timeFormat == 'E')
			return  "et:" + (long) et;

		try
		 {
			TimeCache tc = TimeCacheFactory.instance().getTimeCacheInstance(timeCraft);
			
				switch(timeFormat)
				 {
				 case 'S':
				return  timeCraft + ":sclk:"+ sclkDecFormat.format(tc.et2sclkf(et));

				 case 'O':
				return  timeCraft + ":orbit:" + tc.et2orbit(et);

				 case 'U':
				return  "utc:" + tc.et2utc(et);

				 default:
					throw  new TimeException("IMPOSSIBLE: bad format (" +
											 timeFormat + ")");
				 }
			 
		 }
		catch(TimeException e)
		 {
			log.println(e);
			return  "-ERROR-" + e;
		 }
	 }

	public void convertTo(String format)
	 throws TimeException
	 {
		String newText = etToFormat(getEt(), format);
		if(newText.startsWith("-ERROR-"))
			throw  new TimeException(newText);
		setText(newText);
	 }

	public static void main(String[] av)
	 throws TimeException
	 {
		DebugLog.readFile(".debugrc");
		TimeCacheFactory.instance().getTimeCacheInstance(timeCraft);
		final TimeField tf = new TimeField(timeCraft+":orbit:1000");
		tf.addActionListener(
			new ActionListener()
			 {
				public void actionPerformed(ActionEvent e)
				 {
					try
					 {
						log.aprintln("-------(" + tf.getText() + ")-------");
						log.aprintln("--> " + (long) tf.getEt());
					 }
					catch(Throwable ex)
					 {
						log.aprintln(ex);
					 }
					log.aprintln();
				 }
			 }
			);
		Dimension d = new Dimension(300, 30);
		tf.setMinimumSize(d);
		tf.setPreferredSize(d);

		JFrame f = new JFrame("test driver");
		f.getContentPane().add(tf);
		f.pack();
		f.setVisible(true);
	 }

	class MyPopup extends JPopupMenu
	 {
		ButtonGroup group = new ButtonGroup();
		String currFormat = parseTimeToFormat(TimeField.this.getText());

		MyPopup()
		 {
			add(new TimeConv("et:"));
			add(new TimeConv("utc:"));
			for(String sc: TimeCacheFactory.instance().getCraftIdMap().keySet()){
				add(new JPopupMenu.Separator());
				add(new TimeConv(sc+":orbit:"));
				add(new TimeConv(sc+":sclk:" ));
			}
		 }

		class TimeConv extends JRadioButtonMenuItem implements ActionListener
		 {
			String format;
			TimeConv(String format)
			 {
				super(format);
				this.format = format;
				group.add(this);
				if(format.equals(currFormat))
					setSelected(true);
				addActionListener(this);
			 }

			public void actionPerformed(ActionEvent e)
			 {
				try
				 {
					String currText = TimeField.this.getText();
					log.println("Converting to " + format);
					if(currText == null  ||  !currText.matches(".*\\d.*"))
						TimeField.this.setText(format);
					else
						convertTo(format);
					log.aprintln("Converted " + currText +
								 " to " + TimeField.this.getText());
				 }
				catch(TimeException ex)
				 {
					log.aprintln(ex);
					JOptionPane.showMessageDialog(TimeField.this, ex,
												  "INVALID TIME",
												  JOptionPane.ERROR_MESSAGE);
				 }
			 }
		 }
	 }

	private class MyMouse extends MouseAdapter
	 {
		public void mousePressed(MouseEvent e)
		 {
			if(!SwingUtilities.isRightMouseButton(e))
				return;

			JPopupMenu popup = new MyPopup();
			popup.show(TimeField.this, e.getX(), e.getY());
		 }
	 }
	
	public static class TimeFormatEvent {
		public final char oldFormat, newFormat;
		
		public TimeFormatEvent(char oldFormat, char newFormat){
			this.oldFormat = oldFormat;
			this.newFormat = newFormat;
		}
	}
	public static interface TimeFormatChangeListener {
		public void timeFormatChanged(TimeFormatEvent e);
	}
 }
