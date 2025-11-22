package edu.asu.jmars.layer.shape2;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.jdesktop.swingx.combobox.ListComboBoxModel;
import edu.asu.jmars.Main;
import edu.asu.jmars.graphics.JFontChooser;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.Style;
import edu.asu.jmars.layer.util.features.StyleFieldSource;
import edu.asu.jmars.layer.util.features.StyleGlobalSource;
import edu.asu.jmars.layer.util.features.StyleSource;
import edu.asu.jmars.layer.util.features.Styles;
import edu.asu.jmars.swing.ColorButton;
import edu.asu.jmars.swing.FillStyleButton;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.FillStyle;
import edu.asu.jmars.util.LineType;
import edu.asu.jmars.util.Util;

// TODO: add support for Font and FPath types (geometry style could be path, centroid(path), buffer(path), etc.)

public class StyleEditor {
	private static DebugLog log = DebugLog.instance();
	
	/**
	 * Shows a modal dialog that the user can use to edit style field sources and defaults.
	 * 
	 * A special field source is reserved for 'no field source', aka a global or what used to be considered an override value.
	 * 
	 * A special field source is reserved for creating new style columns within the editor, a huge convenience.
	 * 
	 * @param styles The styles object to modify.
	 * @param schema The feature collection schema to modify.
	 * @return true if the styles were modified, false if nothing was changed.
	 */
	@SuppressWarnings({ "unchecked", "serial" })
	public Set<Style<?>> showStyleEditor(Styles styles, List<Field> schema) {
		// create header of table and separator between header and styles
		JPanel itemPanel = new JPanel(new GridBagLayout());
		itemPanel.setBorder(new EmptyBorder(8,8,8,8));
		int row = 0;
		Insets in = new Insets(4,4,4,4);
		GridBagConstraints gbc = new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.NONE,in,0,0);
		gbc.gridx = 0;
		itemPanel.add(new JLabel("Name"), gbc);
		gbc.gridx = 1;
		itemPanel.add(new JLabel("Use Column"), gbc);
		gbc.gridx = 2;
		itemPanel.add(new JLabel("Default Value"), gbc);
		row ++;
		itemPanel.add(new JSeparator(), new GridBagConstraints(0,row++,3,1,0,1,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,in,0,0));
		
		// create dialog and button to close it, using 'usedOkay' to see if it accepted or not
		final JDialog d = new JDialog((Frame)null, "Style Settings...", true);
		Util.addEscapeAction(d);
		
		final boolean[] usedOkay = {false};
		
		final JButton okay = new JButton("Save".toUpperCase());
		okay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.setVisible(false);
				usedOkay[0] = true;
			}
		});
		
		final JButton cancel = new JButton("Cancel".toUpperCase());
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.setVisible(false);
			}
		});
		
		final String globalItem = "<use default>";
		final String createItem = "<create column...>";
		
		// used to determine what the user changed without going digging through JComponents
		final Map<Style<?>,Object> oldFields = new HashMap<Style<?>,Object>();
		final Map<Style<?>,Object> oldDefaults = new HashMap<Style<?>,Object>();
		final Map<Style<?>,Object> newFields = new HashMap<Style<?>,Object>();
		final Map<Style<?>,Object> newDefaults = new HashMap<Style<?>,Object>();
		
		// create a row for each style
		for (final Style<? extends Object> s: styles.getStyles()) {
			final StyleSource<?> source = s.getSource();
			final Object defaultValue = source.getValue(null);
			
			// get the name, field, and default value
			String styleName = s.getName();
			Object styleField;
			Object styleDefault;
			if (source instanceof StyleFieldSource<?>) {
				StyleFieldSource<?> fieldSource = (StyleFieldSource<?>)source;
				styleField = fieldSource.getFields().iterator().next();
				styleDefault = fieldSource.getValue(null);
			} else if (source instanceof StyleGlobalSource<?>) {
				StyleGlobalSource<?> orideSource = (StyleGlobalSource<?>)source;
				styleField = globalItem;
				styleDefault = orideSource.getValue(null);
			} else {
				// unrecognized style handler, show it as such
				styleField = null;
				styleDefault = null;
			}
			
			// the field combo is only enabled for recognized StyleSources
			final List<Object> items = new ArrayList<Object>();
			items.add(globalItem);
			if (styleDefault != null) {
				// if we have a default value, we could create a column on the fly with its type
				items.add(createItem);
			}
			
			if (styleDefault != null) {
				Class<?> styleClass = styleDefault.getClass();
				Class<?>[] typeMatches = {Number.class, String.class, Color.class, LineType.class, FillStyle.class};
				for (Field f: schema) {
					boolean ok = styleDefault.getClass().isAssignableFrom(f.type);
					for (Class<?> match: typeMatches) {
						if (ok) {
							break;
						}
						if (match.isAssignableFrom(styleClass) && match.isAssignableFrom(f.type)) {
							ok = true;
						}
					}
					if (ok) {
						items.add(f);
					}
				}
			}
			
			final JComboBox cbSource = new JComboBox(new ListComboBoxModel<Object>(items));
			if (styleField == null) {
				cbSource.setSelectedIndex(0);
				cbSource.setEnabled(false);
			} else {
				cbSource.setSelectedItem(styleField);
			}
			
			final Class<?> type = styleDefault == null ? null : styleDefault.getClass();
			cbSource.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Object sel = cbSource.getSelectedItem();
					if (sel == createItem) {
						SwingUtilities.invokeLater(new Runnable() {
							// invoke later to let action handlers finish their work
							public void run() {
								String name = Util.showInputDialog("Enter new column name:",null);
								if (name != null) {
									Field f = new Field(name, type);
									items.add(f);
									cbSource.setSelectedItem(f);
								} else {
									cbSource.setSelectedItem(newFields.get(s));
								}
							}
						});
					} else {
						newFields.put(s, cbSource.getSelectedItem());
					}
				}
			});
			
			// the 'default' value is parsed every time a change is made and okay
			// is disabled if the value is invalid
			Component comp;
			if (styleDefault == null) {
				log.println("Editor is ignoring uneditable source " + source.getClass().getName());
				continue;
			} else if (s == styles.labelFont) {
				final JComboBox cb = new JComboBox(JFontChooser.fonts);
				cb.setSelectedItem(s.getValue(null));
				cb.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						newDefaults.put(s, cb.getSelectedItem().toString());
					}
				});
				comp = cb;
			} else if (s == styles.labelStyle) {
				final JComboBox cb = new JComboBox(JFontChooser.fontStyles);
				cb.setSelectedItem(s.getValue(null));
				cb.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						newDefaults.put(s, cb.getSelectedItem().toString());
					}
				});
				comp = cb;
			} else if (s == styles.labelSize) {
				final JComboBox cb = new JComboBox(JFontChooser.sizes);
				cb.setSelectedItem(""+s.getValue(null));
				cb.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						newDefaults.put(s, Integer.valueOf(cb.getSelectedItem().toString()));
					}
				});
				comp = cb;
			} else if (String.class.isInstance(defaultValue)) {
				final JTextField txt = new JTextField(styleDefault.toString());
				txt.getDocument().addDocumentListener(new DocumentListener() {
					public void changedUpdate(DocumentEvent e) {change();}
					public void insertUpdate(DocumentEvent e) {change();}
					public void removeUpdate(DocumentEvent e) {change();}
					private void change() {
						newDefaults.put(s, txt.getText().trim());
					}
				});
				comp = txt;
			} else if (Number.class.isInstance(defaultValue)) {
				final JTextField txt = new JTextField(styleDefault.toString());
				final Color orgBG = txt.getBackground();
				txt.getDocument().addDocumentListener(new DocumentListener() {
					public void changedUpdate(DocumentEvent e) {change();}
					public void insertUpdate(DocumentEvent e) {change();}
					public void removeUpdate(DocumentEvent e) {change();}
					private void change() {
						try {
							if (defaultValue.getClass().isAssignableFrom(Integer.class)) {
								newDefaults.put(s, Integer.parseInt(txt.getText()));
							} else if (defaultValue.getClass().isAssignableFrom(Float.class)) {
								newDefaults.put(s, Float.parseFloat(txt.getText()));
							} else if (defaultValue.getClass().isAssignableFrom(Double.class)) {
								newDefaults.put(s, Double.parseDouble(txt.getText()));
							} else {
								throw new IllegalStateException("Unsupported number type");
							}
							okay.setEnabled(true);
							txt.setBackground(orgBG);
						} catch (Exception ex) {
							okay.setEnabled(false);
							txt.setBackground(Color.pink);
						}
					}
				});
				comp = txt;
			} else if (Boolean.class.isInstance(defaultValue)) {
				final JCheckBox cb = new JCheckBox("Enable", ((Boolean)defaultValue).booleanValue());
				cb.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						newDefaults.put(s, cb.isSelected());
					}
				});
				comp = cb;
			} else if (LineType.class.isInstance(defaultValue)) {
				final Object[] options = {
					LineType.PATTERN_ID_NULL,
					LineType.PATTERN_ID_2_2,
					LineType.PATTERN_ID_6_3,
					LineType.PATTERN_ID_12_3_3_3,
					LineType.PATTERN_ID_3_3,
					LineType.PATTERN_ID_1_6,
					LineType.PATTERN_ID_3_9,
					LineType.PATTERN_ID_3_6,
					LineType.PATTERN_ID_12_3,
					LineType.PATTERN_ID_8_3_8_3,
					LineType.PATTERN_ID_10_5_5_10,
					LineType.PATTERN_ID_30_4,
				};
				final JComboBox cb = new JComboBox(options);
				cb.setSelectedItem(((LineType)defaultValue).getType());
				cb.setRenderer(new DefaultListCellRenderer() {
					private LineType type;
					public void paintComponent(Graphics g) {
						if (type == null) {
							super.paintComponent(g);
						} else {
							Graphics2D g2 = (Graphics2D)g;
							Dimension d = getSize();
							g2.setBackground(getBackground());
							g2.clearRect(0, 0, d.width, d.height);
							BasicStroke st = new BasicStroke(
								2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
								type.getDashPattern(), 0.0f);
							g2.setStroke(st);
							g2.setColor(getForeground());
							g2.draw(new Line2D.Double(0,d.height/2, d.width, d.height/2));
						}
					}
					public Component getListCellRendererComponent(JList list, Object value, final int index, boolean isSelected, boolean cellHasFocus) {
						type = new LineType((Integer)value);
						return super.getListCellRendererComponent(list, defaultValue, index, isSelected, cellHasFocus);
					}
				});
				cb.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						LineType lt = new LineType((Integer)options[cb.getSelectedIndex()]);
						newDefaults.put(s, lt);
					}
				});
				comp = cb;
			} else if (FillStyle.class.isInstance(defaultValue)) {
				Box h = Box.createHorizontalBox();
				FillStyle style = (FillStyle)styleDefault;
				final FillStyleButton fsb = new FillStyleButton(style);
				// We're not actually changing the "background", but trying to use "style" didn't trigger properly
				fsb.addPropertyChangeListener("background", new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						FillStyle fs = fsb.getStyle();
						newDefaults.put(s, fs);
					}
				});
				h.add(fsb);
				h.add(Box.createHorizontalGlue());
				comp = h;
			} else if (Color.class.isInstance(defaultValue)) {
				Box h = Box.createHorizontalBox();
				Color col = (Color)styleDefault;
				final ColorButton cb = new ColorButton("Color...", col, true);
				cb.setToolTipText(col.getAlpha()*100/255 + "% opaque");
				cb.addPropertyChangeListener("background", new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						Color col = cb.getBackground();
						newDefaults.put(s, col);
						cb.setToolTipText(col.getAlpha()*100/255 + "% opaque");
					}
				});
				h.add(cb);
				h.add(Box.createHorizontalGlue());
				comp = h;
			} else {
				log.println("Editor is skipping unrecognized type " + defaultValue.getClass().getName());
				continue;
			}
			
			oldFields.put(s, styleField);
			oldDefaults.put(s, styleDefault);
			
			if (s == styles.pointSize || s == styles.drawOutlines  || s == styles.fillPolygons || s == styles.showLabels) {
				itemPanel.add(new JSeparator(), new GridBagConstraints(0,row++,3,1,0,1,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,in,0,0));
			}
			
			gbc.gridy = row++;
			gbc.gridx = 0;
			itemPanel.add(new JLabel(styleName), gbc);
			gbc.gridx = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			itemPanel.add(cbSource, gbc);
			gbc.weightx = 0;
			gbc.gridx = 2;
			itemPanel.add(comp, gbc);
			gbc.fill = GridBagConstraints.NONE;
		}
		
		// initialize new fields and defaults to old ones
		newFields.putAll(oldFields);
		newDefaults.putAll(oldDefaults);
		
		// create outer gui elements
		Box h = Box.createHorizontalBox();
		h.add(Box.createHorizontalStrut(4));
		JButton help = new JButton("Help".toUpperCase());
		help.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Util.showMessageDialogObj(
					new String[]{
						"Each style can either be a fixed global value, or can vary from feature to feature.",
						"\nFor example:",
						"\n",
						"To fill all polygons, next to the 'fill polygon' style, choose '" + globalItem + "'' and check the 'enable' box.",
						"\n",
						"To fill polygons for features where the 'fill' column is true, choose 'fill' from the fields list.",
						"The default value will be used when a feature does not have a value in the 'fill' column."
					},
					"Style Settings Help", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		h.add(help);
		h.add(Box.createHorizontalStrut(4));
		h.add(Box.createHorizontalGlue());
		h.add(Box.createHorizontalStrut(4));
		h.add(cancel);
		h.add(Box.createHorizontalStrut(4));
		h.add(okay);
		h.setBorder(new EmptyBorder(8,8,8,8));
		JPanel content = new JPanel(new BorderLayout());
		content.add(h, BorderLayout.SOUTH);
		content.add(new JScrollPane(itemPanel), BorderLayout.CENTER);
		d.getContentPane().add(content);
		
		GraphicsConfiguration gc = Main.mainFrame.getGraphicsConfiguration(); //get display this window is on
		DisplayMode dm  = gc.getDevice().getDisplayMode();
		int screenHeight = dm.getHeight();
		d.pack();	
		int contentHeight = d.getHeight();
		int littleExtra = (int)(screenHeight * 10.0 / 100.0);
		if (contentHeight > (screenHeight - littleExtra)) { //adjust height to fit into screen
			d.setPreferredSize(new Dimension(d.getWidth(), (int) (screenHeight * 70.0 / 100.0)));	
			d.pack();  //re-pack; without it, new dim will not be effective
		}
		
		d.setLocationRelativeTo(Main.mainFrame);
		d.setVisible(true);
		
		// if dialog returned because okay was not hit, get out now
		if (!usedOkay[0]) {
			return Collections.emptySet();
		}
		
		// otherwise go through the styles that were deemed editable, update
		// what the user changed, and return a set of those changed styles
		Set<Style<?>> changed = new HashSet<Style<?>>();
		for (Style<?> s: oldFields.keySet()) {
			Object oldField = oldFields.get(s);
			Object newField = newFields.get(s);
			Object oldDefault = oldDefaults.get(s);
			Object newDefault = newDefaults.get(s);
			if (!oldField.equals(newField) || !oldDefault.equals(newDefault)) {
				changed.add(s);
				log.println("Settings change: " + oldField + ", " + newField + ", " + oldDefault + ", " + newDefault);
				if (newField == globalItem) {
					s.setConstant(newDefault);
				} else {
					s.setSource((Field)newField, newDefault);
				}
			}
		}
		
		return changed;
	}
}
