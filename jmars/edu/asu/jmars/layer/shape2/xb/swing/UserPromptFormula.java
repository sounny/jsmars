package edu.asu.jmars.layer.shape2.xb.swing;

/**
 * 
 * prompts that we show to the user as tips for building a formula.
 */
public enum UserPromptFormula {
	
	ON_FORMULA_START(
			" Enter  formula  here\n") {
			//+ " Press CTRL-SPACE for autocomplete, after typing at least one character.\n"
		    //+ " Click right mouse button to bring up context menu.\n") 
	},
	PREVIEW_PROMPT_WHEN_ERRORS("A warning has occurred while you were editing this formula.\n"
			+ "To view more details, navigate to the 'Warnings' tab. \n"
			+ "Once you resolve the warnings, the result will be shown here.\n") {
	},
	PREVIEW_PROMPT_WHEN_OK("Result preview based on 1st row:  ") {
	},
	PREVIEW_PROMPT_WHEN_OK2("\nThe complete result for all rows will be calculated when you finish "
			+ "defining the\n formula " + " and press the 'UPDATE  FEATURES' button.\n Also, make sure the 'Update All Rows' is checked.\n") {
		
	},
	COLUMN_REMINDER("\n\nReminder, when typing a column name into your formula,\n"
			+ "surround its name with colons. For example, ") {
	},
	
	DRAG_N_DROP_TIP("<html><div>"
			+ "Tips for inserting a selected column into formula<br>"
			+ "-&nbsp; drag &amp; drop selected column into formula<br>"
			+ "-&nbsp; or single-click on your selection to insert it<br>"
			+ "-&nbsp; or navigation with&nbsp;&#8593; and &#8595;&nbsp; keys, press ENTER<br>"
			+ "</div></html>") {
			
	};
	
	String prompt;	

	private UserPromptFormula(String msg) {
		this.prompt = msg;
	}

	public String asString() {
		return this.prompt;
	}

}

