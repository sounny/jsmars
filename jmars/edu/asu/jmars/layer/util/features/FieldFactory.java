package edu.asu.jmars.layer.util.features;

import java.awt.Component;
import java.util.Set;

import javax.swing.JPanel;

import edu.asu.jmars.layer.shape2.ColumnEditor;

public abstract class FieldFactory<E extends Field> {
	private String name;
	private Class<?> fieldType;
	private Class<?> dataType;
	/**
	 * @param name The name of this field.
	 * @param fieldType The type of Field object this factory produces.
	 * @param dataType The type of data the fields of this factory work with, or null if unconstrained.
	 */
	public FieldFactory(String name, Class<?> fieldType, Class<?> dataType) {
		this.name = name;
		this.fieldType = fieldType;
		this.dataType = dataType;
	}
	public String getName() {
		return name;
	}
	public Class<?> getFieldType() {
		return fieldType;
	}
	public Class<?> getDataType() {
		return dataType;
	}
	public String toString() {
		return name;
	}
	/** Creates a new default field, potentially based on these other fields in the feature collection. */
	public abstract E createField(Set<Field> fields);
	public E createField(Set<Field> fields, Component c){
		return createField(fields);
	}
	/**
	 * Creates a new editor for editing the given field, which should be
	 * returned by {@link #createField(FeatureCollection, Field)}.
	 */
	public abstract JPanel createEditor(ColumnEditor editor, Field source);
}