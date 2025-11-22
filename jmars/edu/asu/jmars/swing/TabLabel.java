package edu.asu.jmars.swing;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.*;
import java.awt.*;


public class TabLabel extends JPanel {

    private JLabel somethingsLabel = new JLabel();
    private Dimension somethingsLabelDimension;
    private JLabel ographics = new JLabel();
    private JLabel space = new JLabel();
    private Dimension ographixsLabelDimension;
    private Dimension dimSpace = new Dimension(0, 32);
    
 
	public TabLabel(String text) {
		super(new BorderLayout());
		int wightSomethings = this.getFontMetrics(this.getFont()).stringWidth("Center: OCENTRIC  -16.781°E, 4.25°N     Radius (km): 0.12500OCENTRIC00369.532° N, 886.935° W");
		somethingsLabelDimension = new Dimension(wightSomethings, dimSpace.height);
		somethingsLabel.setMinimumSize(somethingsLabelDimension);
		somethingsLabel.setPreferredSize(somethingsLabelDimension);
		int wightOgraphicsLabel = this.getFontMetrics(this.getFont()).stringWidth("Center: OCENTRIC  -16.781°E, 4.25°N     Radius (km): 0.1250000000OGRAPHIC699.532° N, 886.935° W");
		ographixsLabelDimension = new Dimension(wightOgraphicsLabel, dimSpace.height);
		ographics.setMinimumSize(ographixsLabelDimension);
		ographics.setPreferredSize(ographixsLabelDimension);
		space.setMinimumSize(dimSpace);
		space.setMaximumSize(dimSpace);
		space.setPreferredSize(dimSpace);
		space.setSize(dimSpace);
		this.parsingLogic(text);
		
		int width = somethingsLabelDimension.width + dimSpace.width + ographixsLabelDimension.width;
		int height = somethingsLabelDimension.height + dimSpace.height +ographixsLabelDimension.height;
		this.setMinimumSize(new Dimension(width, height));
		initLayout();
	}

	public void setText(String newText) {
		this.parsingLogic(newText);
		initLayout();
    }

    public String getText(){
		return somethingsLabel.getText() + "\t" + ographics.getText();
	}

	public void setIconTextGap(int value){
		this.somethingsLabel.setIconTextGap(value);
		this.ographics.setIconTextGap(value);
	}

	public Icon getIcon(){
		return this.somethingsLabel.getIcon();
	}

	public void setIcon(Icon icon){
		this.somethingsLabel.setIcon(icon);
	}
	
	public JLabel getInnerLabel()
	{
		return this.somethingsLabel;
	}		

	private void parsingLogic(String string) {
		this.somethingsLabel.setText(string);
		this.ographics.setText("");
	}

	private void initLayout(){
		this.add(this.somethingsLabel, BorderLayout.WEST);
		this.add(this.space, BorderLayout.CENTER);
		this.add(this.ographics);
	}
}



