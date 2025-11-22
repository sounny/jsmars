package edu.asu.jmars.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToolTip;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.filechooser.FileFilter;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.map2.stages.ColorStretcherStageSettings;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

/**
 ** A {@link MultiSlider} used to drag around tie-points on a color
 ** scale, bundled with a visual display of that scale.
 **
 ** <p><big>The cmap file format:</big>
 **
 ** <p>Colormap files should generally have the extension ".cmap"
 ** appended. Lines beginning with a hash are considered comments, as
 ** are blank lines. Non-empty lines should consist of a key, followed
 ** by (non-line-terminating) whitespace, followed by a value.
 ** Colormap file readers should read keys leniently... unrecognized
 ** keys are to be ignored.
 **
 ** <p>Both CR and LF should be (individually) interpreted as EOL
 ** characters.
 **
 ** <p>All contents of colormap files are case-insensitive. Integers
 ** occurring in a colormap file can be formatted in any of the three
 ** standard C integer formats for decimal, octal, and hexadecimal.
 ** That is, the following all represent same number: 42, 052, 0x2A.
 **
 ** <p>The first 8 bytes of any colormap file conforming to this
 ** standard must be ASCII values "#cmap256". This is the only data in
 ** the file that is case-sensitive.
 **
 ** <p><b>Tie-point key/values.</b> For these, the keys are index
 ** numbers, the values are a numeric color value. Index numbers must
 ** be in the range 0-255, colors must be 24-bit integers
 ** (signed/unsigned doesn't matter).  For the color values, the 24
 ** bits are interpreted as RGB: rrrrggggbbbb. Any bits above 24 are
 ** ignored. Each such index/value pair represents a single tie-point
 ** in the color map. The tie points must occur in strictly ascending
 ** order by index.
 **
 ** <p><b>Interpolation key/value.</b> The method for interpolation
 ** between colors may be specified anywhere in the file by the
 ** presence of a single line with the key "interpolation". The value
 ** is one or more whitespace-separated words. The first word should
 ** be either "hsb" or "rgb", to specify which colorspace to use for
 ** interpolation. For hsb, this can be followed by any one of the
 ** following words: "shortest", "direct", "increasing", "decreasing".
 ** These specify how to interpolate hues. The "shortest" method takes
 ** the shortest path possible between two hues, potentially wrapping
 ** past the 0/360 boundary. The "direct" method simply takes the
 ** direct route between hues without ever crossing the 0/360 wrap
 ** border. The "increasing" and "decreasing" methods always use
 ** increasing or decreasing hue values to go between two hues. If
 ** omitted, the default interpolation is "hsb short", if all/any of
 ** the interpolation scheme is omitted. Currently only linear
 ** interpolation is supported, others may be added in the
 ** future. Programs should ignore interpolation words they don't
 ** understand.
 **/
public class ColorMapper
 extends JPanel
 {
	private static DebugLog log = DebugLog.instance();

	private static final String FILE_TYPE = "#cmap256";

	protected MultiSlider mslider;
	protected ColorScale scale;
	private double minSettingsValue;
	private double maxSettingsValue;

	public ColorMapper()
	 {
		this(new int[] { 0, 255 },
			 new Color[] { Color.black, Color.white }
			);
	 }
	public void setColorSettingsMax(double colorSettingsMax) {
		this.maxSettingsValue = colorSettingsMax;
	}
	public void setColorSettingsMin(double colorSettingsMin) {
		this.minSettingsValue = colorSettingsMin;
	}
	public ColorMapper(int[] values, Color[] colors, ColorStretcherStageSettings settings) {
		super(new BorderLayout());
		
		minSettingsValue = settings.getMinValue();
		maxSettingsValue = settings.getMaxValue();
		mslider = new MultiSlider(0, 255, values);
		scale = new ColorScale(mslider, colors, settings);
		scale.setBorder(
			BorderFactory.createEmptyBorder(0, mslider.getLeftPadding(),
											0, mslider.getRightPadding())
			);

		add(mslider, BorderLayout.NORTH);
		add(scale, BorderLayout.CENTER);

		DntipListener dntip = new DntipListener();
		mslider.addMouseListener(dntip);
		mslider.addMouseMotionListener(dntip);

		PopupListener popup = new PopupListener();
		mslider.addMouseListener(popup);
		scale.addMouseListener(popup);

		DoubleClickListener doubles = new DoubleClickListener();
		mslider.addMouseListener(doubles);
		scale.addMouseListener(doubles);

		extraInit();
	}
	public ColorMapper(int[] values, Color[] colors)
	 {
		this(values,colors, new ColorStretcherStageSettings());
	 }

	public ColorScale getColorScale()
	 {
		return  scale;
	 }

	public Dimension getPreferredSize()
	 {
		Dimension size = super.getPreferredSize();
		size.height += 3 * scale.getPreferredSize().height;
		return  size;
	 }
	
	public Dimension getMinimumSize()
	 {
		Dimension size = super.getMinimumSize();
		size.height += 3 * scale.getMinimumSize().height;
		return  size;
	 }

	protected void extraInit()
	 {
	 }

	private class DntipListener extends MouseInputAdapter
	 {
		public void mouseDragged(MouseEvent e)
		 {
			if(!mslider.isEnabled())
                return;
			if(tip.isVisible())
				updateTip();
		 }
		public void mousePressed(MouseEvent e)
		 {
			if(!mslider.isEnabled())
                return;
			if(!SwingUtilities.isRightMouseButton(e)  &&  mslider.locationToTab(e.getX()) != -1)
				updateTip();
		 }
		public void mouseReleased(MouseEvent e)
		 {
			if(!mslider.isEnabled())
                return;
			if(tip.isVisible())
				tip.setVisible(false);
		 }

		JToolTip tip = new JToolTip() {{ setVisible(false); }};
		JLayeredPane pane;
		private void updateTip()
		 {
			if(pane == null)
			 {
				JRootPane rpane = mslider.getRootPane();
				pane = rpane.getLayeredPane();
				pane.add(tip, JLayeredPane.POPUP_LAYER);
			 }
			
			int xLoc = mslider.getTabLocation();
			Insets insets = getInsets();
			//Add the value to to tool tip 
			int index = Math.round(255f * (xLoc - insets.left)/(getWidth() - insets.right - insets.left));
			double inc = (maxSettingsValue - minSettingsValue) / 255.0 ;
			double value = (inc * index) + minSettingsValue;
			tip.setTipText("Value: " + NumberFormat.getInstance().format(value) + " / DN: " + mslider.getValue());
			Dimension size = tip.getPreferredSize();
			tip.setSize(size);

			// Set the location, takes some thinking
			int x = mslider.getTabLocation() - size.width/2;
			x = Util.bound(0, x, mslider.getWidth() - size.width);
			int y = mslider.getHeight() + scale.getHeight() - size.height;
			y = Math.max(mslider.getHeight(), y);
			tip.setLocation(SwingUtilities.convertPoint(
				mslider, x, y, pane));

			tip.setVisible(true);
			tip.repaint();
		 }
	 }

	public static class State implements Cloneable, Serializable
	 {
		private static final long serialVersionUID = 2945325279792141976L;
		
		private int[] values;
		private Color[] colors;
		private ColorInterp interpolation;
		// WARNING: everything is referenced, NOT cloned!
		public State(int[] values, Color[] colors, ColorInterp interpolation)
		 {
			this.values = values;
			this.colors = colors;
			this.interpolation = interpolation;
		 }
		
		public State(int[] values, Color[] colors)
		 {
			this(values, colors, ColorInterp.LIN_HSB_SHORT);
		 }
		
		public Object clone() throws CloneNotSupportedException {
			State s = (State)super.clone();
			return s;
		}

		public ColorMapOp getColorMapOp(BufferedImage image)
		 {
			if(values.length == 2  &&  values[0] == 0  &&  values[1] == 255  &&
			   colors[0].equals(Color.black)  && colors[1].equals(Color.white))
				return  new ColorMapOp(image);
			else
				return  new ColorMapOp(interpolation.createColorMap(values,
																	colors), image);
		 }
		
		public int[] getValues(){
			return values.clone();
		}
		
		public Color[] getColors(){
			return colors.clone();
		}
		
		public ColorInterp getInterpolation(){
			return interpolation;
		}

		public String toString(){
			return this.getClass().getSimpleName()+"(values="+Arrays.asList(getValues())+
			"; colors="+Arrays.asList(getColors())+
			"; interp="+getInterpolation().getKeyword()+")";
		}
		
		public static State DEFAULT = new State(
			new int[] { 0, 255 },
			new Color[] { Color.black, Color.white },
			ColorInterp.LIN_HSB_SHORT
			);
		
		/**
		 * Compare the values to determine if the passed in state is equal to this state
		 * @param checkState
		 * @return
		 */
		public boolean equals(State checkState){
			boolean valuesEqual = Arrays.equals(checkState.getValues(),values);
			boolean colorsEqual = Arrays.equals(checkState.getColors(),colors);
			boolean interpEqual = interpolation.equals(checkState.getInterpolation());
			return (valuesEqual && colorsEqual && interpEqual);
		}
	 }

	public State getState()
	 {
		return  new State(mslider.getValues(),
						  scale.getColors(),
						  scale.getInterpolation());
	 }

	public void setState(State state)
	 {
		if(state == null)
			return;
		setColors(state.values, state.colors, state.interpolation);
	 }

	public void setColors(int[] vals, Color[] cols, ColorInterp interpolation)
	 {
		mslider.setValueIsAdjusting(true);
		scale.setColors(vals, cols, interpolation);
		mslider.setValues(vals);
		mslider.setValueIsAdjusting(false);
	 }

	public void setColors(int[] vals, Color[] cols)
	 {
		mslider.setValueIsAdjusting(true);
		scale.setColors(vals, cols);
		mslider.setValues(vals);
		mslider.setValueIsAdjusting(false);
	 }

	public void setValues(int[] vals)
	 {
		mslider.setValues(vals);
	 }

	public int[] getValues()
	 {
		return  mslider.getValues();
	 }

	public ColorMapOp getColorMapOp()
	 {
		return  new ColorMapOp(scale, null);
	 }

	public void addChangeListener(ChangeListener l)
	 {
		scale.addChangeListener(l);
	 }

	public void removeChangeListener(ChangeListener l)
	 {
		scale.removeChangeListener(l);
	 }

	public boolean isAdjusting()
	 {
		return  mslider.getValueIsAdjusting();
	 }

	private static String niceHex(long val, int bits)
	 {
		long mask = Long.MAX_VALUE % (1 << bits);
		String hex = Long.toHexString(val & mask).toUpperCase();
		hex = "0000000000000" + hex;
		return  hex.substring(hex.length() - (bits+3)/4);
	 }

	public void saveColors(File f)
        {
	    log.aprintln(f);
	    
	    try {
		saveColors(new FileOutputStream(f));
	    }
	    catch(IOException e)
	    {
		log.aprintln(e);
		Util.showMessageDialog(
					      "Unable to save to file:\n" + f + "\n" + e,
					      "File Error",
					      JOptionPane.ERROR_MESSAGE);
	    }
        }

	public void saveColors(OutputStream sout)
	    throws IOException
	{
	    PrintStream fout = null;
	    try
	    {
		fout =
		    new PrintStream(
				    new BufferedOutputStream(sout));
		if(fout.checkError())
		    throw  new IOException("Problem opening stream.");
		
		fout.println(FILE_TYPE + "  Colormap file, created by JMARS");
		for(int i=0; i<scale.getColorCount(); i++)
		    fout.println(
				 scale.getColorVal(i) + "\t" +
				 "0x" + niceHex(scale.getColor(i).getRGB(), 24)
				 );
		fout.println("interpolation " +
			     scale.getInterpolation().getKeyword());
	    }
	    catch(IOException e)
	    {
		throw(e);
	    }
	    finally
	    {
		if(fout != null)
		    fout.close();
	    }
	}

	public void loadColors(URL url)
	 {
		log.println(url);
		BufferedReader fin = null;
		try
		 {
			fin = new BufferedReader(new InputStreamReader(url.openStream()));
			loadColors(fin);
		 }
		catch(Throwable e)
		 {
			log.aprintln(e);
			Util.showMessageDialog("Unable to load built-in colormap:\n" + e,
				"File Load Error",
				JOptionPane.ERROR_MESSAGE);
			return;
		 }
		finally
		 {
			try
			 {
				if(fin != null)
					fin.close();
			 }
			catch(IOException e)
			 {
			 }
		 }
	 }

	public void loadColors(File f)
	 {
		log.aprintln(f);
		BufferedReader fin = null;
		try
		 {
			fin = new BufferedReader(new FileReader(f));
			loadColors(fin);
		 }
		catch(Throwable e)
		 {
			log.aprintln(e);
			Util.showMessageDialog("Unable to load colormap:\n" + f + "\n" + e,
				"File Load Error",
				JOptionPane.ERROR_MESSAGE);
			return;
		 }
		finally
		 {
			try
			 {
				if(fin != null)
					fin.close();
			 }
			catch(IOException e)
			 {
			 }
		 }
	 }

        public void loadColors(InputStream sin)
	    throws IOException
        {
	    loadColors(new BufferedReader(new InputStreamReader(sin)));
	}

	public void loadColors(BufferedReader fin)
	 throws IOException
	 {
		char[] fileType = new char[FILE_TYPE.length()];
		fin.read(fileType);
		if(!FILE_TYPE.equals(new String(fileType)))
			throw  new IOException("Unrecognized file format.");
		fin.readLine();

		ColorInterp interpolation = ColorInterp.LIN_HSB_SHORT;
		SortedMap colors = new TreeMap();
		String line;
		while((line = fin.readLine()) != null)
		 {
			try
			 {
				line = line.trim();

				// Skip useless lines
				if(line.length() == 0  ||  line.charAt(0) == '#')
					continue;

				StringTokenizer tok = new StringTokenizer(line);
				String key = tok.nextToken().toLowerCase();
				if(Character.isDigit(key.charAt(0)))
				 {
					// Parse tie-point
					String val = tok.nextToken();
					colors.put(Long.decode(key),
							   new Color(Long.decode(val).intValue(), false));
				 }
				else if(key.equals("interpolation"))
				 {
					String word = tok.nextToken().toLowerCase();
					interpolation = ColorInterp.forKeyword(word);
				 }
				else
					throw  new Throwable();
			 }
			catch(Throwable e)
			 {
				log.aprintln("Ignoring line from file: " + line);
			 }
		 }

		int len = colors.size();
		log.println("Read " + len + " colors");
		if(len < 2)
			throw  new Error(
				"Colormap files must have at least 2 tie-points");

		int[] vals = new int[len];
		Color[] cols = new Color[len];
		Iterator iter = colors.entrySet().iterator();
		for(int i=0; i<len; i++)
		 {
			Map.Entry entry = (Map.Entry) iter.next();
			vals[i] = ( (Long) entry.getKey() ).intValue();
			cols[i] = (Color) entry.getValue();
		 }
		setColors(vals, cols, interpolation);
	 }
	
	private FileFilter cmapFilter = new FileFilter() {
		public boolean accept(File f) {
			String fname = f.getName().toLowerCase();
			return f.isDirectory() || fname.endsWith(".cmap");
		}
		public String getDescription() {
			return "Colormap files (*.cmap)";
		}
	};
	
	private FileFilter imageFilter = new FileFilter() {
		private final Set<String> formatNames = new TreeSet<String>();
		{
			for (String name: ImageIO.getReaderFormatNames()) {
				formatNames.add(name.toLowerCase());
			}
		}
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			} else {
				String name = f.getName();
				int formatIdx = name.lastIndexOf(".");
				if (formatIdx >= 0 && formatIdx < name.length() - 1) {
					return formatNames.contains(name.substring(formatIdx + 1).trim().toLowerCase());
				} else {
					return false;
				}
			}
		}
		public String getDescription() {
			return "Image (*." + Util.join(", *.", formatNames) + ")";
		}
	};
	
	private JFileChooser chooser;
	private JFileChooser getFileChooser(FileFilter filter) {
		if (chooser == null) {
			chooser = new JFileChooser(Util.getDefaultFCLocation());
		}
		for (FileFilter f: chooser.getChoosableFileFilters()) {
			chooser.removeChoosableFileFilter(f);
		}
		chooser.addChoosableFileFilter(filter);
		chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
		chooser.setFileFilter(filter);
		return chooser;
	}
	
	/**
	 ** Re-scales the current range of tabs to fit within the given DN
	 ** range.
	 **/
	public void rescaleTo(int min, int max)
	 {
		int[] vals = mslider.getValues();
		int count = vals.length;
		int newWidth = max-min;
		int last = count-1;
		int oldWidth = vals[last] - vals[0];
		int oldMin = vals[0];

		// Make sure we can fit in all the tabs
		if(newWidth+1 < count)
		 {
			int need = count - newWidth - 1;
			newWidth = count - 1;
			min -= need/2;
			max = min + newWidth;
			int slide = 0;
			// Make sure we stay within the bounds of the slider
			if(min < mslider.getMinimum()) slide += mslider.getMinimum() - min;
			if(max > mslider.getMaximum()) slide += mslider.getMaximum() - max;
			min += slide;
			max += slide;
		 }

		// Rescale everything
		float scale = (float) newWidth / oldWidth;
		for(int i=0; i<count; i++)
			vals[i] = Math.round( (vals[i]-oldMin) * scale + min );

		// Fix overlapping values
		for(int i=1; i<count; i++)
			if(vals[i] <= vals[i-1]) vals[i] = vals[i-1] + 1;
		for(int i=last-1; i>=0; i--)
			if(vals[i] >= vals[i+1]) vals[i] = vals[i+1] - 1;

		// Done!
		mslider.setValues(vals);
	 }

	public void setEnabled(boolean e)
	 {
		super.setEnabled(e);
		mslider.setEnabled(e);
		scale.setEnabled(e);
	 }

	private class RescaleTo extends AbstractAction
	 {
		int min, max;

		RescaleTo(int min, int max)
		 {
			super("Rescale to " + min + "-" + max);
			this.min = min;
			this.max = max;
		 }

		public void actionPerformed(ActionEvent e)
		 {
			rescaleTo(min, max);
		 }
	 }

	private class DoubleClickListener extends MouseAdapter
	 {
		public void mouseClicked(MouseEvent e)
		 {
			if(!mslider.isEnabled())
                return;
			if(e.getClickCount() != 2)
				return;
			if(!SwingUtilities.isLeftMouseButton(e))
				return;

			int tab = mslider.locationToTab(e.getX());
			if(tab != -1)
			 {
				Color newColor =
					JColorChooser.showDialog(ColorMapper.this,
											 "Change tab color",
											 scale.getColor(tab));
				if(newColor != null)
				 {
					scale.setColor(tab, newColor);
					scale.repaint();
				 }
			 }
			else
			 {
				int value = mslider.locationToValue(e.getX());
				// Determine which tab the click precedes
				for(tab=0; tab<mslider.getValueCount(); tab++)
					if(value < mslider.getValue(tab))
						break;
				int[]   vals =
					(int[]  ) Util.insElement(mslider.getValues(), tab);
				Color[] cols =
					(Color[]) Util.insElement(  scale.getColors(), tab);
				vals[tab] = value;
				cols[tab] = scale.locationToColor(e.getX());
				setColors(vals, cols);
				mslider.setActiveTab(tab);
			 }
		 }
	 }

	private class PopupListener extends MouseAdapter
	 {
		public void mousePressed(MouseEvent e)
		 {
			if(!mslider.isEnabled())
                return;
			if(!SwingUtilities.isRightMouseButton(e))
				return;

			int tab = mslider.locationToTab(e.getX());
			JPopupMenu popup = new JPopupMenu();

			if(tab != -1)
				mslider.setActiveTab(tab);
			mslider.requestFocus();

			if(tab == -1)
				popup.add(new DoInsertTab(tab, e));
			else
			 {
				popup.add(new DoSetTabColor(tab));
				popup.add(new DoDeleteTab(tab));
			 }
			popup.add(new Interpolation());
			popup.add(new JSeparator());
			popup.add(new SaveColors());
			popup.add(new LoadColors());
			popup.add(new LoadImagePalette());
			popup.add(new ResetColors());

			popup.show((Component) e.getSource(), e.getX(), e.getY());
		 }
	 }

	private class Interpolation extends JMenu
	 {
		ButtonGroup group = new ButtonGroup();

		Interpolation()
		 {
			super("Interpolation");

			for(int i=0; i<ColorInterp.ALL.length; i++)
				add(new InterpolationItem(ColorInterp.ALL[i]));
		 }

		class InterpolationItem extends JRadioButtonMenuItem
		 {
			InterpolationItem(final ColorInterp interp)
			 {
				super(interp.getTitle());
				group.add(this);
				setSelected(interp.equals(scale.getInterpolation()));
				addActionListener(
					new ActionListener()
					 {
						public void actionPerformed(ActionEvent e)
						 {
							scale.setInterpolation(interp);
						 }
					 }
					);
			 }
		 }
	 }

	private class SaveColors extends AbstractAction
	 {
		SaveColors()
		 {
			super("Save colors...");
		 }

		public void actionPerformed(ActionEvent e)
		 {
			// Show the file chooser until we find an acceptable file
			JFileChooser fc = getFileChooser(cmapFilter);
			if(Util.showSaveWithConfirm(fc, ".cmap"))
				saveColors(fc.getSelectedFile());
		 }
	 }
	
	private class LoadColors extends AbstractAction
	 {
		LoadColors()
		 {
			super("Load colors...");
		 }

		public void actionPerformed(ActionEvent e)
		 {
			// Show the file chooser
			JFileChooser fc = getFileChooser(cmapFilter);
			if(fc.showOpenDialog(ColorMapper.this)
			   != JFileChooser.APPROVE_OPTION)
				return;
			
			loadColors(fc.getSelectedFile());
		 }
	 }
	
	private class LoadImagePalette extends AbstractAction {
		public LoadImagePalette() {
			super("Load Colors From File...");
		}
		private void setPalette(Map<Integer,Color> palette) {
			int[] values = new int[palette.size()];
			Color[] colors = new Color[palette.size()];
			int i = 0;
			for (int value: palette.keySet()) {
				values[i] = value;
				colors[i] = palette.get(value);
				i++;
			}
			setColors(values, colors);
		}
		private Map<Integer,Color> choosePalette(final List<Map<Integer,Color>> palettes) {
			final JDialog dlg = new JDialog(JOptionPane.getFrameForComponent(ColorMapper.this), "Multiple palettes founds, choose one...", true);
			JList palList = new JList(palettes.toArray());
			palList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			palList.setSelectedIndex(0);
			palList.setCellRenderer(new ListCellRenderer() {
				private int renderIndex;
				private JLabel row = new JLabel() {
					public void paintComponent(Graphics g) {
						super.paintComponent(g);
						int gap = 4;
						int width = getWidth() - gap*2;
						int height = getHeight() - gap*2;
						g.clearRect(gap, gap, width, height);
						Map<Integer,Color> palette = palettes.get(renderIndex);
						for (int pos: palette.keySet()) {
							g.setColor(palette.get(pos));
							int x1 = gap + pos*width/256;
							int x2 = gap + (pos+1)*width/256;
							g.fillRect(x1, gap, x2-x1+1, height);
						}
					}
				};
				private Border space = new EmptyBorder(15,120,15,120);
				private Border selBorder = new CompoundBorder(new LineBorder(Color.blue, 2), space);
				private Border unsBorder = new CompoundBorder(new EmptyBorder(2,2,2,2), space);
				public Component getListCellRendererComponent(JList list,
						Object value, int index, boolean isSelected,
						boolean cellHasFocus) {
					this.renderIndex = index;
					Dimension d = list.getSize();
					row.setSize(new Dimension(d.width, d.width / 8));
					row.setBorder(isSelected ? selBorder : unsBorder);
					return row;
				}
			});
			
			JButton ok = new JButton("OK".toUpperCase());
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dlg.setVisible(false);
				}
			});
			
			Box h = Box.createHorizontalBox();
			h.add(Box.createHorizontalGlue());
			h.add(ok);
			
			JPanel pane = new JPanel(new BorderLayout(4,4));
			pane.add(new JScrollPane(palList), BorderLayout.CENTER);
			pane.add(h, BorderLayout.SOUTH);
			pane.setBorder(new EmptyBorder(4,4,4,4));
			
			dlg.getContentPane().add(pane);
			dlg.pack();
			dlg.setSize(400,400);
			dlg.setVisible(true);
			
			return palettes.get(palList.getSelectedIndex());
		}
		public void actionPerformed(ActionEvent e) {
			JFileChooser fc = getFileChooser(imageFilter);
			if (fc.showOpenDialog(ColorMapper.this) == JFileChooser.APPROVE_OPTION
					&& fc.getSelectedFile() != null
					&& fc.getSelectedFile().exists()) {
				try {
					List<Map<Integer,Color>> palettes = loadImagePalettes(fc.getSelectedFile());
					switch (palettes.size()) {
					case 0:
						throw new IllegalArgumentException("That image does not contain a palette");
					case 1:
						setPalette(palettes.get(0)); break;
					default:
						setPalette(choosePalette(palettes)); break;
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					Util.showMessageDialog("An error occurred reading the color palette:\n\n" + 
							ex.getMessage() +
							"\n\nSee command line for details", 
						"Unable to read color palette",
						JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}
	
	/**
	 * Returns a list of palettes found in this file, by retrieving the image
	 * format's color information if an indexcolormodel is used, and by parsing
	 * the image pixels if x*y==256 and z in [1,3,4].
	 */
	private List<Map<Integer,Color>> loadImagePalettes(File file) throws IOException {
		List<Map<Integer,Color>> palettes = new ArrayList<Map<Integer,Color>>(2);
		ImageInputStream iis = ImageIO.createImageInputStream(file);
		Iterator<ImageReader> irit = ImageIO.getImageReaders(iis);
		while (irit.hasNext()) {
			ImageReader ir = irit.next();
			ir.setInput(iis);
			for (int imgnum = 0; imgnum < Integer.MAX_VALUE; imgnum++) {
				try {
					Iterator<ImageTypeSpecifier> its = ir.getImageTypes(imgnum);
					while (its.hasNext()) {
						ImageTypeSpecifier it = its.next();
						ColorModel cm = it.getColorModel();
						// get a palette from an indexcolormodel
						if (cm instanceof IndexColorModel) {
							IndexColorModel icm = (IndexColorModel)cm;
							int size = icm.getMapSize();
							int[] rgbs = new int[size];
							icm.getRGBs(rgbs);
							Map<Integer,Color> colors = new TreeMap<Integer,Color>();
							for (int i = 0; i < size; i++) {
								colors.put(i, new Color(rgbs[i], icm.hasAlpha()));
							}
							palettes.add(colors);
						}
					}
					// check for a palette in the pixels of the image
					int width = ir.getWidth(imgnum);
					int height = ir.getHeight(imgnum);
					if (width * height == 256) {
						int bands = ir.getRawImageType(imgnum).getNumBands();
						if (bands == 1 || bands == 3 || bands == 4) {
							BufferedImage bi = ir.read(imgnum);
							Map<Integer,Color> palette = new TreeMap<Integer,Color>();
							boolean alpha = bi.getColorModel().hasAlpha();
							for (int j = 0; j < height; j++) {
								for (int i = 0; i < width; i++) {
									palette.put(j*width+i, new Color(bi.getRGB(i, j), alpha));
								}
							}
							palettes.add(palette);
						}
					}
				} catch (IndexOutOfBoundsException e) {
					// this is icky flow control but recommended for performance
					// with some ImageReaders
					break;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return palettes;
	}

private class ResetColors extends JMenu
	 {
		ResetColors()
		 {
			super("Built-in colors");
			add(new GrayscaleMaps());
			add(new ColorMaps());
			add(new ColorblindMaps());
			add(new MiscellaneousMaps());
			add(new LoadColorMap("colormap.cmap", "TES Colormap"));
			add(new LoadColorMap("colormap_daily.cmap", "TES Daily Colormap"));
		 }
	 }

private class ColorblindMaps extends JMenu {
	ColorblindMaps() {
    	super("Colorblind Accessible ");
		add(new LoadColorMap("viridis256.cmap", "Viridis Palette (256)"));
		add(new LoadColorMap("viridis17.cmap", "Viridis Palette (17)"));
		add(new LoadColorMap("viridis9.cmap", "Viridis Palette (9)"));
		add(new LoadColorMap("viridis256i.cmap", "Viridis Palette (256) Inverted"));
		add(new LoadColorMap("viridis17i.cmap", "Viridis Palette (17) Inverted"));
		add(new LoadColorMap("viridis9i.cmap", "Viridis Palette (9) Inverted"));
    }
}

    private class GrayscaleMaps extends JMenu {
    	GrayscaleMaps() {
        	super("Grayscale");
			add(new LoadColorMap("gray2.cmap",     "Grayscale (2)"         ));
			add(new LoadColorMap("gray3.cmap",     "Grayscale (3)"         ));
			add(new LoadColorMap("gray4.cmap",     "Grayscale (4)"         ));
			add(new LoadColorMap("gray5.cmap",     "Grayscale (5)"         ));
			add(new LoadColorMap("gray2i.cmap",    "Inverted Grayscale (2)"));
        }
    }

    private class ColorMaps extends JMenu {
    	ColorMaps() {
        	super("Color");
			add(new LoadColorMap("rgbr.cmap",      "RGB"                   ));
			add(new LoadColorMap("b_rgb_w.cmap",   "Black-RGB-White"       ));
			add(new LoadColorMap("spectrum.cmap",  "Full Spectrum"         ));
			add(new LoadColorMap("spectrumi.cmap", "Inverted Spectrum"     ));
			add(new LoadColorMap("dv6color.cmap", "Color Scale (6)"));
			add(new LoadColorMap("dv7color.cmap", "Color Scale (7)"));
			add(new LoadColorMap("xvcolor.cmap", "XV Color Scale"));
        }
    }

    private class MiscellaneousMaps extends JMenu {
    	MiscellaneousMaps() {
        	super("Miscellaneous");
			add(new LoadColorMap("xvcolor.cmap", "XV Color Scale"));
			add(new LoadColorMap("topo_schwarzwald.cmap", "Topo Schwarzwald"));
        }
    }


    
	private class LoadColorMap extends AbstractAction
	 {
		URL url;
		LoadColorMap(String fname, String title)
		 {
			super(title);
			url = Main.getResource("resources/" + fname);
		 }

		public void actionPerformed(ActionEvent e)
		 {
			loadColors(url);
		 }
	 }

	private class DoInsertTab extends ColorSubMenu
	 {
		int value;
		int tab;
		DoInsertTab(int clickedTab, MouseEvent e)
		 {
			super("Insert tab");
			colorChooserTitle = "New tab color";
			colorChooserDefault = scale.locationToColor(e.getX());
			colorChooserParent = ColorMapper.this;


			value = mslider.locationToValue(e.getX());
			this.setEnabled(clickedTab == -1  &&  value != -1);

			// Determine which tab the click precedes
			for(tab=0; tab<mslider.getValueCount(); tab++)
				if(value < mslider.getValue(tab))
					break;
		 }

		protected void prependMenuItems()
		 {
			add(
				new AbstractAction("Default color")
				 {
					public void actionPerformed(ActionEvent e)
					 {
						colorChosen(colorChooserDefault);
					 }
				 }
				);
		 }

		protected void colorChosen(Color newColor)
		 {
			int[]   vals =   (int[]) Util.insElement(mslider.getValues(), tab);
			Color[] cols = (Color[]) Util.insElement(  scale.getColors(), tab);
			vals[tab] = value;
			cols[tab] = newColor;
			setColors(vals, cols);
		 }
	 }

	private class DoDeleteTab extends AbstractAction
	 {
		int tab;
		DoDeleteTab(int tab)
		 {
			super("Delete tab");
			this.tab = tab;
			this.setEnabled(tab != -1  &&  mslider.getValueCount() > 2);
		 }

		public void actionPerformed(ActionEvent e)
		 {
			int[]   vals =   (int[]) Util.delElement(mslider.getValues(), tab);
			Color[] cols = (Color[]) Util.delElement(  scale.getColors(), tab);
			setColors(vals, cols);
		 }
	 }

	private class DoSetTabColor extends ColorSubMenu
	 {
		int tab;
		DoSetTabColor(int tab)
		 {
			super("Set tab color");
			this.tab = tab;
			colorChooserTitle = "Change tab color";
			colorChooserDefault = scale.getColor(tab);
			colorChooserParent = ColorMapper.this;
			this.setEnabled(tab != -1);
		 }

		protected void colorChosen(Color newColor)
		 {
			scale.setColor(tab, newColor);
			scale.repaint();
		 }
	 }
 }
