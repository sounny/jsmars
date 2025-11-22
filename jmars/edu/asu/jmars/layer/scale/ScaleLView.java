package edu.asu.jmars.layer.scale;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.asu.jmars.graphics.FontRenderer;
import edu.asu.jmars.graphics.JFontChooser;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.WrappedMouseEvent;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.swing.ColorButton;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;

public class ScaleLView extends Layer.LView implements MouseMotionListener, MouseListener {
	private static final long serialVersionUID = 4808626677999276798L;
	private final ScaleParameters parms;
	private Point mouseOffset = null;
	private Rectangle lastBox;
	private ScaleLayer myLayer;
	
	public ScaleLView(boolean main, ScaleLayer layerParent, ScaleParameters params, ScaleLView3D lview3d) {
		super(layerParent, lview3d);
		myLayer = layerParent;
		setBufferCount(1);
		this.parms = layerParent.getParameters();
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	
	private static Map<Double,String> siUnits = new LinkedHashMap<Double,String>();
	private static Map<Double,String> usUnits = new LinkedHashMap<Double,String>();
	static {
		siUnits.put(1d, "km");
		siUnits.put(1000d, "m");
		siUnits.put(1000*100d, "cm");
		usUnits.put(0.621371192, "mi");
		usUnits.put(3280.83989376, "ft");
	}
	
	private FontRenderer getFontBox(String label) {
		FontRenderer fr = new FontRenderer(parms.labelFont, parms.fontOutlineColor, parms.fontFillColor);
		fr.setLabel(label);
		fr.setAntiAlias(true);
		fr.setBorder(null);
		return fr;
	}
	
	public void paintComponent(Graphics g) {
		Graphics2D g2 = null;
		try {
			g2 = (Graphics2D) getOffScreenG2Direct(0);
			// Don't paint if we're on the panner, or not ready to paint
			if (getChild() == null || g2 == null) {
				return;
			}
			
			int rulerWidth = (getWidth()-2)*parms.width/100;
			int rulerHeight = 10;
			int tickHeight = rulerHeight * 3 / 2;
			
			Map<Double,String> units = (parms.isMetric ? siUnits : usUnits);
			String scale = units.values().iterator().next();
			double value = 0;
			
			for (int pass = 0; pass < 2; pass++) {
				// each pass through, we calculate a number of positional
				// arguments based on the rulerWidth, scale, and value to
				// display
				FontRenderer fr = getFontBox(MessageFormat.format("{0,number,#.#} {1}", value, scale));
				
				// box to draw in will be at least as wide as it takes to draw our test label,
				// and as tall as the font box, the ruler, and some space between them
				Dimension font = fr.getPreferredSize();
				Dimension ruler = new Dimension(rulerWidth, Math.max(rulerHeight, tickHeight));
				Dimension box = new Dimension(ruler.width, ruler.height + font.height);
				
				// confine the offset to keep the corner closest to the edge within the screen
				int constraintX = getWidth()-box.width;
				parms.offsetX = Math.max(-constraintX, Math.min(constraintX, parms.offsetX));
				int constraintY = getHeight()-box.height;
				parms.offsetY = Math.max(-constraintY, Math.min(constraintY, parms.offsetY));
				
				int left, right, bottom, top;
				if (parms.offsetX < 0) {
					right = getWidth() + parms.offsetX;
					left = right - box.width;
				} else {
					left = parms.offsetX;
					right = left + box.width;
				}
				if (parms.offsetY < 0) {
					bottom = getHeight() + parms.offsetY;
					top = bottom - box.height;
				} else {
					top = parms.offsetY;
					bottom = top + box.height;
				}
				lastBox = new Rectangle((int)left, (int)top, (int)(right-left), (int)(bottom-top+1));
				
				double km = 0;
				final int count = 10;
				HVector prior = null;
				for (int i = 0; i < count; i++) {
					int x = (int)Math.round(lastBox.getMinX() + i*lastBox.width/(count-1));
					HVector current = HVector.intersectMars(HVector.ORIGIN, getProj().screen.toHVector(x, getHeight()/2));
					if (prior != null) {
						km += current.sub(prior).norm();
					}
					prior = current;
				}
				
				if (pass == 0) {
					// the first pass through, we update scale, value, and rulerWidth
					for (double s: units.keySet()) {
						scale = units.get(s);
						double base = km * s;
						if (base < 1) continue;
						int digits = (int)Math.floor(Math.log(base)/Math.log(10));
						double[] breaks = {1.0, 2.5, 5.0, 7.5, 10.0};
						int idx = Arrays.binarySearch(breaks, base * Math.pow(10, -digits));
						if (idx < 0) {
							idx = -idx - 1;
						}
						double base10value = Math.pow(10, digits);
						double smallest = base10value * breaks[Math.max(0,idx-1)];
						double nearest = base10value * breaks[Math.min(breaks.length-1,idx)];
						if (Math.abs(nearest-base) > Math.abs(smallest-base)) {
							nearest = smallest;
						}
						double largeWidth = nearest / base * lastBox.width;
						if (parms.offsetX < 0) {
							if (lastBox.getMaxX() - largeWidth < 0) {
								value = smallest;
							} else {
								value = nearest;
							}
						} else {
							if (lastBox.getMinX() + largeWidth >= getWidth()-1) {
								value = smallest;
							} else {
								value = nearest;
							}
						}
						rulerWidth = (int)Math.ceil(value / base * lastBox.width);
						if (value >= 1) {
							break;
						}
					}
				} else {
					// the second pass through, the positions are updated with
					// new scale/value/rulerWidth values, so draw
					clearOffScreen();
					
					// draw ruler
					g2.setColor(parms.barColor);
					Rectangle rulerBox = new Rectangle((int)left, (int)top + tickHeight - rulerHeight, (int)(right-left), rulerHeight);
					g2.fill(rulerBox);
					
					//set the ruler on the layer so it can
					// be used by the high res export
					myLayer.setRulerBox(rulerBox);
					
					// draw ticks
					g2.setColor(parms.tickColor);
					//clear tickboxes on the layer
					myLayer.clearTickBoxes();
					for (int i = 0; i < parms.numberOfTicks && parms.numberOfTicks >= 2; i++) {
						int x = lastBox.x + lastBox.width * i/(parms.numberOfTicks-1) - 1;
						Rectangle tick = new Rectangle(x, (int)top, 2, tickHeight);
						g2.fill(tick);
						//add the tick to the layer
						myLayer.addTickBox(tick);
					}
					
					if (Util.between(-90, getProj().screen.toWorld(getWidth()/2, getHeight()/2).getY(), 90)) {
						// draw font
						int x, y;
						int labelWidth = fr.getPreferredSize().width;
						switch (parms.h_alignment) {
						case Center:
							x = (left+right)/2-labelWidth/2;
							break;
						case Right:
							x = right - labelWidth;
							break;
						case Left:
						default:
							x = left;
							break;
						}
						switch (parms.v_alignment) {
						case Above:
							y = top - font.height;
							break;
						case Below:
							y = bottom - font.height;
							break;
						default:
							y = top - font.height;
							break;
						}
						g2.translate(x, y);
						fr.paintComponent(g2);
						//set the font string on the layer
						myLayer.setFontString(fr.getLabel());
					} else {
						clearOffScreen();
						g2.setColor(parms.barColor);
						g2.draw(lastBox);
					}
				}
			}
		} finally {
			// ensure that no g2's are left around
			if (g2 != null) {
				g2.dispose();
			}
			
			// call the LView painter to stack the back buffers together
			super.paintComponent(g);
		}
	}
	
	public String getName() {
		return "Map Scalebar";
	}

	public FocusPanel getFocusPanel() {
		if (focusPanel == null){
			focusPanel = new ScalePanel(this);
			return focusPanel;
			}
		return focusPanel;
	}
	
	private enum UnitStrings {
		Metric("SI (km/m)", true),
		Imperial("Imperial (mi/ft)", false);
		
		private final String text;
		private final boolean metric;
		public String toString() {
			return text;
		}
		public boolean isMetric() {
			return metric;
		}
		UnitStrings(String text, boolean metric) {
			this.text = text;
			this.metric = metric;
		}
	}
	
	private class ScalePanel extends FocusPanel implements ActionListener, ChangeListener {
		private static final long serialVersionUID = 3142320762007483904L;
		JButton modFont;
		ColorButton barColorButton;
		ColorButton tickColorButton;
		JComboBox units;
		JSlider tickNumberSlider;
		JSlider widthSlider;
		FontRenderer frSample;
		
		private GridBagConstraints gbc(int row, boolean isData) {
			int px = 4, py = 4;
			Insets in = new Insets(py,px,py,px);
			return new GridBagConstraints(isData?1:0, row, 1,1, isData?1:0, 0, GridBagConstraints.WEST, isData?GridBagConstraints.HORIZONTAL:GridBagConstraints.NONE,in,px,py);
		}
		
		public ScalePanel(ScaleLView slv) {
			super(slv, true);		
			
			
	// The Scalebar Adjustments tab on the focus panel consists of two titlebordered panels,
	// the first containing the label options, the second containing the ruler options
			
	// Label options panel		
			JPanel labelOptions = new JPanel();
			labelOptions.setBorder(BorderFactory.createTitledBorder("Label Options"));
			labelOptions.setLayout(new GridBagLayout());
		
			int row = 0;
		//Add space at the top of options panel	
			labelOptions.add(Box.createGlue(), gbc(row, false));
			labelOptions.add(Box.createGlue(), gbc(row++, true));
		//Add units label and dropdown menu to options panel	
			units = new JComboBox(UnitStrings.values());
			units.addActionListener(this);
			units.setSelectedItem(parms.isMetric ? UnitStrings.Metric : UnitStrings.Imperial);
			labelOptions.add(new JLabel("Label Units"), gbc(row, false));
			labelOptions.add(units, gbc(row++, true));		
			//Create font button and example and place on font panel	
			if (parms.labelFont == null) {
				parms.labelFont = getFont().deriveFont(100f);
			}
			frSample = new FontRenderer(parms.labelFont, parms.fontOutlineColor, parms.fontFillColor);
			frSample.setLabel("1234 km/mi");
			frSample.setAntiAlias(true);
				int pad = 4;
				Insets in = new Insets(pad,pad,pad,pad);
			
			modFont = new JButton("Choose Font...".toUpperCase());
			modFont.addActionListener(this);
			JPanel font = new JPanel(new BorderLayout(4,0));
			font.add(modFont, BorderLayout.WEST);
			font.add(frSample, BorderLayout.CENTER);
		//Add font label and font panel onto options panel	
			labelOptions.add(new JLabel("Label Font"), gbc(row, false));
			labelOptions.add(font, gbc(row++, true));
		//Add alignment label to options panel
			labelOptions.add(new JLabel("Label Alignment"), gbc(row, false));
			//Create vertical alignment radio dial group
			ButtonGroup alignGroup_v= new ButtonGroup();
			Box alighnBox_v = Box.createHorizontalBox();
			ScaleParameters.VerticalAlignment[] alignTypes_v = ScaleParameters.VerticalAlignment.values();
			for (int j = 0; j < alignTypes_v.length; j++) {
				final JRadioButton choice1 = new JRadioButton(alignTypes_v[j].label);
				alignGroup_v.add(choice1);
				if (j > 0) {
					alighnBox_v.add(Box.createHorizontalStrut(2));
				}
				alighnBox_v.add(choice1);
				final ScaleParameters.VerticalAlignment b = alignTypes_v[j];
				if (b.equals(parms.v_alignment)) {
					choice1.setSelected(true);
				}
				choice1.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// ignore disable events caused by the button group
						if (choice1.isSelected()) {
							parms.v_alignment = b;
							ScaleLView.this.repaint();
						}
					}
				});
			}
		//Add vertical alignment radio dial group to options panel	
			labelOptions.add(alighnBox_v, gbc(row++, true));	
			//Create horizontal alignment radio dial group	
			ButtonGroup alignGroup_h = new ButtonGroup();
			Box alignBox_h = Box.createHorizontalBox();
			ScaleParameters.HorizontalAlignment[] alignTypes_h = ScaleParameters.HorizontalAlignment.values();
			for (int i = 0; i < alignTypes_h.length; i++) {
				final JRadioButton choice = new JRadioButton(alignTypes_h[i].label);
				alignGroup_h.add(choice);
				if (i > 0) {
					alignBox_h.add(Box.createHorizontalStrut(2));
				}
				alignBox_h.add(choice);
				final ScaleParameters.HorizontalAlignment a = alignTypes_h[i];
				if (a.equals(parms.h_alignment)) {
					choice.setSelected(true);
				}
				choice.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// ignore disable events caused by the button group
						if (choice.isSelected()) {
							parms.h_alignment = a;
							ScaleLView.this.repaint();
						}
					}
				});
			}
		//Add horizontal alignment radio dial group to options panel	
			labelOptions.add(alignBox_h, gbc(row++, true));
			
			

	// Ruler options panel		
			JPanel rulerOptions = new JPanel();
			rulerOptions.setLayout(new GridBagLayout());
			rulerOptions.setBorder(BorderFactory.createTitledBorder("Ruler Options"));
			
			int row2 = 0;
		//Add space at the head of panel	
			rulerOptions.add(Box.createGlue(), gbc(row2, false));
			rulerOptions.add(Box.createGlue(), gbc(row2++, true));
		//Add the ruler width title and slider	
			rulerOptions.add(new JLabel("Ruler Length %"), gbc(row2, false));
			widthSlider = new JSlider(0, 100, parms.width);
			widthSlider.setPaintTicks(true);
			widthSlider.setPaintLabels(true);
			widthSlider.setMajorTickSpacing(25);
			widthSlider.setPaintTicks(true);
			widthSlider.addChangeListener(this);
			rulerOptions.add(widthSlider, gbc(row2++, true));
		//Add the ruler color title and options box	
			rulerOptions.add(new JLabel("Ruler Color"), gbc(row2, false));
			barColorButton = new ColorButton("Ruler Color", parms.barColor);
			barColorButton.addPropertyChangeListener(colorListener);
			rulerOptions.add(barColorButton, gbc(row2++, true));
		//Add the tick count title and slider	
			rulerOptions.add(new JLabel("Tick Count"), gbc(row2, false));
			if(parms!=null && parms.numberOfTicks>25){
				parms.numberOfTicks=25;
			}
			tickNumberSlider = new JSlider(0, 25, parms.numberOfTicks);
			tickNumberSlider.setPaintLabels(true);
			tickNumberSlider.setMajorTickSpacing(5);
			tickNumberSlider.setPaintTicks(true);
			tickNumberSlider.addChangeListener(this);
			rulerOptions.add(tickNumberSlider, gbc(row2++, true));
		//Add the tick color title and slider	
			rulerOptions.add(new JLabel("Tick Color"), gbc(row2, false));
			tickColorButton = new ColorButton("Tick Color", parms.tickColor);
			tickColorButton.addPropertyChangeListener(colorListener);
			rulerOptions.add(tickColorButton, gbc(row2++,true));
		//Add bottom buffer	
			GridBagConstraints endCap = gbc(row2, true);
			endCap.weighty = 1;
			rulerOptions.add(new JLabel(""), endCap);
			
			
	// Put both panels into a body panel to set background color and spacing	
			JPanel body = new JPanel();
			body.setLayout(new BoxLayout(body, BoxLayout.PAGE_AXIS));
			body.setBorder(new EmptyBorder(10, 5, 5, 5));			
			body.add(labelOptions);
			body.add(Box.createRigidArea(new Dimension(0, 15)));
			body.add(rulerOptions);
		//Put body panel inside a scrollPane	
			JPanel sbOptions = new JPanel(new BorderLayout());
			sbOptions.add(body, BorderLayout.NORTH);		
			JScrollPane sbPane = new JScrollPane(sbOptions, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	//Add the scalebar scrollpane as a focus panel tab		
			addTab("Scalebar Options".toUpperCase(), sbPane);
			
		}

		private PropertyChangeListener colorListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				Object source = evt.getSource();
				if (source == tickColorButton) {
					Color newColor = tickColorButton.getColor();
					if (newColor != null) {
						parms.tickColor = newColor;
						tickColorButton.setBackground(parms.tickColor);
						ScaleLView.this.repaint();
					}
				} else if (source == barColorButton) {
					Color newColor = barColorButton.getColor();
					if (newColor != null) {
						parms.barColor = newColor;
						barColorButton.setBackground(parms.barColor);
						ScaleLView.this.repaint();
					}
				}
			}
		};
		
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == modFont) {
				JFontChooser jfc = new JFontChooser(parms.labelFont, parms.fontOutlineColor, parms.fontFillColor);
				if (null != jfc.showDialog(modFont)) {
					parms.labelFont = jfc.selectedFont;
					parms.fontFillColor = jfc.getFontColor();
					parms.fontOutlineColor = jfc.getOutlineColor();
					frSample.setOutlineColor(parms.fontOutlineColor);
					frSample.setForeground(parms.fontFillColor);
					frSample.setFont(parms.labelFont);
					ScaleLView.this.repaint();
				}
			} else if (e.getSource() == units) {
				JComboBox object = (JComboBox) e.getSource();

				boolean last = parms.isMetric;
				parms.isMetric = UnitStrings.values()[object.getSelectedIndex()].isMetric();
				if (last != parms.isMetric) {
					ScaleLView.this.repaint();
				}
			}
		}
		
		public void stateChanged(ChangeEvent e) {
			if (e.getSource() == tickNumberSlider) {
				parms.numberOfTicks = tickNumberSlider.getValue();
			} else if (e.getSource() == widthSlider) {
				parms.width = widthSlider.getValue();
			}
			ScaleLView.this.repaint();
		}
	}
	
	protected LView _new() {
		return new ScaleLView(false, (ScaleLayer)getLayer(), parms, null);
	}
	
	protected Object createRequest(Rectangle2D where) {
		repaint();
		return null;
	}
	
	public void receiveData(Object layerData) {

	}
	
	public void mouseDragged(MouseEvent e) {
		if (lastBox != null) {
			Point p = e instanceof WrappedMouseEvent ? ((WrappedMouseEvent)e).getRealPoint() : e.getPoint();
			
			Rectangle box = new Rectangle(lastBox);
			box.x = p.x - mouseOffset.x;
			box.y = p.y - mouseOffset.y;
			box = box.intersection(new Rectangle(0,0,getWidth()-1,getHeight()-1));
			
			if (box.x < getWidth() - box.getMaxX()) {
				parms.offsetX = box.x;
			} else {
				parms.offsetX = (int)(box.getMaxX() - getWidth());
			}
			if (box.y < getHeight() - box.getMaxY()) {
				parms.offsetY = box.y;
			} else {
				parms.offsetY = (int)(box.getMaxY() - getHeight());
			}
			
			repaint();
		}
	}
	
	public void mousePressed(MouseEvent e) {
		if (lastBox != null) {
			mouseOffset = e instanceof WrappedMouseEvent ? ((WrappedMouseEvent)e).getRealPoint() : e.getPoint();
			mouseOffset.x -= lastBox.x;
			mouseOffset.y -= lastBox.y;
		}
	}
	
	public void mouseReleased(MouseEvent e) {
		mouseOffset = null;
		repaint();
	}
	
	public void mouseMoved(MouseEvent e) {
		// not used
	}
	
	public void mouseClicked(MouseEvent e) {
		// not used
	}

	public void mouseEntered(MouseEvent e) {
		// not used
	}

	public void mouseExited(MouseEvent e) {
		// not used
	}
	
//The following two methods are used to query for the
// info panel fields (description, citation, etc)	
 	public String getLayerKey(){
 		return "Map Scalebar";
 	}
 	public String getLayerType(){
 		return "scalebar";
 	}
 	
 	@Override
    public boolean pannerStartEnabled()
    {
        return false;
    }
    
}
