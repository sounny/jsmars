package edu.asu.jmars.util;

import java.io.*;

/**
 ** Wrapper for single or double-exceptions thrown in the use of
 ** {@link DualOutputStream}.
 **/
public class DualIOException extends IOException
 {
    private final IOException cause1;
    private final IOException cause2;

    /**
     ** Constructs a DualIOException from a single or pair of
     ** IOExceptions. The cause of this exception is set to cause1 if
     ** it's non-null, otherwise cause2 (the {@link #getCause1} and
     ** {@link #getCause2} methods can be used to more precisely query
     ** for "who threw what"). The message of the exception is
     ** composed from the message(s) of the non-null cause(s) passed
     ** in.
     **/
    public DualIOException(IOException cause1, IOException cause2)
     {
	super(createMessage(cause1, cause2));
	initCause(cause1!=null ? cause1 : cause2);

	this.cause1 = cause1;
	this.cause2 = cause2;
     }

    /**
     ** Returns the exception thrown by the first output stream, if
     ** there is one, otherwise returns the exception thrown by the
     ** second output stream. In all cases, this will in fact return
     ** an IOException.
     **/
    public Throwable getCause()
     {
	return  super.getCause();
     }

    /**
     ** Returns the exception thrown by the first output stream, or
     ** null if there was none.
     **/
    public IOException getCause1()
     {
	return  cause1;
     }

    /**
     ** Returns the exception thrown by the second output stream, or
     ** null if there was none.
     **/
    public Throwable getCause2()
     {
	return  cause2;
     }

    private static String createMessage(Throwable e1, Throwable e2)
     {
	if(e1 == null  &&  e2 == null) return  null;
	if(e1 == null) return  "(stream2) " + e2.getMessage();
	if(e2 == null) return  "(stream1) " + e1.getMessage();
	return  e1.getMessage() + " || " + e2.getMessage();
     }
 }
