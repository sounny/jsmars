package edu.asu.jmars.util;

/**
 * Used sparingly to defer control of internal values when package scoping will
 * not suffice. Roughly analogous usage to the C++ 'friend' operator.
 * @param name The name of the argument to override.
 * @param val The new value to set the argument to. It must be of the expected
 * type.
 * @throws IllegalArgumentException If the name is unknown or the value is of
 * the wrong type.
 */
public interface SetProxy { void set(String name, Object val); }
