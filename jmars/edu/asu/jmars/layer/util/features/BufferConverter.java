package edu.asu.jmars.layer.util.features;

import java.nio.ByteBuffer;

/** Defines a regularly sized record and methods for saving and loading from a ByteBuffer of that size */
public interface BufferConverter<E> {
	int getItemSize();
	void save(ByteBuffer buffer, E item);
	E load(ByteBuffer buffer);

	public static final class IntConverter implements BufferConverter<Integer> {
		private final int nullValue;
		public IntConverter(int nullValue) {
			this.nullValue = nullValue;
		}
		public int getItemSize() {
			return 4;
		}
		public Integer load(ByteBuffer buffer) {
			int v = buffer.getInt();
			if (v == nullValue) {
				return null;
			} else {
				return v;
			}
		}
		public void save(ByteBuffer buffer, Integer item) {
			if (item == null) {
				buffer.putInt(nullValue);
			} else {
				buffer.putInt(item);
			}
		}
	}

	public static final class DoubleConverter implements BufferConverter<Double> {
		private final double nullValue;
		public DoubleConverter(double nullValue) {
			this.nullValue = nullValue;
		}
		public int getItemSize() {
			return 8;
		}
		public Double load(ByteBuffer buffer) {
			double d = buffer.getDouble();
			if (Double.compare(d, nullValue) == 0) {
				return null;
			} else {
				return d;
			}
		}
		public void save(ByteBuffer buffer, Double item) {
			if (item == null) {
				buffer.putDouble(nullValue);
			} else {
				buffer.putDouble(item);
			}
		}
	}

	public static final class StringConverter implements BufferConverter<String> {
		private final char[] chars;
		private final char nullValue;
		/**
		 * @param maxLength The max length in bytes (<b>not</b> characters) of all strings
		 * @param charset The character set to use for encoding and decoding the strings
		 */
		public StringConverter(int maxLength, char nullValue) {
			chars = new char[maxLength/2];
			this.nullValue = nullValue;
		}
		public int getItemSize() {
			return chars.length*2;
		}
		public String load(ByteBuffer buffer) {
			int i = 0;
			while (i < chars.length) {
				chars[i] = buffer.getChar();
				if (chars[i] == '\0' || (i == 0 && chars[i] == nullValue)) {
					break;
				}
				i ++;
			}
			return new String(chars, 0, i);
		}
		public void save(ByteBuffer buffer, String item) {
			if (item == null) {
				for (int i = 0; i < chars.length; i++) {
					buffer.putChar(nullValue);
				}
			} else {
				int end = Math.min(item.length(), chars.length - 1);
				item.getChars(0, end, chars, 0);
				chars[end] = '\0';
				for (int i = 0; i < chars.length; i++) {
					buffer.putChar(chars[i]);
				}
			}
		}
	}
}
