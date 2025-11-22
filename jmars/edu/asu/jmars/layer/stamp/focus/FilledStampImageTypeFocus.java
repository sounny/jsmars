package edu.asu.jmars.layer.stamp.focus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import edu.asu.jmars.layer.MultiProjection;
import edu.asu.jmars.layer.stamp.FilledStamp;
import edu.asu.jmars.layer.stamp.FilledStampImageType;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.swing.FancyColorMapper;
import edu.asu.jmars.swing.IconButtonUI;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeText;

public class FilledStampImageTypeFocus extends FilledStampFocus{
	private JButton btnPanNW; 
	private JButton btnPanN;
	private JButton btnPanNE;
	private JButton btnPanW;
	private JButton btnPanE;
	private JButton btnPanSW;
	private JButton btnPanS; 
	private JButton btnPanSE;
	private JButton btnPanSize;
	private JButton resetPan;
	private JLabel xoffset;
	private JLabel yoffset;
	private FancyColorMapper mapper;
  
	private int panIdx = 0;
	private int panSize = panSizeList[panIdx];
  
	private static final int[] panSizeList = { 1, 2, 5, 10 };
	private static final ImageIcon[] panSizeIcons;
	private static final ImageIcon[] panSizeIconsD; // disabled icons
	static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	static Color disabledtext = ((ThemeText) GUITheme.get("text")).getTextDisabled();
	static Icon pan, panD;
	static String imgname;
	
	static {
		panSizeIcons  = new ImageIcon[panSizeList.length];
		panSizeIconsD = new ImageIcon[panSizeList.length];
		for (int i=0; i<panSizeList.length; i++)
			try {
				imgname = ("PAN_" + panSizeList[i]).toUpperCase();
				pan = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.valueOf(imgname)
				         .withDisplayColor(imgColor)));
				panD = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.valueOf(imgname)
				         .withDisplayColor(disabledtext)));			
				panSizeIcons[i]  = (ImageIcon) pan;
				panSizeIconsD[i] = (ImageIcon) panD;
			}
		catch(Throwable e) {
			log.println("Failed to load icon for pansize " + panSizeList[i]);
		}
	}
  
  
	public FilledStampImageTypeFocus(StampLView lview) {
		super(lview);
		
		btnPanNW = new PanButton(-1, +1, false);
		btnPanN =  new PanButton( 0, +1, false);
		btnPanNE = new PanButton(+1, +1, false);
		btnPanE =  new PanButton(+1,  0, false);
		btnPanSE = new PanButton(+1, -1, false);
		btnPanS =  new PanButton( 0, -1, false);
		btnPanSW = new PanButton(-1, -1, false);
		btnPanW =  new PanButton(-1,  0, false);
		btnPanSize = new PanButton();
		  
		JPanel pnlPanning = new JPanel(new GridLayout(3, 3, 5, 5));
		pnlPanning.add(btnPanNW);
		pnlPanning.add(btnPanN);
		pnlPanning.add(btnPanNE);
		pnlPanning.add(btnPanW);
		pnlPanning.add(btnPanSize);
		pnlPanning.add(btnPanE);
		pnlPanning.add(btnPanSW);
		pnlPanning.add(btnPanS);
		pnlPanning.add(btnPanSE);
      
		mapper = new FancyColorMapper();
		mapper.addChangeListener( new ChangeListener() {
	         public void stateChanged(ChangeEvent e) {
	             if (mapper.isAdjusting())
	                 return;
	             
	             List<FilledStamp> fs=getFilledSelections();
	             
	             for (FilledStamp fstamp : fs) {
	            	 FilledStampImageType f = (FilledStampImageType) fstamp;
	            	 f.colors = mapper.getState();	 
	             }
	             
	             parent.clearLastFilled();
	             redrawTriggered();
	         }
		});
		
        mapper.btnAuto.setEnabled(true);
        mapper.btnAuto.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 FilledStamp fs = getFilledSingle();
                 if (fs == null)
                     return;
                                                 
                 int[] hist=null;;
                 
                 try {
                	 hist = fs.pdsi.getHistogram();
                 } catch (IOException ioe) {
                	 ioe.printStackTrace();
                	 return;
                 }
                 
                 if (hist==null) return;
                 
                 // Find the peak
                 int top = 0;
                 for (int i=0; i<256; i++)
                     if (hist[i] > hist[top])
                         top = i;
                     
                     // Find the hi boundary: the next time we hit 5% peak
                 int hi = top;
                 while(hi < 255  &&  hist[hi]*20 > hist[top])
                     ++hi;
                 
                 // Find the lo boundary: the prior time we hit 5% peak
                 int lo = top;
                 while(lo > 0  &&  hist[lo]*20 > hist[top])
                     --lo;
                 
                 mapper.rescaleTo(lo, hi);
             }
        });
        
        
        resetPan = new JButton("Reset Offset".toUpperCase());
        xoffset = new JLabel("X offset: ");
        yoffset = new JLabel("Y offset: ");
      
        resetPan.addActionListener(new ActionListener(){		
        	public void actionPerformed(ActionEvent e) {
        		List<FilledStamp> filledStamps = getFilledSelections();
        		for (FilledStamp fstamp : filledStamps)	{
					FilledStampImageType fs = (FilledStampImageType) fstamp;
					fs.setOffset(new Point2D.Double(0,0));        					
					fs.saveOffset();
					refreshPanInfo(fs);
        		}
        		clearClipAreas();
        		redrawTriggered();
        	}
        });
      
        resetPan.setToolTipText("Reset this stamp to the original, unnudged position");
        
        
        JPanel col1 = new JPanel(new GridBagLayout());
        col1.add(pnlPanning, new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.NONE,in,pad,pad));
      
        JPanel col2 = new JPanel(new GridBagLayout());
        col2.add(xoffset, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.NONE,in,pad,pad));
        col2.add(yoffset, new GridBagConstraints(0,1,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.NONE,in,pad,pad));
        col2.add(resetPan, new GridBagConstraints(0,2,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.NONE,in,pad,pad));
              
        Box h = Box.createHorizontalBox();
        h.add(col1);
        h.add(Box.createHorizontalStrut(pad));
        h.add(col2);
        h.add(Box.createHorizontalGlue());
      
        JPanel selPanel = new JPanel(new GridBagLayout());
        selPanel.setBorder(new TitledBorder("Selected Stamps"));
        selPanel.add(h, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
        selPanel.add(mapper, new GridBagConstraints(0,1,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,pad,pad));

        add(selPanel, new GridBagConstraints(0,1,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.BOTH,in,pad,pad));
	}

  
	protected void enableEverything(){
		//only proceed if this constructor has completed, not just the super
		if(btnPanN != null){
			//call parent to enable those GUI components
			super.enableEverything();
			
			boolean anySelected = !listStamps.isSelectionEmpty();
			boolean singleSelected = listStamps.getSelectedIndices().length == 1;
			
			btnPanNW.setEnabled(anySelected);
			btnPanN .setEnabled(anySelected);		
			btnPanNE.setEnabled(anySelected);
			btnPanW .setEnabled(anySelected);
			btnPanE .setEnabled(anySelected);
			btnPanSW.setEnabled(anySelected);
			btnPanS .setEnabled(anySelected);
			btnPanSE.setEnabled(anySelected);
			btnPanSize.setEnabled(anySelected);
			
			FilledStampImageType fs = (FilledStampImageType) getFilledSingle();
	      
			mapper.setEnabled(singleSelected && fs != null);
	
			if (anySelected) {
				mapper.setPasteEnabled(true);
			}
	      
			resetPan.setEnabled(anySelected);
	      
			if (fs != null) {
				log.println("Implementing newly-selected " + fs.stamp);
				refreshPanInfo(fs);
				mapper.setState(fs.colors);
			} else {
				xoffset.setText("X offset: 0");
				yoffset.setText("Y offset: 0");
			}
		}
	}	

	private class PanButton extends JButton {
		// Button for toggling pan step-size
		private PanButton() {
			setAction(new AbstractAction(null, panSizeIcons[0]) { 
				public void actionPerformed(ActionEvent e) {
					panIdx = (panIdx + 1) % panSizeList.length;
                    panSize = panSizeList[panIdx];
                    setIcon(panSizeIcons [panIdx]);
                    setDisabledIcon(panSizeIconsD[panIdx]);
				}
			});
			setToolTipText("Toggle the number of pixels that the arrow buttons shift by.");
			squish();
			setUI(new IconButtonUI());
		}	
		
		// Movement button
		private PanButton(final int x, final int y, boolean squishLayout) {
			// Determine an icon for the given x/y direction
			String dir = "";
			switch(y){
				case -1:  dir += "s";  break;
				case +1:  dir += "n";  break;
			}
			switch(x){
				case -1:  dir += "w";  break;
				case +1:  dir += "e";  break;
			}
			Icon dirIcon = null;
			try{
				imgname = ("PAN_" + dir).toUpperCase();
				pan = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.valueOf(imgname)
				         .withDisplayColor(imgColor)));
				panD = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.valueOf(imgname)
				         .withDisplayColor(disabledtext)));		
				dirIcon = pan;
			}
			catch(Throwable e){
				log.aprintln("Unable to load dir " + dir);
			}
			
			setBorder(new EmptyBorder(0, 2, 0, 2));
            setDisabledIcon(panD);
			setAction(new AbstractAction(null, dirIcon) {
				public void actionPerformed(ActionEvent e){
					Point2D worldPan = getWorldPan(x * panSize, y * panSize);
          			List<FilledStamp> filledStamps = getFilledSelections();
          			for (FilledStamp fstamp : filledStamps){
          				FilledStampImageType fs = (FilledStampImageType)fstamp;
          					
          				Point2D oldOffset = fs.getOffset();
          				fs.setOffset(new Point2D.Double(oldOffset.getX() + worldPan.getX(),
          												oldOffset.getY() + worldPan.getY()));
          					
          				fs.saveOffset();
          				refreshPanInfo(fs);
          			}
          			
          			clearClipAreas();
   					redrawTriggered();
       			}
       		});
			setToolTipText("Shift the filled stamp(s) on-screen.");
			if(squishLayout){
				squish();
			}
			
			setUI(new IconButtonUI());
		}
      
		private void squish(){
			setFocusPainted(false);          
			Dimension d = this.getMinimumSize();
			d.width = d.height;
			setMaximumSize(d);
			setPreferredSize(d);
		}
	}
	
	private void refreshPanInfo(FilledStampImageType fs) {
		String fmt = "{0} offset: {1,number,#.####}";
		xoffset.setText(MessageFormat.format(fmt, "X", fs.getOffset().getX()));
		yoffset.setText(MessageFormat.format(fmt, "Y", fs.getOffset().getY()));    	
	}
  
	/**
	 ** Given a user-requested pan in pixels, should return the actual
	 ** pan in world coordinates.
	 **/
	protected Point2D getWorldPan(int px, int py) {
		MultiProjection proj = parent.viewman.getProj();
		if (proj == null) {
			log.aprintln("null projection");
			return null;
		}
		
		Dimension2D pixelSize = proj.getPixelSize();
		if (pixelSize == null) {
			log.aprintln("no pixel size");
			return null;
		}
		
		return  new Point2D.Double(px * pixelSize.getWidth(),
		                           py * pixelSize.getHeight());
	}
}
