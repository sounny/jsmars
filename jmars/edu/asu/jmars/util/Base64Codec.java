package edu.asu.jmars.util;


/**
 * Base64Codec provides encoder / decoder for Base 64 encoding as defined in RFC 2045.
 * The following explanation of the protocol is extracted from RFC 2045:
 <dl>
 <dd>
 <p>
   The Base64 Content-Transfer-Encoding is designed to represent
   arbitrary sequences of octets in a form that need not be humanly
   readable.  The encoding and decoding algorithms are simple, but the
   encoded data are consistently only about 33 percent larger than the
   unencoded data.  This encoding is virtually identical to the one used
   in Privacy Enhanced Mail (PEM) applications, as defined in RFC 1421.
<p>
   A 65-character subset of US-ASCII is used, enabling 6 bits to be
   represented per printable character. (The extra 65th character, &quot;=&quot;,
   is used to signify a special processing function.)
<p>
</dl>
 *
 * This implementation is based on the com.sun.util BASE64Encoder and BASE64Decoder
 * classes written by Chuck McManis.  It does not handle embedded carriage return
 * or line feed characters.
 *
 * @author Chuck McManis
 *
 **/
public class Base64Codec {

    private char base64[] = {
		'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P',
		'Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f',
		'g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v',
		'w','x','y','z','0','1','2','3','4','5','6','7','8','9','+','/'
	};

    protected char encoding[];

    static private Base64Codec _instance = new Base64Codec();

    protected Base64Codec () {
        encoding = base64;
    }

    /**
        There's no need for more than one instance.  Use this method to
        retrieve it.
    */
    public static Base64Codec getInstance() {
        return _instance;
    }

	/**
	 * Encode a String into Base64 format.
	 *
	 * @param source    Original String
	 * @return          Encoded string
	 *
	 **/
    public String encode (String source) {
        int srcLength = source.length();
        int chunks = ((srcLength % 3) == 0) ? srcLength / 3 : srcLength / 3 + 1;

        byte [] src = source.getBytes();
        char [] dst = new char[chunks * 4];

	    for (int j = 0, k = 0; j < srcLength; j += 3, k += 4) {
		    if ((j + 3) <= srcLength) {
		        encodeAtom (src, j, dst, k, 3);
		    } else {
		        encodeAtom (src, j, dst, k, (srcLength - j));
		    }
		}

        return new String (dst);
    }

    /**
     * enocodeAtom - Take up to three bytes of input and encode it as 4
     * printable characters. Note that if the length in len is less
     * than three is encodes either one or two '=' signs to indicate
     * padding characters.
     */
    protected void encodeAtom(byte dataIn[], int inOffset, char dataOut[], int outOffset, int len) {
	    byte a, b, c;

	    if (len == 1) {
	        a = dataIn[inOffset];
	        b = 0;
	        c = 0;
	        dataOut[outOffset] = (encoding[(a >>> 2) & 0x3F]);
	        dataOut[outOffset+1] = (encoding[((a << 4) & 0x30) + ((b >>> 4) & 0xf)]);
	        dataOut[outOffset+2] = '=';
	        dataOut[outOffset+3] = '=';
	    } else if (len == 2) {
	        a = dataIn[inOffset];
	        b = dataIn[inOffset+1];
	        c = 0;
	        dataOut[outOffset] = (encoding[(a >>> 2) & 0x3F]);
	        dataOut[outOffset+1] = (encoding[((a << 4) & 0x30) + ((b >>> 4) & 0xf)]);
	        dataOut[outOffset+2] = (encoding[((b << 2) & 0x3c) + ((c >>> 6) & 0x3)]);
	        dataOut[outOffset+3] = ('=');
	    } else {
	        a = dataIn[inOffset];
	        b = dataIn[inOffset+1];
	        c = dataIn[inOffset+2];
	        dataOut[outOffset] = (encoding[(a >>> 2) & 0x3F]);
	        dataOut[outOffset+1] = (encoding[((a << 4) & 0x30) + ((b >>> 4) & 0xf)]);
	        dataOut[outOffset+2] = (encoding[((b << 2) & 0x3c) + ((c >>> 6) & 0x3)]);
	        dataOut[outOffset+3] = (encoding[c & 0x3F]);
	    }
    }

	/**
	 * Decode a String stored in Base64 format.
	 *
	 * @param source    Encoded String
	 * @return          Decoded String
	 *
	 **/
    public String decode (String source)  {
        int srcLength = source.length();
        int dstLength = 0;
        int chunks = (srcLength / 4);

        char [] src = source.toCharArray();
        byte [] dst = new byte[chunks * 3];

        try {
	    for (int j = 0, k = 0; j < srcLength; j += 4, k += 3) {
            dstLength += decodeAtom (src, j, dst, k);
		}

        } catch ( Exception e) {

          //Error - just return source
          return source;
        }

        return new String (dst, 0, dstLength);
    }

    /**
     * Decode one BASE64 atom (4 character span) into 1, 2, or 3 bytes of data.
     * Returns the number of characters decoded.
     */
    protected int decodeAtom (char inData[], int inOffset, byte outData[], int outOffset)
        throws Exception {
        int i, len = 3;
	    byte	a = (byte)-1, b = (byte)-1, c = (byte)-1, d = (byte)-1;
        try {
	        for (i = 0; i < 64; i++) {
	            if (inData[inOffset] == encoding[i]) {
    		        a = (byte) i;
	            }
	            if (inData[inOffset+1] == encoding[i]) {
	    	        b = (byte) i;
	            }
	            if (inData[inOffset+2] == encoding[i]) {
		            c = (byte) i;
	            }
	            if (inData[inOffset+3] == encoding[i]) {
		            d = (byte) i;
	            }
	        }

	        if ( inData[inOffset+3] == '=') {	// correct length based on pad byte
	            len = (inData[inOffset+2] == '=') ? 1 : 2;
	        }

	        if ((len == 2) && (inData[inOffset+3] != '=')) {
	            throw new Exception ("Base64Decoder: Bad Padding byte 2.");
	        }

	        if ((len == 1) &&
	            ((inData[inOffset+2] != '=') || (inData[inOffset+3] != '='))) {
	            throw new Exception ("Base64Decoder: Bad Padding byte 1.");
	        }

	        switch (len) {
	            case 1:
	                outData[outOffset] = ( (byte)(((a << 2) & 0xfc) | ((b >>> 4) & 3)) );
	                break;
	            case 2:
	                outData[outOffset] = ( (byte) (((a << 2) & 0xfc) | ((b >>> 4) & 3)) );
	                outData[outOffset+1] = ( (byte) (((b << 4) & 0xf0) | ((c >>> 2) & 0xf)) );
	                break;
	            case 3:
	                outData[outOffset] = ( (byte) (((a << 2) & 0xfc) | ((b >>> 4) & 3)) );
	                outData[outOffset+1] = ( (byte) (((b << 4) & 0xf0) | ((c >>> 2) & 0xf)) );
	                outData[outOffset+2] = ( (byte) (((c << 6) & 0xc0) | (d  & 0x3f)) );
	                break;
	        }
	    } catch (ArrayIndexOutOfBoundsException e) {
	        throw new Exception ("Base64Decoder: Bad data length.");
	    }

	    return len;
    }

    //For testing.

    static public void main (String args[]) {


        if (args.length == 0) {
            System.out.println ("USAGE:  com.etractions.util.Base64Codec [-d] string");
            System.out.println ("        -d Decode given string\n");
            System.out.println ("  If -d is not specified, the string will be encoded");
            System.out.println ("  and then decoded");
            System.exit (0);
        }

        Base64Codec codec = Base64Codec.getInstance();

        try {
            String encoded;

            if (args[0].equals("-d")) {
                if (args.length < 2) {
                    System.out.println ("Must provide encoded string.");
                    System.exit(1);
                }
                encoded = args[1];
            } else {
                System.out.println ("Original String: " + args[0]);
                encoded = codec.encode (args[0]);
            }
            System.out.println ("Encoded: " + encoded);
            String decoded = codec.decode (encoded);
            System.out.println ("Decoded: " + decoded);
        } catch (Exception e) {
            System.err.println ("Exception: " + e.getMessage());
        }
    }

}
