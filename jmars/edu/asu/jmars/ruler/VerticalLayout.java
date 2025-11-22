/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *       Sun Microsystems, Inc. for Project JXTA."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact Project JXTA at http://www.jxta.org.
 *
 * 5. Products derived from this software may not be called "JXTA",
 *    nor may "JXTA" appear in their name, without prior written
 *    permission of Sun.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: VerticalLayout.java 2670 2003-10-10 19:53:52Z jwinburn $
 */  
package edu.asu.jmars.ruler;


import java.awt.*; 

public class VerticalLayout 
implements LayoutManager 
{
	int gap; 


	public VerticalLayout() {} 


	public VerticalLayout(int gap) {
		this.gap = gap;
	} 


	public int getGap() {
		return gap;
	} 


	public void setGap(int rows) {
		this.gap = gap;
	}

	public void addLayoutComponent(String name, Component comp) { }
	public void removeLayoutComponent(Component comp) { } 


	public Dimension preferredLayoutSize(Container parent) 
	{
		synchronized (parent.getTreeLock()) {
			Insets insets = parent.getInsets();
			int ncomponents = parent.getComponentCount(); 
			int width = 0;
			int height = insets.top + insets.bottom;

			int numVisible = 0;

			for (int i = 0 ; i < ncomponents ; i++) {
				Component component = parent.getComponent(i);
				if (component.isVisible()) {
					numVisible ++;
					Dimension dimension = component.getPreferredSize();
					height += dimension.height; if (width < dimension.width)
						width = dimension.width;
				}
			}

			if (numVisible > 1)
				height += gap * (numVisible - 2);

			width += insets.left + insets.right;
			return new Dimension(width, height);
		}
	} 


	public Dimension minimumLayoutSize(Container parent) 
	{
		synchronized (parent.getTreeLock()) {
			Insets insets = parent.getInsets();
			int numComponents = parent.getComponentCount(); int width = 0;
			int height = insets.top + insets.bottom;

			int numVisible = 0;

			for (int i = 0 ; i < numComponents ; i++) {
				Component component = parent.getComponent(i);
				if (component.isVisible()) {
					numVisible ++;
					Dimension dimension = component.getMinimumSize();
					height += dimension.height; if (width < dimension.width)
						width = dimension.width;
				}
			} if (numVisible > 1)
				height += gap * (numVisible - 2);

			width += insets.left + insets.right;
			return new Dimension(width, height);
		}
	}


	public void layoutContainer(Container parent) 
	{
		synchronized (parent.getTreeLock()) {

			int numComponents = parent.getComponentCount();
			Insets insets = parent.getInsets();
			Dimension parentSize = parent.getSize(); int width = parentSize.width - (insets.left + insets.right);
			int height = parentSize.height - (insets.top + insets.bottom);

			int x = insets.left;
			int y = insets.top;

			for (int i = 0; i < numComponents; i++) {
				Component component = parent.getComponent(i); if (component.isVisible()) {
					Dimension componentSize = component.getPreferredSize();
					int componentHeight = componentSize.height;

					component.setBounds(x, y, width, componentHeight);

					y += componentHeight;
					height -= (componentHeight + gap);
					height -= gap;
				}
			}
		}
	} 


	public String toString() {
		return getClass().getName() + "[gap=" + gap + "]";
	}

}
