/**
 *  A class for displaying rulers.
 *  The only things actually drawn by this class are objects common to all rulers.  Specific 
 *  rulers are drawn in subclasses..
 * 
 *  @author: James Winburn MSFF-ASU 
 */
package edu.asu.jmars.ruler;

// generic java imports 
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.Hashtable;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.LViewManager;
import edu.asu.jmars.layer.Layer;

public class BaseRuler extends JPanel implements DataReceiver, MouseListener, MouseMotionListener {
	public BaseRuler() {
		ToolTipManager.sharedInstance().registerComponent( this);
		addMouseListener( this);
		addMouseMotionListener( this);
	}

	// Subclasses may implement to update view specific settings.  If they do, 
	// they should call these superclass versions of the methods to maintain
	// the properties that do not show up on the panel but nevertheless affect
	// every ruler.
	public void  loadSettings(Hashtable<String,Object> rulerSettings){
		if (rulerSettings.containsKey("height") ) {
			int height = ((Integer) rulerSettings.get("height")).intValue();
			if (height > 0) {
				RulerManager.Instance.resizeComponent( this, height);
				setRestoreHeight(height);
			}
		}
		if ( rulerSettings.containsKey("hidden") ) {
			boolean hidden = ((Boolean) rulerSettings.get("hidden")).booleanValue();
			if (hidden == true){
				RulerManager.Instance.hideComponent(this);
			} else {
				RulerManager.Instance.showComponent(this);
			}
		}
	}

    // By overwriting this method, rulers may save properties that are particular to 
    // them. If this is not overwritten, then the system simply saves off the generic
    // ruler properties.
    public Hashtable<String,Object> saveSettings(){
    	Hashtable<String,Object> rulerSettings = new Hashtable<String,Object>();
    	saveSettings(rulerSettings);
    	return rulerSettings;
    }

    // If a ruler DOES overwrite the saveSettings() method to save ruler specific 
    // settings, it should make a call to this method (with a call to
    // super.saveSettings( rulerSettings) somewhere in the body of the method) to 
    // make sure that the generic ruler properties are properly stored.
    public void saveSettings(Hashtable<String,Object> rulerSettings) {
    	rulerSettings.put("hidden", new Boolean(isHidden()));
    	rulerSettings.put("height", isHidden() ? getRestoreHeight() : getHeight());
    }

    // Any properties common to rulers of a given BaseRuler type can be saved
    // in the jmars.config file by overwriting this method in the BaseRuler class.
    public Hashtable<String,Object> saveBaseRulerSettings() {
    	return new Hashtable<String,Object>();
    }

    // Any properties common to rulers of a given BaseRuler type can be loaded
    // from the jmars.config file by overwriting this method in the BaseRuler class.
    public void loadBaseRulerSettings(Hashtable<String,Object> rulerSettings){};

    // methods and fields for maintaining the ruler label, which appears in
    // the upper left-hand corner of the ruler.  Note that a label does not
    // NEED to be specified.  A ruler without a specified label uses the 
    // ruler description, which DOES need to be specified for all rulers.
    // This is separate from the description to allow for ruler labels with
    // colors different from the default black/light gray.
    protected AttributedString label;
    public AttributedString getLabel(){
    	return label;
    }
    public void setLabel( AttributedString as){
    	label = as;
    }

    // methods and fields for maintaining the ruler description.  This 
    // appears in the right-click menu for hiding/unhiding the ruler.
    // If a label is not specified for the ruler, the description is used
    // as the label.
    protected String description;
    public String getDescription(){
    	return description;
    }
    public void setDescription( String s){
    	description = s;
    }

    // controls whether a ruler is hidden or not.
    boolean hidden = false;
    public boolean isHidden() {
    	return hidden;
    }
    public void setHidden( boolean h){
    	hidden = h;
    	setVisible(!h);
    }

    // controls for storing and retrieving the height of the ruler 
    // when it is hidden/unhidden.
    protected int restoreHeight;
    public void setRestoreHeight( int i){
    	restoreHeight = i;
    }
    public int getRestoreHeight(){
    	return restoreHeight;
    }

    // all derived rulers are required to specify the initial height of the 
    // ruler, presumbably in the constructor.
    public void setDefaultHeight( int height){
    	setPreferredSize( new Dimension( 0, height));
    }

    // All derived rulers are required to specify the LView they are connected to,
    // presumbably in the constructor.  These objects and methods control access to
    // that LView. 
    protected Layer.LView originatingLView;
    public void setLView(Layer.LView lview) {
    	originatingLView = lview;

    	if ( lview.getLayer() != null && this instanceof DataReceiver )
    		lview.getLayer().registerDataReceiver((DataReceiver)this);
    }
    public Layer.LView getLView() {
    	return originatingLView;
    }

    // returns the LView manager connected to the ruler.
    public LViewManager getLViewManager() {
    	return (LViewManager) originatingLView.getParent();
    }

    // Rulers can implement the DataReceiver interface by overwriting this
    // method.  This is optional.
    public void receiveData(Object data) {}

    // All derived rulers are required to implement this method to display ruler specific data.
    public void paintBaseRuler( Graphics g){};

    // displays objects common to ALL rulers, both MRO and non-MRO.
    public void paintComponent(Graphics g)
    {
    	// draw the background.
    	Rectangle bound = getBounds();
    	Graphics2D g2 = (Graphics2D)g;
    	Font lblFont = g2.getFont();
    	lblFont = lblFont.deriveFont(Math.max(1.0f, lblFont.getSize2D()+RulerManager.relFontSize.getIntValue()));

    	g2.setPaint( RulerManager.Instance.backgroundColor.getColor());
    	g2.fill( new Rectangle2D.Double(0, 0, bound.width, bound.height));

    	// Paint the details defined in the application base ruler.  This will
    	// ultimately call specific ruler painting..um, stuff.
    	paintBaseRuler( g);

    	// regardless of whether the ruler is hidden, draw the title above everything 
    	// in the ruler.  If a label has not been defined by the ruler, use the
    	// ruler description.
    	if (label == null) {
    		AttributedString as = new AttributedString( getDescription() );
    		if (isHidden()){
    			as.addAttribute( TextAttribute.FOREGROUND, Color.gray);
    			as.addAttribute( TextAttribute.BACKGROUND, Color.black);
    		} else {
    			as.addAttribute( TextAttribute.FOREGROUND, Color.white);
    			as.addAttribute( TextAttribute.BACKGROUND, Color.darkGray);
    		}
    		as.addAttribute(TextAttribute.FONT, lblFont);
    		((Graphics2D)g).drawString(as.getIterator(), 0, lblFont.getSize());
    	} else {
    		label.addAttribute(TextAttribute.FONT, lblFont);
    		((Graphics2D)g).drawString(label.getIterator(), 0, lblFont.getSize());
    	}
    }

    // returns right-click menu items that are common to all MRO rulers.  Note that
    // all derived rulers are required to overwrite the getRulerSpecificMenu method 
    // to add their own menu items. 
    public Component [] getRulerSpecificMenu(){
    	return null;
    }

    // This should be overwritten by the individual rulers to put their properties panel
    // into the general properties tabbed panel.
    public JPanel getPropertiesPanel(){
    	return new JPanel();
    }

    // These mouse handler routines may be overwritten by the application specific
    // BaseRuler class.
    public void mousePressed(MouseEvent me){}
    public void mouseReleased(MouseEvent me){}
    public void mouseDragged(MouseEvent me) {}
    public void mouseMoved(MouseEvent me){}
    public void mouseExited(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}

    // Defines the text of the tool tip, although nothing is specified here.
    // This method may be overwritten by derived rules for ruler specific tooltip behavior.
    public String getToolTipText( MouseEvent me){
    	return null;
    }

    // Get a panel containing properties applicable to all rulers.  This should be
    // overwritten by derived ruler types.
    public JPanel getBaseRulerPropertiesPanel() {
    	return new JPanel();
    }
} // End: class BaseRuler

