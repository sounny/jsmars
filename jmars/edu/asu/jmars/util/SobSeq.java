package edu.asu.jmars.util;

/**
 ** Routines for quasi-random number generation... this is quite
 ** different from pseudo-random numbers. These numbers are in fact
 ** highly correlated and deterministic, with uniform distribution
 ** properties that are useful for optimizing Monte Carlo methods.
 **
 ** Shamelessly stolen out of Section 7.7 "Quasi- (that is, Sub-)
 ** Random Sequences" from Numerical Recipes in C. Modified to be
 ** 2D-specific, and for other small optmization purposes.
 **/
public final class SobSeq
 {
    private static final int MAXBIT = 30;
    private static final int MAXDIM = 6;
    private static final int N = 2;
    private static final double fac = 1.0 / (1L << MAXBIT);

    private long in;
    private final long[] ix = new long[MAXDIM+1];
    private final int[] iu = new int[MAXBIT+1];
    private final long[] mdeg = { 0,1,2,3,3,4,4 };
    private final long[] ip   = { 0,0,1,1,2,1,4 };
    private final long[] iv = new long[MAXDIM*MAXBIT+1];

    /**
     ** Internally initializes a set of MAXBIT direction numbers for each
     ** of MAXDIM different Sobol sequences.
     **/
    public SobSeq()
     {
	long[] tmp = { 0,1,1,1,1,1,1,3,1,3,3,1,1,5,7,7,3,3,5,15,11,5,15,13,9 };
	System.arraycopy(tmp, 0, iv, 0, tmp.length);

	int j,k,l;
	long i,ipp;
	//Initialize, don't return a vector.
	for(k=1; k<=MAXDIM; k++) ix[k] = 0;
	in = 0;
	if(iv[1] != 1) return;

	for(j=1,k=0; j<=MAXBIT; j++,k+=MAXDIM) iu[j] = k;
	// To allow both 1D and 2D addressing.
	for(k=1; k<=MAXDIM; k++)
	 {
	    for(j=1; j<=mdeg[k]; j++) iv[iu[j]+k] <<= (MAXBIT-j);
	    // Stored values only require normalization.
	    for(j=(int)mdeg[k]+1; j<=MAXBIT; j++) 
	     {
		// Use the recurrence to get other values.
		ipp = ip[k];
		i = iv[(int)iu[j-(int)mdeg[k]]+k];
		i ^= (i >> mdeg[k]);
		for(l=(int) mdeg[k]-1; l>=1; l--) 
		 {
		    if((ipp & 1) != 0) i ^= iv[iu[j-l]+k];
		    ipp >>= 1;
		 }
		iv[iu[j]+k] = i;
	     }
	 }
     }

    /**
     ** Call to retrieve a uniformly-distributed quasi-random 2D
     ** vector, in v[0] and v[1]. Returns v. If v is null, allocates a
     ** new array.
     **/
    public double[] next(double[] v)
     {
	if(v == null)
	    v = new double[2];
	int j;
	long im;
	// Calculate the next vector in the sequence.
	im = in++;
	for(j=1; j<=MAXBIT; j++) 
	 {
	    // Find the rightmost zero bit.
	    if((im & 1) == 0) break;
	    im >>= 1;
	 }
	if(j > MAXBIT)
	    throw new IllegalStateException("MAXBIT too small in sobseq");
	im = (j-1) * MAXDIM;

	// XOR the appropriate direction number into each
	// component of the vector and convert to a floating
	// number.
	ix[1] ^= iv[(int)im+1]; v[0] = ix[1]*fac;
	ix[2] ^= iv[(int)im+2]; v[1] = ix[2]*fac;

	return  v;
     }

    public static void main(String[] av)
     {
	int count = Integer.parseInt(av[0]);

	java.text.DecimalFormat f =
	    new java.text.DecimalFormat("0.0000000000");

	SobSeq ss = new SobSeq();
	double[] v = new double[2];

	for(int i=0; i<count; i++)
	 {
	    ss.next(v);
	    System.out.println(v[0] + "\t" + v[1]);
	 }
     }
 }
