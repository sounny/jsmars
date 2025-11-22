package edu.asu.jmars.layer.mcdslider;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.InvestigateData;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.MultiProjection;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.map2.AlphaCombinerOp;
import edu.asu.jmars.util.FloatingPointImageOp;
import edu.asu.jmars.util.Util;

/**
 * 
 * @author srcheese
 *
 */
public class MCDSliderLView extends LView {

	private MCDSliderFocusPanel myFocusPanel;
	private MCDSliderLayer myLayer;
		
	public MCDSliderLView(Layer layerParent, boolean isChild) {
		super(layerParent);
		
		myLayer = (MCDSliderLayer) layerParent;
				
		if (!isChild) {
			getFocusPanel();
		}
	}
	
	public String getName(){
		return myLayer.settings.name;
	}

	@Override
	protected Object createRequest(Rectangle2D where) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void receiveData(Object layerData) {
		// TODO Auto-generated method stub

	}

	@Override
	protected LView _new() {
		return new MCDSliderLView(getLayer(), true);
	}
	
	public MCDSliderFocusPanel getFocusPanel(){
		//Do not create fp for the panner
		if(getChild() == null){
			return null;
		}
		if(focusPanel == null || myFocusPanel ==  null){
			focusPanel = myFocusPanel = new MCDSliderFocusPanel(MCDSliderLView.this);
		}
		return myFocusPanel;
	}
	
	public synchronized void paintComponent(Graphics g){
		
		Graphics2D g2 = getOffScreenG2();
		if(g2 == null){
			return;
		}
		
		//if projection has changed, re-generate image first
		if(Main.PO != myLayer.getCurrentProjection()){
			myLayer.loadImage();
		}

		if (getChild()!=null) {
			myFocusPanel.setMinValue(myLayer.minVal);
			myFocusPanel.setMaxValue(myLayer.maxVal);
		}
		
		BufferedImage img = doStuff(myLayer.getMapImage(), null, null);
		
		if (myLayer.settings.blendMola) {
			// Test to see if our image is grayscale AFTER the colormapper has been applied
			boolean grayscale = isGrayScale(img);
			
			BufferedImage hsvImg = myLayer.getHSVImage();
			
			BufferedImage imgOut = Util.createCompatibleImage(img, img.getWidth(), img.getHeight());
		
			// The below logic was taken from the Map layer's HSV Composite Stage
			Extractor extractor[] = new Extractor[3];
			extractor[0] = Extractor.create(img, grayscale);  // h
			extractor[1] = Extractor.create(img, grayscale);  // s
			extractor[2] = Extractor.create(hsvImg, true);  // v
			
			float[] alphas = new float[3];
	
			final int x1 = 0;
			final int y1 = 0;
			final int x2 = img.getWidth();
			final int y2 = img.getHeight();
			for (int i = x1; i < x2; i++) {
				for (int j = y1; j < y2; j++) {
					// convert HSB values to RGB for this output location
					// and always enable alpha on pixels we set here
					for(int k=0; k<3; k++)
						alphas[k] = extractor[k].getAlpha(i, j);
	
					int outAlpha = (int)AlphaCombinerOp.alphaCombine(alphas) * 255;
					imgOut.setRGB(i, j, (outAlpha << 24) | 
							(Color.HSBtoRGB(
									extractor[0].getHue(i, j),
									extractor[1].getSat(i, j),
									extractor[2].getVal(i, j)) & 0x00FFFFFF)
						);
				}
			}
			
			img = imgOut;
		}
		
		//transform the image into it's known world bounds (only works in default projection)
		Rectangle2D worldClip = new Rectangle2D.Double();
		worldClip.setFrame(-180,-90, 360,180);
		
		if (img!=null) {
			g2.drawImage(img, Util.image2world(img.getWidth(), img.getHeight(), worldClip), null);
	
			//draw the image one more time shifted by 360 because the graphics wrapped doesn't
			// seem to draw enough times (sometimes it will end at the prime meridian)
			// So, one more instance of the drawing ensures it always covers the screen
			worldClip.setFrame(180, -90, 360, 180);
			g2.drawImage(img, Util.image2world(img.getWidth(), img.getHeight(), worldClip), null);
		}
		
		// super.paintComponent draws the back buffers onto the layer panel
		super.paintComponent(g);	
	}

	private BufferedImage doStuff(BufferedImage originalImage, FloatingPointImageOp fop, BufferedImageOp colorMapOp) {
		BufferedImage convertedImg = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

		if (myFocusPanel==null) {
			myFocusPanel = (MCDSliderFocusPanel)getParentLView().focusPanel;
			return convertedImg;
		}
			
		//get values to stretch the image (start with the
		// min and max values for that map)
		double minVal = myLayer.minVal;
		double maxVal = myLayer.maxVal;
			
		//if the user has entered either min or max value overrides,
		// use those instead
		String userMinVal = myFocusPanel.userMinVal.getText();
		String userMaxVal = myFocusPanel.userMaxVal.getText();
		if(userMinVal!=null && userMinVal.length()>0){
			minVal = Double.parseDouble(userMinVal);
		}
		if(userMaxVal!=null && userMaxVal.length()>0){
			maxVal = Double.parseDouble(userMaxVal);
		}
		
		//create the floating point image op from the min/max values
		FloatingPointImageOp flop = null;
		if (!Double.isNaN(minVal) && !Double.isNaN(maxVal)) {
			flop = new FloatingPointImageOp(minVal, maxVal);
		} else {
			flop = new FloatingPointImageOp();
		}

		convertedImg.createGraphics().drawImage(originalImage, flop, 0, 0);
						
		BufferedImageOp op;
		
		if (getChild()!=null) {
			op = getFocusPanel().colorMapper.getColorMapOp().forAlpha(1);
		} else {
			op = ((MCDSliderFocusPanel)getParentLView().getFocusPanel()).colorMapper.getColorMapOp().forAlpha(1);
		}
		
		//perform the filter transform
		op.filter(convertedImg, convertedImg);
		
		return convertedImg;
	}

    /**
     * Overridden by individual views, returns information for formatting for 
     * Investigate mode while hovering with the cursor.
     */
    public InvestigateData getInvestigateData(MouseEvent event) {
		// Don't do this for the panner
 		if (getChild()==null) return null;

		MultiProjection proj = getProj();	
		if (proj == null) return null;

		Point2D screenPt = event.getPoint();
		Point2D worldPoint = proj.screen.toWorld(screenPt);
		
		int worldX = (int)Math.floor(worldPoint.getX());
		int worldY = (int)Math.floor(worldPoint.getY());

		worldX += 180;
		
		while (worldX<0) {
			worldX+=360;
		}
		worldX = worldX%360;
		
		BufferedImage img = myLayer.getNumericImage();
		
		int ppd = img.getWidth()/360;
		
		int pixelX = (worldX) * ppd;
		int pixelY = (180-(90+worldY)) * ppd;  // 
		
		if (pixelX<0 || pixelY<0 || pixelX >= img.getWidth() || pixelY >= img.getHeight()) return null;
		
		double val = img.getRaster().getSampleDouble(pixelX, pixelY, 0);
		
		InvestigateData id = new InvestigateData(getName());
		
		String category = myFocusPanel.getTypeString();
		
		id.add(category, ""+val);
		return id;
    }
    
  	/**
	 * Override to update view specific settings
	 */
	protected void updateSettings(boolean saving) {
//		if (saving) {
//			viewSettings.put("mcdslider", myLayer.settings);
//		} else {
//			if (viewSettings.containsKey("mcdslider")) {
//				myLayer.settings = (MCDLayerSettings) viewSettings.get("mcdslider");
//				System.out.println("loading: " + myLayer.settings.timeDelay);
//			}
//		}
	}
	
	static boolean isGrayScale(BufferedImage image)	{
		int width = image.getWidth();
		int height = image.getHeight();
		int bands = image.getRaster().getNumBands();
	
		if (bands>3) bands=3;
		
		// Test the type
		if ( image.getType() == BufferedImage.TYPE_BYTE_GRAY ) return true ;
		if ( image.getType() == BufferedImage.TYPE_USHORT_GRAY ) return true ;
		// Test the number of channels / bands
		if ( image.getRaster().getNumBands() == 1 ) return true ; // Single channel => gray scale

		// Multi-channels image; then you have to test the color for each pixel.
		for (int y=0 ; y < height ; y++) {
			for (int x=0 ; x < width ; x++) {
				for (int c=1 ; c < bands ; c++) {
					if ( image.getRaster().getSample(x, y, c-1) != image.getRaster().getSample(x, y, c) ) {
						return false;
					}
				}
			}
		}
		
		return true;
	}		

	// Is used to implement session saving	
	public SerializedParameters getInitialLayerData(){
		MCDLayerSettings settings = myLayer.settings;
		return settings;
	}
	
}

// TODO: Copied from HSVComposite.  Promote to common class?
abstract class Extractor {
	protected final BufferedImage input;
	protected final float[] hsb = new float[3];
	public Extractor(BufferedImage input) {
		this.input = input;
	}
	public static Extractor create(BufferedImage bi, boolean grayscale) {
		int bands = bi.getColorModel().getNumColorComponents();
		int type = bi.getRaster().getTransferType();
		
		if (grayscale || type == DataBuffer.TYPE_BYTE && bands == 1)
			return new GrayByteExtractor(bi);
		return new ColorExtractor(bi);
	}
	public abstract float getHue(int i, int j);
	public abstract float getSat(int i, int j);
	public abstract float getVal(int i, int j);
	public abstract float getAlpha(int i, int j);
}

final class GrayByteExtractor extends Extractor {
	final Raster raster;
	public GrayByteExtractor(BufferedImage input) {
		super(input);
		raster = input.getRaster();
	}
	public float getHue(int i, int j) {
		return (float)raster.getSample(i, j, 0) / 255.0f;
	}
	public float getSat(int i, int j) {
		return (float)raster.getSample(i, j, 0) / 255.0f;
	}
	public float getVal(int i, int j) {
		return (float)raster.getSample(i, j, 0) / 255.0f;
	}
	public float getAlpha(int i, int j) {
		if (raster.getNumBands() == 2)
			return raster.getSampleFloat(i, j, 1) / 255.0f;
		return 1.0f;
	}
}

final class ColorExtractor extends Extractor {
	private final ColorModel cm;
	private final Raster raster;
	private Object pixel;
	public ColorExtractor (BufferedImage input) {
		super(input);
		cm = input.getColorModel();
		raster = input.getRaster();
	}
	public float getHue(int i, int j) {
		pixel = raster.getDataElements(i, j, pixel);
		Color.RGBtoHSB(cm.getRed(pixel), cm.getGreen(pixel), cm.getBlue(pixel), hsb);
		return hsb[0];
	}
	public float getSat(int i, int j) {
		pixel = raster.getDataElements(i, j, pixel);
		Color.RGBtoHSB(cm.getRed(pixel), cm.getGreen(pixel), cm.getBlue(pixel), hsb);
		return hsb[1];
	}
	public float getVal(int i, int j) {
		pixel = raster.getDataElements(i, j, pixel);
		Color.RGBtoHSB(cm.getRed(pixel), cm.getGreen(pixel), cm.getBlue(pixel), hsb);
		return hsb[2];
	}
	public float getAlpha(int i, int j) {
		pixel = raster.getDataElements(i, j, pixel);
		return cm.getAlpha(pixel) / 255.0f;
	}
}


