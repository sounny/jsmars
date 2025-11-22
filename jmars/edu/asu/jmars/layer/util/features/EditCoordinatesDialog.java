package edu.asu.jmars.layer.util.features;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.apache.commons.lang3.tuple.Pair;
import edu.asu.jmars.Main;
import edu.asu.jmars.parsers.gis.CoordinatesParser.Ordering;
import edu.asu.jmars.swing.EditCoordsPanel;
import edu.asu.jmars.util.Config;


public class EditCoordinatesDialog extends JDialog implements PropertyChangeListener {

	private JOptionPane optionPane;
	private String button1Str = "APPLY";
	private String button2Str = "APPLY and CLOSE";
	private String button3Str = "CANCEL";
	private List<String> varText = new ArrayList<>();
	private EditCoordsPanel editcoordspanel;
	private Feature currentFeature;
	private Point2D currentVertex;
	private FeatureMouseHandler handlerInstance;
	private String userCoordsInput;

	public EditCoordinatesDialog() {
		setTitle("Edit Point");
		createUI();
	}
	
	private void createUI() {
		Object[] options = { button1Str, button2Str, button3Str };

		editcoordspanel = new EditCoordsPanel(this.varText);

		optionPane = new JOptionPane(editcoordspanel.getCoordinatesInputLayout(), JOptionPane.QUESTION_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION, null, options, options[0]);
		
		setContentPane(optionPane);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		optionPane.addPropertyChangeListener(this);
		
		setLocationRelativeTo(Main.mainFrame);
		
	}	

	@Override
	public void propertyChange(PropertyChangeEvent e) {

		String prop = e.getPropertyName();
		List<String> userInput = new ArrayList<>();
		List<String> updatedValues = new ArrayList<>();

		if (isVisible() && (e.getSource() == optionPane)
				&& (JOptionPane.VALUE_PROPERTY.equals(prop) || JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {

			Object value = optionPane.getValue();

			if (value == JOptionPane.UNINITIALIZED_VALUE) {
				return;
			}

			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

			if (button1Str.equals(value)) { //apply
				userInput = editcoordspanel.getNewCoordinates();
				
				if (!userInput.isEmpty()) {
					userCoordsInput = userInput.get(0);
					String newPointStr = userCoordsInput;
					Pair<Point2D, String> parsingResult = Main.testDriver.locMgr.parse(newPointStr);
					Point2D newpoint2D = parsingResult.getKey();
					if (newpoint2D != null) {
						this.handlerInstance.editVertex(currentFeature, currentVertex, newpoint2D);
						//since we do not close "Edit Position" when user clicks APPLY, redraw it with new values
						String updatedValue = getCoordOrdering().format(newpoint2D);
						updatedValues.clear();
						updatedValues.add(updatedValue);
						withPoints(updatedValues);
						withVertex(newpoint2D);
					} else {
						String errorMsg = parsingResult.getValue();
						if (errorMsg != null && !errorMsg.isEmpty()) {
							JOptionPane.showMessageDialog(this,
							    userInput + " isn't a valid position.\n" +
								errorMsg, 
								"Input Error",
								JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			} else if (button2Str.equals(value)) { //apply and close
				userInput = editcoordspanel.getNewCoordinates();
				if (!userInput.isEmpty()) {
					userCoordsInput = userInput.get(0);
					String newPointStr = userCoordsInput;
					Pair<Point2D, String> parsingResult = Main.testDriver.locMgr.parse(newPointStr);
					Point2D newpoint2D = parsingResult.getKey();
					if (newpoint2D != null) {
						this.handlerInstance.editVertex(currentFeature, currentVertex, newpoint2D);
					} else {
						String errorMsg = parsingResult.getValue();
						if (errorMsg != null && !errorMsg.isEmpty()) {
							JOptionPane.showMessageDialog(this,
								userInput + " isn't a valid position.\n" +
							    errorMsg,
							    "Input Error",
								JOptionPane.ERROR_MESSAGE);
						}
					}
				}
				exit();
			} else { //cancel or close dialog
				exit();
			}
		}
	}

	public void exit() {
		dispose();
	}

	public void withFeature(Feature feature) {
		this.currentFeature = feature;
	}

	public void withVertex(Point2D vertex) {
		this.currentVertex = vertex;
	}

	public void withMouseHandler(FeatureMouseHandler featureMouseHandler) {
		this.handlerInstance = featureMouseHandler;
	}

	public void withPoints(List<String> varPoints) {
		this.varText.clear();
		this.varText.addAll(varPoints);
		editcoordspanel = new EditCoordsPanel(this.varText);
		optionPane.setMessage(editcoordspanel.getCoordinatesInputLayout());
		pack();
	}


	private Ordering getCoordOrdering() {
		String coordOrdering = Config.get(Config.CONFIG_LAT_LON, Ordering.LAT_LON.asString());
		Ordering ordering = Ordering.get(coordOrdering);
		return ordering;
	}	

}
