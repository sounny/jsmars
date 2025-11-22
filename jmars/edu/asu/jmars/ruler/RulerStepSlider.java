package edu.asu.jmars.ruler;

import java.io.Serializable;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class RulerStepSlider extends JSlider implements RulerComponent {
	public RulerStepSlider(int min, int max, int step, int initial){
		super(min, max, initial);
		setMajorTickSpacing(step);
		setSnapToTicks(true);
		setPaintTicks(true);
		setPaintLabels(true);
	
		getModel().addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				RulerManager.Instance.notifyRulerOfViewChange();
			}
		});
	}
	
	public Settings getSettings(){
		Settings s = new Settings(
				getMinimum(),
				getMaximum(),
				getMajorTickSpacing(),
				getValue());
		
		return s;
	}
	
	public void restoreSettings(Settings s){
		setMinimum(s.min);
		setMaximum(s.max);
		setMajorTickSpacing(s.step);
		setValue(s.initial);
	}
	
	public final static class Settings implements Cloneable, Serializable {
		public Settings(){
			min = -10;
			max =  10;
			step = 2;
			initial = 0;
		}
		
		public Settings(int min, int max, int step, int initial){
			this.min = min;
			this.max = max;
			this.step = step;
			this.initial = initial;
		}
		public int min;
		public int max;
		public int step;
		public int initial;
	}
}
