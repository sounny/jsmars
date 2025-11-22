package edu.asu.jmars.viz3d.core.geometry;

/**
 * Enumeration of clipping types in JOGL
 * 
 * "Portions Copyright (C) Thatcher Ulrich, 2000"
 */

public enum Visibility {
    NoClip(0), 
    SomeClip(1), 
    NotVisible(2);
    
   private int value;

   private Visibility (int value) {
           this.value = value;
   }
}
