package edu.asu.jmars.layer.util.features;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import org.jdesktop.swingx.combobox.ListComboBoxModel;
import org.jdesktop.swingx.combobox.MapComboBoxModel;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.shape2.ColumnEditor;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.swing.ColorInterp;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.util.FillStyle;
import edu.asu.jmars.util.LineType;
import edu.asu.jmars.util.ListType;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.stable.ColorCellEditor;
import edu.asu.jmars.util.stable.ColorCellRenderer;
import edu.asu.jmars.util.stable.FillStyleCellEditor;
import edu.asu.jmars.util.stable.FillStyleTableCellRenderer;
import edu.asu.jmars.util.stable.LineTypeCellEditor;
import edu.asu.jmars.util.stable.LineTypeTableCellRenderer;
import edu.asu.jmars.util.stable.ListTypeCellEditor;


/** Describes a value lookup table and methods to pass a value through the table. */
public final class LookupTable<E> extends CalculatedField {
	/** Defines a lookup table operation. Implementations should implement hashCode() and equals() based on type. */
	public static interface Operation<E> extends Serializable {
		public String getName();
		public E lookup(LookupTable<E> table, Comparable<Object> value);
	}
	public static final <E> int getPos(LookupTable<E> table, Comparable<Object> value) {
		int pos = Collections.binarySearch(table.breakPoints, value);
		if (pos < 0) {
			pos = -pos - 1;
		}
		return pos;
	}
	public static final class Breakpoints<E> implements Operation<E> {
		private static final long serialVersionUID = 1L;
		public String getName() {
			return "Breakpoints";
		}
		public E lookup(LookupTable<E> table, Comparable<Object> value) {
			int pos = Collections.binarySearch(table.breakPoints, value);
			if (pos >= 0) {
				return table.values.get(pos);
			} else {
				pos = -pos - 1;
				if (pos > 0) {
					return table.values.get(pos-1);
				} else {
					return null;
				}
			}
		}
		public String toString() {
			return getName();
		}
		public int hashCode() {
			return getName().hashCode();
		}
		public boolean equals(Object o) {
			return o instanceof Operation<?> && ((Operation<?>)o).getName().equals(getName());
		}
	}
	public static final class Lookup<E> implements Operation<E> {
		private static final long serialVersionUID = 1L;
		public String getName() {
			return "Lookup";
		}
		public E lookup(LookupTable<E> table, Comparable<Object> value) {
			int pos = Collections.binarySearch(table.breakPoints, value);
			return pos >= 0 ? table.values.get(pos) : null;
		}
		public String toString() {
			return getName();
		}
		public int hashCode() {
			return getName().hashCode();
		}
		public boolean equals(Object o) {
			return o instanceof Operation<?> && ((Operation<?>)o).getName().equals(getName());
		}
	}
	public static final class Interpolate<E> implements Operation<E> {
		private static final long serialVersionUID = 1L;
		private final Interp<E> interp;
		public Interpolate(Interp<E> interp) {
			this.interp = interp;
		}
		public String getName() {
			return interp.getName() + " Interpolation";
		}
		public E lookup(LookupTable<E> table, Comparable<Object> value) {
			int pos = Collections.binarySearch(table.breakPoints, value);
			if (pos >= 0) {
				return table.values.get(pos);
			} else {
				pos = -pos - 1;
				if (pos == 0 || pos == table.breakPoints.size()) {
					return null;
				} else if (value instanceof Number) {
					double n1 = ((Number)table.breakPoints.get(pos-1)).doubleValue();
					double n2 = ((Number)table.breakPoints.get(pos)).doubleValue();
					double v = ((Number)value).doubleValue();
					return interp.interp(table.values.get(pos-1), table.values.get(pos), (v-n1)/(n2-n1));
				} else {
					return null;
				}
			}
		}
		public String toString() {
			return getName();
		}
		public int hashCode() {
			return getName().hashCode();
		}
		public boolean equals(Object o) {
			return o instanceof Operation<?> && ((Operation<?>)o).getName().equals(getName());
		}
	}
	
	public static interface Interp<E> extends Serializable {
		String getName();
		E interp(E item1, E item2, double percent);
	}
	private static final Interp<Number> numberInterp = new Interp<Number>() {
		private static final long serialVersionUID = 1L;
		public String getName() {
			return "Number";
		}
		public Number interp(Number item1, Number item2, double percent) {
			double n1 = ((Number)item1).doubleValue();
			double n2 = ((Number)item2).doubleValue();
			return n1*(1-percent) + n2*percent;
		}
	};
	public static final class ByteInterpolator implements Interp<Byte> {
		private static final long serialVersionUID = 1L;
		public String getName() {
			return "Byte";
		}
		public Byte interp(Byte item1, Byte item2, double percent) {
			return numberInterp.interp(item1, item2, percent).byteValue();
		}
	};
	public static final class ShortInterpolator implements Interp<Short> {
		private static final long serialVersionUID = 1L;
		public String getName() {
			return "Short";
		}
		public Short interp(Short item1, Short item2, double percent) {
			return numberInterp.interp(item1, item2, percent).shortValue();
		}
	}
	public static final class IntInterpolator implements Interp<Integer> {
		private static final long serialVersionUID = 1L;
		public String getName() {
			return "Integer";
		}
		public Integer interp(Integer item1, Integer item2, double percent) {
			return numberInterp.interp(item1, item2, percent).intValue();
		}
	}
	public static final class FloatInterpolator implements Interp<Float> {
		private static final long serialVersionUID = 1L;
		public String getName() {
			return "Float";
		}
		public Float interp(Float item1, Float item2, double percent) {
			return numberInterp.interp(item1, item2, percent).floatValue();
		}
	}
	public static final class DoubleInterpolator implements Interp<Double> {
		private static final long serialVersionUID = 1L;
		public String getName() {
			return "Double";
		}
		public Double interp(Double item1, Double item2, double percent) {
			return numberInterp.interp(item1, item2, percent).doubleValue();
		}
	}
	public static final class ColorInterpolator implements Interp<Color> {
		private static final long serialVersionUID = 1L;
		private ColorInterp interp;
		public ColorInterpolator(ColorInterp interp) {
			this.interp = interp;
		}
		public String getName() {
			return interp.getTitle() + " Color";
		}
		public Color interp(Color item1, Color item2, double percent) {
			Color out = interp.mixColor(item1, item2, (float)percent);
			if (item1.getAlpha() != 255 || item2.getAlpha() != 255) {
				int alpha = (int)(item1.getAlpha()*(1-percent) + item2.getAlpha()*percent);
				out = new Color(out.getRed(), out.getGreen(), out.getBlue(), alpha);
			}
			return out;
		}
	}
	
	private static final long serialVersionUID = 1L;
	private final List<Comparable<Object>> breakPoints = new ArrayList<Comparable<Object>>();
	private final List<E> values = new ArrayList<E>();
	private final Field source;
	private Operation<E> op = new Lookup<E>();
	
	public LookupTable(Field source, Class<?> targetType) {
		super("Lookup Table", targetType);
		this.source = source;
	}
	
	public Set<Field> getFields() {
		return Collections.singleton(source);
	}
	
	public Object getValue(ShapeLayer layer, Feature f) {
		Object value = f.getAttribute(source);
		if (value instanceof Comparable) {
			return op.lookup(this, (Comparable)value);
		} else {
			return null;
		}
	}
	
	public static class Factory extends FieldFactory<LookupTable<Object>> {
		public Factory() {
			super("Lookup Table", LookupTable.class, null);
		}
		
	// This is not called anywhere -- just exists to satisfy FieldFactory interface	
		public LookupTable<Object> createField(Set<Field> fields){
			return null;
		}
		
		
		
		/**
		 * Used to tell whether the initial dialog is closed when a lookup column
		 * is being created. If it's closed, then null is passed on so that 
		 * a column doesn't get created.
		 */
		protected Boolean isNull = false;
		
		/**
		 * @return prompts the user for the field to use as the source and the
		 *         target type, and returns a table to map from the source field
		 *         to the target type.
		 */
		public LookupTable<Object> createField(Set<Field> fields, Component c) {
			isNull = false;
			final JDialog dlg = new JDialog(LManager.getDisplayFrame(), "Configure lookup table", true);
			//override close action to pass on null so that a new column isn't created
			dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			dlg.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent windowEvent){
					dlg.dispose();
					isNull = true;
				}
			});
			
			
			JButton okay = new JButton("Create".toUpperCase());
			okay.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dlg.dispose();
				}
			});
			
			Set<Field> temp = new LinkedHashSet<Field>();
			for (Field f: fields) {
				if (Comparable.class.isAssignableFrom(f.type)) {
					temp.add(f);
				}
			}
			if (temp.isEmpty()) {
				return null;
			}
			
			fields = temp;
			
			JComboBox sourceFields = new JComboBox(new ListComboBoxModel<Field>(new ArrayList<Field>(fields)));
			JComboBox targetTypes = new JComboBox(new MapComboBoxModel<String,Class<?>>(ColumnEditor.basicTypes));
			
			JPanel panel = new JPanel(new GridBagLayout());
			panel.setBorder(new EmptyBorder(5, 5, 5, 5));
			int pad = 4;
			Insets in = new Insets(pad,pad,pad,pad);
			int row=0;
			panel.add(new JLabel("Source Field"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
			panel.add(sourceFields, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
			panel.add(new JLabel("Target Type"), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
			panel.add(targetTypes, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
			panel.add(okay, new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
			
			dlg.add(panel);
			dlg.pack();
			dlg.setLocationRelativeTo(Util.getDisplayFrame(c));
			dlg.toFront();
			dlg.setVisible(true);
			
			if(isNull){
				return null;
			}else{
				return new LookupTable<Object>((Field)sourceFields.getSelectedItem(),
						(Class<?>)ColumnEditor.basicTypes.get(targetTypes.getSelectedItem()));
			}
		}
		
		public JPanel createEditor(ColumnEditor editor, final Field source) {
			if (!(source instanceof LookupTable)) {
				return null;
			}
			
			final LookupTable<Object> field = (LookupTable<Object>)source;
			final List<Operation<?>> ops = getTypes(field.source.type, field.type);
			final JComboBox operationTypes = new JComboBox(ops.toArray(new Operation[ops.size()]));
			operationTypes.setSelectedItem(field.op);
			operationTypes.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Operation op = ops.get(operationTypes.getSelectedIndex());
					field.op = op;
				}
			});
			
			// create STable with overridden cell editor creator that
			// ignores the cell editor implementations of isCellEditable()
			final STable lookupTable = new STable(false) {
				private static final long serialVersionUID = 1L;
				public TableCellEditor getCellEditor(int row, int column) {
					final TableCellEditor e = super.getCellEditor(row, column);
					return new TableCellEditor() {
						public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
							return e.getTableCellEditorComponent(table, value, isSelected, row, column);
						}
						public void addCellEditorListener(CellEditorListener l) {
							e.addCellEditorListener(l);
						}
						public void cancelCellEditing() {
							e.cancelCellEditing();
						}
						public Object getCellEditorValue() {
							return e.getCellEditorValue();
						}

						/**
						 * Replaces the isCellEditable() implementation on
						 * <code>e</code> with one that allows editing on a
						 * single click, or any key press.
						 */
						public boolean isCellEditable(EventObject anEvent) {
							if (anEvent instanceof MouseEvent) {
								return ((MouseEvent)anEvent).getClickCount()==1;
							} else if (anEvent instanceof KeyEvent) {
								return true;
							} else {
								return false;
							}
						}
						public void removeCellEditorListener(CellEditorListener l) {
							e.removeCellEditorListener(l);
						}
						public boolean shouldSelectCell(EventObject anEvent) {
							return e.shouldSelectCell(anEvent);
						}
						public boolean stopCellEditing() {
							return e.stopCellEditing();
						}
					};
				}
				
			};
			lookupTable.setUnsortedTableModel(new AbstractTableModel() {
				private static final long serialVersionUID = 1L;
				public Class<?> getColumnClass(int columnIndex) {
					switch (columnIndex) {
					case 0: return field.source.type;
					case 1: return field.type;
					case 2: return Boolean.class;
					default: return Class.class;
					}
				}
				public int getColumnCount() {
					return 3;
				}
				public String getColumnName(int columnIndex) {
					switch (columnIndex) {
					case 0: return field.source.name + " (" + getTypeName(field.source.type) + ")";
					case 1: return "Result (" + getTypeName(field.type) + ")";
					case 2: return "Delete";
					default: return "";
					}
				}
				private String getTypeName(Class<?> type) {
					int idx = type.getName().lastIndexOf('.');
					return type.getName().substring(idx+1);
				}
				public int getRowCount() {
					return 1 + field.values.size();
				}
				public Object getValueAt(int rowIndex, int columnIndex) {
					if (rowIndex == field.breakPoints.size()) {
						return null;
					} else switch (columnIndex) {
					case 0: return field.breakPoints.get(rowIndex);
					case 1: return field.values.get(rowIndex);
					case 2: return rowIndex < field.breakPoints.size() ? false : null;
					default: return null;
					}
				}
				public void setValueAt(Object value, int row, int col) {
					if (value == null) {
						return;
					}
					if (col == 0) {
						Comparable<Object> cvalue = (Comparable<Object>)value;
						if (field.breakPoints.indexOf(cvalue) == -1) {
							Object value2;
							if (row < field.breakPoints.size()) {
								field.breakPoints.remove(row);
								value2 = field.values.remove(row);
							} else {
								value2 = null;
							}
							int index = getPos(field, cvalue);
							field.breakPoints.add(index, cvalue);
							field.values.add(index, value2);
						}
					} else if (col == 1 && row < field.values.size()) {
						field.values.set(row, value);
					} else if (col == 2) {
						if (value instanceof Boolean && ((Boolean)value).booleanValue()) {
							field.breakPoints.remove(row);
							field.values.remove(row);
						}
					}
					fireTableDataChanged();
					boolean found = false;
					for (int i = 0; i < field.values.size(); i++) {
						if (field.values.get(i) == null) {
							found = true;
							lookupTable.getSelectionModel().setSelectionInterval(i, i);
							lookupTable.getColumnModel().getSelectionModel().setSelectionInterval(1, 1);
						}
					}
					if (!found) {
						lookupTable.getSelectionModel().setSelectionInterval(field.breakPoints.size(), field.breakPoints.size());
						lookupTable.getColumnModel().getSelectionModel().setSelectionInterval(0, 0);
					}
				}
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return rowIndex < field.breakPoints.size() || columnIndex == 0;
				}
			});
			
			lookupTable.setRowHeight(lookupTable.getRowHeight()+4);
			lookupTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "startEditing");
			lookupTable.setColumnSelectionAllowed(true);
			//lookupTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			boolean isColorCellEditable = true;
			lookupTable.setTypeSupport(Color.class, new ColorCellRenderer(isColorCellEditable), new ColorCellEditor());
			lookupTable.setTypeSupport(LineType.class, new LineTypeTableCellRenderer(), new LineTypeCellEditor());
			
			lookupTable.setTypeSupport(FillStyle.class, new FillStyleTableCellRenderer(), new FillStyleCellEditor());
			
			lookupTable.setTypeSupport(ListType.class, new DefaultTableCellRenderer(), new ListTypeCellEditor(field.source));
			
			JPanel panel = new JPanel(new GridBagLayout());
			int pad = 4;
			Insets in = new Insets(pad,pad,pad,pad);
			panel.add(new JLabel("Operation"), new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, in, pad, pad));
			panel.add(operationTypes, new GridBagConstraints(0,1,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, in, pad, pad));
			panel.add(new JLabel("Table"), new GridBagConstraints(0,2,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, in, pad, pad));
			panel.add(new JScrollPane(lookupTable), new GridBagConstraints(0,3,1,1,1,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
			return panel;
		}
		
		private static List<Operation<?>> getTypes(Class<?> sourceType, Class<?> targetType) {
			List<Operation<?>> types = new ArrayList<Operation<?>>();
			if (Comparable.class.isAssignableFrom(sourceType)) {
				types.add(new Lookup<Object>());
				
				// Lists are string based, so only offer Lookup/exact matches, not Breakpoints
				if (!sourceType.isAssignableFrom(ListType.class)) {
					types.add(new Breakpoints<Object>());
				}
				
				if (Number.class.isAssignableFrom(sourceType)) {
					if (targetType.isAssignableFrom(Double.class)) {
						types.add(new Interpolate<Double>(new DoubleInterpolator()));
					} else if (targetType.isAssignableFrom(Float.class)) {
						types.add(new Interpolate<Float>(new FloatInterpolator()));
					} else if (targetType.isAssignableFrom(Integer.class)) {
						types.add(new Interpolate<Integer>(new IntInterpolator()));
					} else if (targetType.isAssignableFrom(Short.class)) {
						types.add(new Interpolate<Short>(new ShortInterpolator()));
					} else if (targetType.isAssignableFrom(Byte.class)) {
						types.add(new Interpolate<Byte>(new ByteInterpolator()));
					} else if (targetType.isAssignableFrom(Color.class)) {
						for (ColorInterp interp: ColorInterp.ALL) {
							types.add(new Interpolate<Color>(new ColorInterpolator(interp)));
						}
					}
				}
			}
			return types;
		}
	}
}
