package edu.asu.jmars.layer.util.features;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import org.apache.commons.text.WordUtils;
import edu.asu.jmars.layer.shape2.ColumnEditor;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.layer.shape2.xb.XB;
import edu.asu.jmars.layer.shape2.xb.data.service.Data;
import edu.asu.jmars.layer.shape2.xb.swing.UserPromptFormula;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.ListType;
import edu.asu.jmars.util.Util;
import gnu.jel.CompiledExpression;
import gnu.jel.DVMap;
import gnu.jel.Evaluator;
import gnu.jel.Library;

/**
 * Provides a formula parser and evaluator for Feature instances based on any
 * combination of byte, short, int, long, float, or double field values.
 * 
 * TODO: boo, autosave badly messes things up, not sure why.
 */
public class FieldFormula extends CalculatedField {
	private static final long serialVersionUID = 1L;
	private static DebugLog log = DebugLog.instance();
	
	/** The text expression the user entered. */
	private String textExpression;
	/** The schema used in the last expression compilation, used to restore a serialized FieldFormula. */
	private  Collection<Field> schema;
	/**
	 * The context array, always a single instance of FieldAccessor. This is a
	 * temp variable really, just passed to the evaluate method again and again,
	 * so we don't save it.
	 */
	private transient FieldAccessor[] callContext;
	/**
	 * The compiled expression, or null if none has been successfully compiled
	 * yet. This is not saved, but is instead rebuilt each time; this is to both
	 * ensure that we are always evaluating code that was compiled with the
	 * latest JEL library, but also because JEL expressions do not serialize
	 * well.
	 */
	private transient CompiledExpression compiledExpression;
	
	private Map<String, String> aliases = new HashMap<>();
	
	private FieldFormula(Class<?> type) {
		super("Formula Field", type);
	}
	
	/**
	 * Called by the deserialization code as the last step to determining if
	 * this is the actual object instance we want to inject into the jvm. We use
	 * this point to set up and compile the expression.
	 */

	private Object readResolve() {
		try {
		} catch (Throwable t) {
			log.aprintln(t);
		}
		return this;
	}
	
	private void setExpression(String text, Collection<Field> schema) throws Throwable {
		FieldAccessor map=new FieldAccessor(schema);
		Library lib = new Library(
			new Class[]{Math.class, ColorMethods.class, FieldFormulaMethods.class},
			new Class[]{FieldAccessor.class},
			new Class[]{},
			map,
			null);
		CompiledExpression exp = null;
		//SUBSTITUTE HERE - from delimited columns to what compiles
		//for ex, :My value1: -> pass to JEL My_value1
		String validforJEL = Data.SCHEMA_COLUMN_VALIDATOR.validateIdentifier(text);
		exp = Evaluator.compile("convertReturnType("+validforJEL+")", lib, type);
		this.callContext = new FieldAccessor[]{map};
		this.schema=schema;
		this.textExpression = text;
		this.compiledExpression = exp;
	}
	
	
	@Override
	public Set<Field> getFields() {
		if (callContext == null) {
			return Collections.emptySet();
		} else {
			return new LinkedHashSet<Field>(callContext[0].getUsedFields());
		}
	}
	
	@Override
	public Object getValue(ShapeLayer layer, Feature f) {  //is called from CalcFieldListener "updateValues" on a thread
		if (compiledExpression == null) {
			return null;
		}
		try {
			callContext[0].setFeature(f);
			return compiledExpression.evaluate(callContext);
		} catch (Throwable e) {
			// TODO: disable this, or gather the spew in some way
			log.println(e);
			return null;
		}
	}
	
	public static final class ColorMethods {
		public static float[] hsb(Color c) {
			float[] values = {0,0,0};
			Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), values);
			return values;
		}
		public static float[] rgba(Color c) {
			return c.getRGBComponents(null);
		}
		public static Color fromhsb(float h, float s, float i) {
			return Color.getHSBColor(h, s, i);
		}
		public static Color fromrgba(float r, float g, float b, float a) {
			return new Color(r,g,b,a);
		}
		public static Color color(String name) {
			return Color.getColor(name);
		}
		public static Color color(int r, int g, int b) {
			return color(r,g,b,255);
		}
		public static Color color(int r, int g, int b, int a) {
		 	return new Color(r,g,b,a);
		}
		public static int red(Color c) {
			return c.getRed();
		}
		public static int green(Color c) {
			return c.getGreen();
		}
		public static int blue(Color c) {
			return c.getBlue();
		}
		public static int alpha(Color c) {
			return c.getAlpha();
		}
		private static float[] temp = new float[3];
		private static Color color;
		public static double hue(Color c) {
			set(c);
			return temp[0];
		}
		public static double saturation(Color c) {
			set(c);
			return temp[1];
		}
		public static double intensity(Color c) {
			set(c);
			return temp[2];
		}
		private static void set(Color c) {
			if (c != color) {
				color = c;
				Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), temp);
			}
		}
	}
	public static class Factory extends FieldFactory<FieldFormula> {
		public Factory() {
			super("Formula Field", FieldFormula.class, null);
		}

		AbstractAction helpAction = new AbstractAction("HELP") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.launchBrowser(Config.get("shape.formula.help", "https://jmars.mars.asu.edu/using-shapefile-expressions"));
            }
        };
		@Override
		public JPanel createEditor(final ColumnEditor editor, Field source) {
			final Collection<Field> schema = editor.getModelFields();
			final FieldFormula fieldformula = (FieldFormula)source;
			XB xb = XB.INSTANCE;
			JPanel xbpanel = xb.XBPanel(editor);
			JTextArea formulatext = xb.getXBTextComponent();
			DocumentListener doclistener = new MyExpressionListener(editor, fieldformula, schema);
			AbstractDocument doc = (AbstractDocument)formulatext.getDocument();
			DocumentListener[] listeners = doc.getDocumentListeners();
			for (DocumentListener listener : listeners) {
				if (listener instanceof MyExpressionListener) {
					formulatext.getDocument().removeDocumentListener(listener);
				}
			}
			formulatext.getDocument().addDocumentListener(doclistener);
			SwingUtilities.invokeLater(() -> 
					formulatext.setText(fieldformula.textExpression != null && 
					!fieldformula.textExpression.isEmpty() ? 
					fieldformula.textExpression : UserPromptFormula.ON_FORMULA_START.asString()));			
			formulatext.setCaretPosition(0);
			return xbpanel;
		}

		@Override
		public FieldFormula createField(Set<Field> fields) {
			Map<String,Class<?>> fftypes = new LinkedHashMap<>();
			fftypes.putAll(ColumnEditor.basicTypes);
			fftypes.remove("Line Type");
			fftypes.remove("Fill Style");
			String[] names = fftypes.keySet().toArray(new String[]{});
			Object result = Util.showInputDialog(
				"Choose expression type", "Choose expression type",
				JOptionPane.QUESTION_MESSAGE, null,
				names, names[0]);
			Class<?> type = fftypes.get(result);
			if (type == null) {
				return null;
			} else {
				return new FieldFormula(type);
			}
		}
		
		private class MyExpressionListener implements DocumentListener {
			FieldFormula ff = null;
			ColumnEditor editor = null;
			Collection<Field> schema = null;
			JTextArea exprTextArea = null;
			JPanel resultPreviewPanel = null;
			JPanel errorPreviewPanel = null;
			List<String> errors = new ArrayList<>();
			XB xb = XB.INSTANCE;
			List<String> previewMessage = new ArrayList<>();

			public MyExpressionListener(ColumnEditor ce, FieldFormula fieldformula, Collection<Field> fields) {
				this.ff = fieldformula;
				this.editor = ce;
				this.schema = fields;
				this.exprTextArea = xb.getXBTextComponent();
				this.resultPreviewPanel = xb.getXBResultPreviewComponent();
				this.errorPreviewPanel = xb.getXBErrorPreviewComponent();
			}		

			@Override
			public void insertUpdate(DocumentEvent e) {
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				change();
			}

			private void change() {
				String expr = this.exprTextArea.getText();
				if ((expr != null) && (expr.length() > 0) &&
						!UserPromptFormula.ON_FORMULA_START.asString().equals(expr)) {
					compileExpression(expr);
					if (errors.isEmpty()) {
						evalExpression();
					}
				} else {
					ff.textExpression = "";
					ff.compiledExpression = null;
					clearTabContent(resultPreviewPanel);
					clearTabContent(errorPreviewPanel);
					updateTabContent(resultPreviewPanel, java.util.Arrays.asList("\n"));
					errors.clear();
					editor.enableOK();
				}
			}

			private void compileExpression(String expr) {
				try {
					errors.clear();
					ff.setExpression(expr, schema);
					editor.validate();
					List<String> validationErrors = this.editor.getErrors();
					if (!validationErrors.isEmpty()) {
						errors.addAll(validationErrors);
						updateTabContent(errorPreviewPanel, validationErrors); // we have errors
						clearTabContent(resultPreviewPanel);
						previewMessage.clear();
						previewMessage.add(UserPromptFormula.PREVIEW_PROMPT_WHEN_ERRORS.asString());
						updateTabContent(resultPreviewPanel, previewMessage);
					}
				} catch (Throwable e) {
					List<String> msg2 = formatmsg(e.getMessage(), expr);
					editor.disableOK();
					updateTabContent(errorPreviewPanel, msg2); // we have compile errors
					clearTabContent(resultPreviewPanel);
					previewMessage.clear();
					previewMessage.add(UserPromptFormula.PREVIEW_PROMPT_WHEN_ERRORS.asString());
					updateTabContent(resultPreviewPanel, previewMessage); // in Preview, inform about errors but don't
																			// switch
				}
			}

			private void evalExpression() {
				Object result = "";
				try {
					// add preview for formula fields based on 1st row in FeatureCollection
					FeatureCollection fc = Data.SERVICE.getSelectedFeatureCollection();					
					if (fc != null) {
						List<Feature> features = fc.getFeatures();
						evalBasedOn1stRow(features);
					}	
					result = ff.compiledExpression.evaluate(ff.callContext);
					if (!result.toString().isEmpty()) { // means, we have a valid result
						errors.clear();
						List<String> results = new ArrayList<String>();
						results.add(result.toString());
						if (previewMessage.size() >= 1) {
							results.addAll(previewMessage);
						}
						updateTabContent(resultPreviewPanel, results);
						clearTabContent(errorPreviewPanel);
					}
				} catch (Throwable e) {
					clearTabContent(resultPreviewPanel);
					List<String> msg2 = formatmsg(e.getMessage(), ff.textExpression);
					updateTabContent(errorPreviewPanel, msg2); // we have errors
					clearTabContent(resultPreviewPanel);
					previewMessage.clear();
					previewMessage.add(UserPromptFormula.PREVIEW_PROMPT_WHEN_ERRORS.asString());
					updateTabContent(resultPreviewPanel, previewMessage);
				}
			}
				
			private void evalBasedOn1stRow(List<Feature> features) {
				previewMessage.clear();
				if (features != null && features.size() > 0) {
					Feature feat = features.get(0); // 1st row only, for preview
					ff.callContext[0].setFeature(feat);
					String usedfields = identifyFieldsUsedInFormula(feat);
					if (!usedfields.isEmpty()) {
						previewMessage.add(UserPromptFormula.PREVIEW_PROMPT_WHEN_OK.asString() + usedfields);
					}
				}
			}

			private String identifyFieldsUsedInFormula(Feature feat) {
				List<String> fieldnamesonly = new ArrayList<>();
				StringBuilder usedfields = new StringBuilder();
				Map<Field, Object> attr = feat.attributes;
				Map<String, String> aliases = Data.SERVICE.getAliases();
				Set<String> fieldsinformula = new java.util.HashSet<String>();
				// validForJava example [0] = "23+ cos(12)"; [1] = "My_value1"; [2]="/", etc.
				List<String> validForJava = Data.SCHEMA_COLUMN_VALIDATOR.validateAsArray(ff.textExpression);
				/*
				 * validForJava will have aliases for fields to comply with JEL; so iterate over
				 * aliases map and compare each value with each entry in validForJava to
				 * identify used Field(s)
				 */
				for (String entryinformula : validForJava) {
					for (Map.Entry<String, String> aliasentry : aliases.entrySet()) {
						if (aliasentry.getValue().equals(entryinformula)) {
							fieldsinformula.add(aliasentry.getKey());
						}
					}
				}
				for (Map.Entry<Field, Object> featmapentry : attr.entrySet()) {
					Field field = featmapentry.getKey();
					Object value = featmapentry.getValue();
					if (fieldsinformula.contains(field.name)) {
						usedfields.append(field.name + " = " + value + "  ");
						fieldnamesonly.add(field.name);
					}
				}
				if (!fieldnamesonly.isEmpty() && fieldnamesonly.containsAll(fieldsinformula)) {
					return usedfields.toString();
				} else {
					return "";
				}
			}

			private void updateTabContent(JPanel panel, List<String> text) {
				panel.removeAll();
				JTextArea textArea = new JTextArea();
				textArea.setEditable(false);
				textArea.setCaretPosition(0);
				textArea.setLineWrap(true);
				textArea.setWrapStyleWord(true);			   		        
				JScrollPane sp = new JScrollPane(textArea);

				StringBuilder res = new StringBuilder();
				if (!text.isEmpty()) {
					for (int i = 0; i < text.size(); i++) {
						res.append(text.get(i));
						res.append("\n");
					}
					textArea.setText(res.toString());
					textArea.setCaretPosition( 0 );
				}				
		        
				java.awt.Dimension panelPreferredSize = new java.awt.Dimension(500, textArea.getFontMetrics(textArea.getFont()).getHeight() * 5);
		        sp.setPreferredSize(panelPreferredSize);
				panel.add(sp);
				panel.revalidate();
				panel.repaint();
			}

			private void clearTabContent(JPanel panel) {
				panel.removeAll();
				panel.revalidate();
				panel.repaint();
			}

			private List<String> formatmsg(String errormsg, String expr) {
				String wrap;
				wrap = WordUtils.wrap(errormsg, 80, "\n", false);
				errors.clear();
				errors.add(wrap);
				errors.add(expr);
				return errors;
			}
		}	
   }
	
	public static final class FieldAccessor extends DVMap implements Serializable{
		private static final long serialVersionUID = 1L;
		/** The fields for this schema. */
		private final Field[] fields;
		/** Fields actually looked up by the compiler. */
		private Set<Field> usedFields = new LinkedHashSet<Field>();
		/** The current feature to retrieve values from */
		private transient Feature f;
		/** Creates the name->field mapping for all possible fields. */
		public FieldAccessor(Collection<Field> fields) {
			this.fields = fields.toArray(new Field[fields.size()]);
		}
		
		/**
		 * After this context is used to compile an expression, this method will
		 * return the fields the compiler looked up.
		 */
		public Set<Field> getUsedFields() {
			return usedFields;
		}
		
		/**
		 * Called by the compiler to get the variable type, which the JEL
		 * assembler will use to determine which get<Type>Property() method to
		 * call. We fail any type for which this class does not have such a
		 * get<Type>Property() method.
		 */
		public String getTypeName(String name) {
			Class<?> failedType = null;
			String mytype = null;
			mytype = checkTypeBasedOnName(name, failedType);
			// if return is null- continue checks using aliases
			if (mytype == null) {
				mytype = checkByAlias(name, failedType);
			}
			//check for deprecated alias, i.e. created manually by users
			//if found, then aliasMap in Data.SERVICE is updated, so reiterate steps from #419
			if (mytype == null) {
				if (Data.SERVICE.isDeprecatedAlias(name)) {
					mytype = checkByAlias(name, failedType);	
				}
			}
			if (mytype == null && failedType == null) {
				// no name matched
				return null;
			} else if (mytype == null && failedType != null) {
				// at least one name matched but the type was wrong, so report
				// the unsupported type of the first field where that occurred.
				throw new IllegalArgumentException(
						"Variable " + name + " has unsupported type " + failedType.getSimpleName());
			}
			return mytype;
		}

	private String checkByAlias(String inputname, Class<?> failedType) {
		String rettype = null;
		for (Map.Entry<String, String> entry : Data.SERVICE.getAliases().entrySet()) {
			if (inputname.equals(entry.getValue())) { // Map entry is <field.name : alias>, so 'alias' is value
				String val = entry.getKey();
				rettype = checkTypeBasedOnName(val, failedType);
				break;
			}
		}
		return rettype;
	}

	private String checkTypeBasedOnName(String inputname, Class<?> failedType) {
		for (int i = 0; i < fields.length; i++) {
			if (inputname.equals(fields[i].name)) {
				if (String.class.isAssignableFrom(fields[i].type)) {
					return "String";
				} else if (Boolean.class.isAssignableFrom(fields[i].type)) {
					return "Boolean";
				} else if (Color.class.isAssignableFrom(fields[i].type)) {
					return "Color";
				} else if (Byte.class.isAssignableFrom(fields[i].type)) {
					return "Byte";
				} else if (Short.class.isAssignableFrom(fields[i].type)) {
					return "Short";
				} else if (Integer.class.isAssignableFrom(fields[i].type)) {
					return "Integer";
				} else if (Long.class.isAssignableFrom(fields[i].type)) {
					return "Long";
				} else if (Float.class.isAssignableFrom(fields[i].type)) {
					return "Float";
				} else if (Double.class.isAssignableFrom(fields[i].type)) {
					return "Double";
				} else if (ListType.class.isAssignableFrom(fields[i].type)) {
					return "ListType";
				} else {
					// mark the unsupported type, but keep looking in case
					// we have another field with the right name *and* the
					// right type
					if (failedType == null) {
						// track the first unsupported type
						failedType = fields[i].type;
						return null; // no match but failedType has something
					}

				}
			}
		}
		return null;
	}

	/**
	 * Called by the compiler to convert variable names into field indices, matching
	 * in a case-insensitive way.
	 */
	public Object translate(String name) {
		for (int i = 0; i < fields.length; i++) {
			if (name.equals(fields[i].name)) {
				usedFields.add(fields[i]);
				return i;
			} else {// 1787
				for (Map.Entry<String, String> entry : Data.SERVICE.getAliases().entrySet()) {
					if (name.equals(entry.getValue())) { // Map entry is <column name : alias>, so 'alias' is value
						String val = entry.getKey(); // field.name is key
						for (i = 0; i < fields.length; i++) {
							if (val.equals(fields[i].name)) {
								usedFields.add(fields[i]);
								return i;
							}
						}
					}
				}
			} // 1787 end
		}
		throw new IllegalArgumentException("Name " + name + " not found");
	}
		
		/**
		 * Sets the feature to use as the source of variable values. A series of
		 * calculations that iterate over a FeatureCollection should call this
		 * method once per feature just prior to evaluating the CompiledExpression.
		 * 
		 * Note that this allows us to use one object that provides access to
		 * each Feature in its turn, rather than creating N wrapper objects that
		 * add nothing but the getXXXValue() methods.
		 */
		public void setFeature(Feature f) {
			this.f = f;
		}

		/**
		 * Called by the evaluator to get the value at the given column
		 * position. We don't optimize access to attributes, beyond the
		 * name->field lookup, because a Feature can contain hundreds of
		 * columns, and the time to optimize will greatly exceed the cost of a
		 * single lookup for a single column, which is probably the common case.
		 */
		public Object getProperty(int column) {
			return f.getAttribute(fields[column]);
		}
		public String getStringProperty(int column) {
			return (String)getProperty(column);
		}
		public Boolean getBooleanProperty(int column) {
			return (Boolean)getProperty(column);
		}
		public Color getColorProperty(int column) {
			return (Color)getProperty(column);
		}
		public Byte getByteProperty(int column) {
			return (Byte)getProperty(column);
		}
		public Short getShortProperty(int column) {
			return (Short)getProperty(column);
		}
		public Integer getIntegerProperty(int column) {
			return (Integer)getProperty(column);
		}
		public Long getLongProperty(int column) {
			return (Long)getProperty(column);
		}
		public Float getFloatProperty(int column) {
			return (Float)getProperty(column);
		}
		public Double getDoubleProperty(int column) {
			return (Double)getProperty(column);
		}
		public String getListTypeProperty(int column) {
			if(getProperty(column)==null){
				return "";
			}
			return (String)getProperty(column);
		}
		
		

	public static void main(String[] args) throws Throwable {
		// int, double
		// 123, 1.05
		// 5, 0.03
		FeatureCollection fc = new SingleFeatureCollection();
			
		Feature f = new Feature();
		Field c1 = new Field("c1", Integer.class);
		Field c2 = new Field("c2", Double.class);
		f.setAttribute(c1, 123);
		f.setAttribute(c2, 1.05);
		fc.addFeature(f);
		
		f = new Feature();
		f.setAttribute(c1, 5);
		f.setAttribute(c2, .03);
		fc.addFeature(f);
		
		FieldAccessor map = new FieldAccessor(fc.getSchema());
		Class type = Integer.class;
		Library lib = new Library(
				new Class[]{Math.class, ColorMethods.class, FieldFormulaMethods.class},
				new Class[]{FieldAccessor.class},
				new Class[]{},
				map,
				null);
		CompiledExpression exp = Evaluator.compile("convertReturnType(5)", lib, type);
		for (Feature feat: fc.getFeatures()) {
			map.setFeature(feat);
			System.out.println(exp.evaluate(new Object[]{map}));
		}
	}
  }

}
