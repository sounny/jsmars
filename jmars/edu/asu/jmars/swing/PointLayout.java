/*
 * @(#)PointLayout.java	1.02 2 Jan 2002
 *
 * Copyright John Redmond (John.Redmond@mq.edu.au).
 * 
 * This software is freely available for commercial and non-commercial purposes.
 * Acknowledgement of its source would be appreciated, but is not required.
 * 
 */

package edu.asu.jmars.swing;
// package au.com.pegasustech.demos.layout;

/**
 * Single cell layout: 
 * For information on usage, see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip129.html">
http://www.javaworld.com/javaworld/javatips/jw-javatip129.html</a>
 */
public class PointLayout extends SGLayout {

  public PointLayout() {
    super(1, 1);
  }

  public PointLayout(int hAlignment, int vAlignment) {
    super(1, 1, hAlignment, vAlignment, 0, 0);
  }

  public void setAlignment(int h, int v) {
    super.setAlignment(h, v, 0, 0);
  }
}
