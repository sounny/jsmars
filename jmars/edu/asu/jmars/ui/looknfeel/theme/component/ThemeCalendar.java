package edu.asu.jmars.ui.looknfeel.theme.component;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.border.Border;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXMonthView;
import edu.asu.jmars.swing.IconButtonUI;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import mdlaf.utils.MaterialColors;
import mdlaf.utils.MaterialImageFactory;
import mdlaf.utils.icons.MaterialIconFont;


public class ThemeCalendar {
final static ThemeProvider uitheme = ThemeProvider.getInstance();
	
	public static void configureUI(JXDatePicker calendar) {
		
		Color bg = uitheme.getBackground().getContrast();
		Color txt = uitheme.getText().getMain();
		Color seltxt = uitheme.getText().getHighlight();
		
	    JButton dateBtn= (JButton) calendar.getComponent(1);
	    dateBtn.setBackground(bg);
	    Icon calicon = MaterialImageFactory.getInstance().getImage(MaterialIconFont.DATE_RANGE, MaterialColors.COSMO_DARK_GRAY);				   
	    dateBtn.setIcon(calicon);
	    dateBtn.setUI(new IconButtonUI());
	    JFormattedTextField datetxt = calendar.getEditor();
	    datetxt.setBackground(bg);
	    datetxt.setOpaque(false);
	    datetxt.setForeground(txt);
	    datetxt.setPreferredSize(new Dimension(120,30));
	    datetxt.setCaretColor(txt);
	    datetxt.setSelectionColor(seltxt);
	    Border border = BorderFactory.createLineBorder(txt, 1);
        datetxt.setBorder(border);       
        JXMonthView mview = calendar.getMonthView();
        mview.setBackground(seltxt);
        mview.setMonthStringBackground(bg);
        mview.setMonthStringForeground(txt);      
	}
		 	
}
