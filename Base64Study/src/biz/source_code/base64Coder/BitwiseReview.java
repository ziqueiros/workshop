package biz.source_code.base64Coder;

/**
 * Check for reference
 * http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.19
 * http://en.wikipedia.org/wiki/Bitwise_operation#Shifts_in_Java
 * 
 * @author jzamora02
 *
 */
public class BitwiseReview {

	private static final String ZEROS = "00000";
	
	public static void main(String... args){

		int bitmask = 0x000F;
	    int val = 0x2222;
	    
	    // prints "2"	    
	    System.out.println("Bitmask " + (val & bitmask));
   
	    val = 0xF00F;
	    //The unsigned right shift operator ">>>" shifts a zero into the leftmost 
	    //position, while the leftmost position after ">>" depends on sign extension.
	    for(int i=0;i<10;i++){
	    	System.out.format(i+" %h - ",val); //%n platform independent newline character
	    	System.out.format("%d%n",val); //%n platform independent newline character
	    	val = val >>> 1; //BE CAREFULL YOU ARE SHIFTING ONE POSITION	    	
	    }
	    
	    //Keep in mind
	    //If n is positive, then the result is the same as that of n >> s.
	    //If n is negative and the type of the left-hand operand is int, then the result is equal to that of the expression (n >> s) + (2 << ~s).
	    //If n is negative and the type of the left-hand operand is long, then the result is equal to that of the expression (n >> s) + (2L << ~s).
	    
	    
	    val = 0xF00F;
	    //The unsigned right shift operator ">>>" shifts a zero into the leftmost 
	    //position, while the leftmost position after ">>" depends on sign extension.
	    for(int j=0;j<10;j++){
	    	System.out.format(j+" %h - ",val); //%n platform independent newline character
	    	System.out.format("%d%n",val); //%n platform independent newline character
	    	val = val >> 1;	    	
	    }	    

	    System.out.println(Integer.toBinaryString(0xF00F));
	    System.out.println(Integer.toBinaryString(0xF00F>>>1));
	    
	    System.out.println(Integer.toBinaryString(-1));
	    System.out.println(Integer.toBinaryString(-1>>>30));
	    
	    //See http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.19
    	//If the promoted type of the left-hand operand is int, only the five lowest-order 
	    //bits of the right-hand operand are used as the shift distance. It is as if the right-hand operand 
	    //were subjected to a bitwise logical AND operator & (�15.22.1) with the mask value 0x1f (0b11111). 
	    //The shift distance actually used is therefore always in the range 0 to 31, INCLUSIVE.
	    //that is -1 >>> 32 is equivalent to -1 >>> 0 and -1 >>> 33 is equivalent to -1 >>> 1 and,
	    //especially confusing, -1 >>> -1 is equivalent to -1 >>> 31
	    System.out.println(Integer.toBinaryString(-1>>>32));
	    
	}
	
	
	
	/**
	 * Formats the string to the specified length using the padString and padded either left (PAD_LEFT) or right (PAD_RIGHT)
	 * @param str
	 * @param len
	 * @param padStr
	 * @param padLeft true if pad left, false otherwise (use constants PAD_LEFT, PAD_RIGHT)
	 * @return
	 */
	private static String formatStrLen(final String str, final int len) {
		return str == null || str.length() == 0 ? ZEROS.substring(0, len) :
				str.length() >= len ? str : ZEROS.substring(str.length(), len) + str;
	}
    
    
}
