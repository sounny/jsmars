package edu.asu.jspice;


public class SpiceException extends RuntimeException {
    private final String explaination;
    private final String long_message;

    public SpiceException() {
        super();
        this.explaination = null;
        this.long_message = null;
    }
    public SpiceException( String spice_error) {
    	this(spice_error, null, null);
    }
    public SpiceException( String spice_error, String explaination ) {
        this(spice_error, explaination, null);
    }
    public SpiceException( String spice_error, String explaination, String long_message ) {
        super( spice_error );
        this.explaination = explaination;
        this.long_message = long_message;
    }

    public String getExplaination(){
        return explaination;
    }

    public String getLongMessage(){
        return long_message;
    }

    public String toString(){
        return super.toString() + (explaination == null || explaination.trim().length() == 0? "": ": "+explaination);
    }
}

