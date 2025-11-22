package edu.asu.jmars.util;

import java.io.*;

/**
 ** Utility class for sending output to two OutputStream objects at
 ** once.
 **
 ** <p>You simply instantiate the class by supplying two streams, then
 ** all output operations are proxied to both streams in sequence. A
 ** given operation is always performed on out1 first, and then on
 ** out2. The operation is performed on out2 regardless of whether or
 ** not out1 caused an IOException to be thrown.
 **
 ** <p>Since a single operation is thus capable of generating two
 ** IOExceptions (in the event that BOTH underlying streams throw
 ** one), all member functions of this class are declared to throw
 ** {@link DualIOException}, which wraps one or two IOExceptions.
 **
 ** <p>Non-IOException exceptions propagate out of all methods on
 ** their own, without being wrapped in a DualIOException. Note that
 ** this means a non-IOException is capable of resulting in failed
 ** parallelism for the two streams.
 **
 ** <p>No added synchronization is provided by this class, beyond
 ** whatever may already be present in the underlying streams.
 **/
public class DualOutputStream extends OutputStream
 {
    /**
     ** The first underlying output stream.
     **/
    protected OutputStream out1;

    /**
     ** The second underlying output stream.
     **/
    protected OutputStream out2;

    /**
     ** Constructs a DualOutputStream from two component streams.
     **
     ** @throws NullPointerException if either out1 or out2 is null.
     **/
    public DualOutputStream(OutputStream out1, OutputStream out2)
     {
	if(out1 == null) throw  new NullPointerException("out1 is null");
	if(out2 == null) throw  new NullPointerException("out2 is null");
	this.out1 = out1;
	this.out2 = out2;
     }

    /**
     ** Calls close() on both streams.
     **
     ** @throws DualIOException If either or both of out1/out2 caused
     ** an exception.
     **/
    public void close()
     throws DualIOException
     {
	IOException e1 = null;
	IOException e2 = null;

	try { out1.close(); } catch(IOException e) { e1 = e; }
	try { out2.close(); } catch(IOException e) { e2 = e; }

	if(e1 != null  ||  e2 != null)
	    throw  new DualIOException(e1, e2);
     }

    /**
     ** Calls flush() on both streams.
     **
     ** @throws DualIOException If either or both of out1/out2 caused
     ** an IOException.
     **/
    public void flush()
     throws DualIOException
     {
	IOException e1 = null;
	IOException e2 = null;

	try { out1.flush(); } catch(IOException e) { e1 = e; }
	try { out2.flush(); } catch(IOException e) { e2 = e; }

	if(e1 != null  ||  e2 != null)
	    throw  new DualIOException(e1, e2);
     }

    /**
     ** Calls write() on both streams.
     **
     ** @throws DualIOException If either or both of out1/out2 caused
     ** an IOException.
     **
     ** @throws NullPointerException If b is null.
     **/
    public void write(byte[] b)
     throws DualIOException
     {
	if(b == null)
	    throw  new NullPointerException("b is null");

	IOException e1 = null;
	IOException e2 = null;

	try { out1.write(b); } catch(IOException e) { e1 = e; }
	try { out2.write(b); } catch(IOException e) { e2 = e; }

	if(e1 != null  ||  e2 != null)
	    throw  new DualIOException(e1, e2);
     }

    /**
     ** Calls write() on both streams.
     **
     ** @throws DualIOException If either or both of out1/out2 caused
     ** an IOException.
     **
     ** @throws NullPointerException If b is null.
     **
     ** @throws ArrayIndexOutOfBoundsException If off is negative, len
     ** is negative, or off+len is greater than the length of the
     ** array b.
     **/
    public void write(byte[] b, int off, int len)
     throws DualIOException
     {
	if(b == null)
	    throw  new NullPointerException("b is null");
	if(off < 0  ||  len < 0  ||  off+len > b.length)
	    throw  new ArrayIndexOutOfBoundsException();

	IOException e1 = null;
	IOException e2 = null;

	try { out1.write(b, off, len); } catch(IOException e) { e1 = e; }
	try { out2.write(b, off, len); } catch(IOException e) { e2 = e; }

	if(e1 != null  ||  e2 != null)
	    throw  new DualIOException(e1, e2);
     }

    /**
     ** Calls write() on both streams.
     **
     ** @throws DualIOException If either or both of out1/out2 caused
     ** an IOException.
     **/
    public void write(int b)
     throws DualIOException
     {
	IOException e1 = null;
	IOException e2 = null;

	try { out1.write(b); } catch(IOException e) { e1 = e; }
	try { out2.write(b); } catch(IOException e) { e2 = e; }

	if(e1 != null  ||  e2 != null)
	    throw  new DualIOException(e1, e2);
     }
 }
