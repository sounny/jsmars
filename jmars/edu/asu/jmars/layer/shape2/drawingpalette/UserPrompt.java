package edu.asu.jmars.layer.shape2.drawingpalette;


/**
 * 
 * callouts that we show to the user for drawing shapes.
 */
public enum UserPrompt {
	
	ACTIVATE_PALETTE("double click here to show drawing palette."  
			 + "<br>" 
			 + "You can also mouse-right-click in main view to bring up drawing menu.") {},
	
	ACTIVATE_PALETTE_2("double click here to show drawing palette."  
			 + "<br>" 
			 + "You can also mouse-right-click in main view to bring up drawing menu.") {},	
		
    ACTIVE_NOT_VISIBLE("You need to turn 'M' ON to draw shapes") {},
    
    ACTIVATED_VISIBLE("'Custom Shape' layer was activated so that you can draw shapes") {},
    
    ACTIVATED_NOT_VISIBLE("'Custom Shape' layer was activated so that you can draw shapes." 
							+ "<br>" 
							+ "You need to turn 'M' ON to draw shapes") {},
	
	ADD_CUSTOMSHAPE_LAYER("'Draw shapes' mode is available when a 'Custom Shape' layer is loaded and is active in 'Layers'.\n" 
							+ "You currently don't have a 'Custom Shape' layer loaded.\n"
							+ "Would you like to add a 'Custom Shape' layer?") {},
	
	SELECT_CUSTOMSHAPE_TO_DRAW("Select 'Custom Shape' layer to draw shapes.") {};
						

	String prompt;
	public static final int SHORT_TIME = 5000;
	public static final int LONG_TIME = 10000; 

	private UserPrompt(String msg) {
		this.prompt = msg;
	}


	public String asString() {
		return this.prompt;
	}

}
