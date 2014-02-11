package xmlstudy;

import javax.xml.stream.*;
import java.net.URL;
import java.io.*;

/*
 * XMLStreamReader is the key interface in StAX. This interface represents a cursor
   that's moved across an XML document from beginning to end. At any given time, this
   cursor points at one thing: a text node, a start-tag, a comment, the beginning of
   the document, etc. The cursor always moves forward, never backward, and normally
   only moves one item at a time. You invoke methods such as getName and getText on
   the XMLStreamReader to retrieve information about the item the cursor is currently
   positioned at.
   
   http://www.xml.com/pub/a/2003/09/17/stax.html

 */
	
public class StAXReaderPOC {

	  public static void main(String[] args) 
	  {
		  
		  if (args.length == 0) {
		      System.err.println("Usage: java XHTMLOutliner url" );
		      return;
		    }
		    String input = args[0];

		  
		    try {
			      //URL u = new URL(input);
			      //InputStream in = u.openStream();
			      
			      File u = new File(input);
			      FileInputStream in = new FileInputStream(u);
			      
			      XMLInputFactory factory = XMLInputFactory.newInstance();
			      XMLStreamReader parser = factory.createXMLStreamReader(in);
			        
			      int inHeader = 0;
			      int sideHeader = 0;
			      for (int event = parser.next();  
			       event != XMLStreamConstants.END_DOCUMENT;
			       event = parser.next()) {			    	  

				        switch (event) {
				          case XMLStreamConstants.START_ELEMENT:
			        		  if(isZoneSide(parser)){
				        		  sideHeader+=1;
				        		  break;
				        	  }

				        	  if(isCard(parser.getLocalName()))
				        	  {
				        		  int copies = Integer.parseInt(parser.getAttributeValue(0));
				        		  //for (int i = 0 ; i < copies ; i++)
				        		  
					        	  if(sideHeader > 0){					        		  
					        		  System.out.println("SB: "+copies +" "+parser.getAttributeValue(2));				        		  
					        	  }else{				        		  
					        		  System.out.println(copies +" "+parser.getAttributeValue(2));	        		  
					        	  }
				        		  inHeader++;
				        	  }
				        	  
				            break;
				          case XMLStreamConstants.END_ELEMENT:
				        	  if(isCard(parser.getLocalName()))
				        		  inHeader--;
				        	  //System.out.println(parser.getLocalName());
				            break;
				          case XMLStreamConstants.CHARACTERS:
				        	  if(inHeader > 0){
				        		  System.out.print(parser.getText());  
				        	  }				        	  
				            break;
				          case XMLStreamConstants.CDATA:
				        	  if(inHeader > 0){
				        		  System.out.print(parser.getText());  
				        	  }
				            break;
				        } // end switch
				      
			    	  
			      } // end while
			      parser.close();
			    }
		    catch (XMLStreamException ex) {
		       return;
		       //ex.printStackTrace();
		    }
		    catch (IOException ex) {
		      System.out.println("IOException while parsing " + input);
		      ex.printStackTrace();
		    }
		  
	  }
	  
	  private static boolean isZoneSide(XMLStreamReader parser){
		   
		  if (parser.getLocalName().equals("zone") && parser.getAttributeCount() > 0)
			  if(parser.getAttributeValue(0).equals("side"))
				  return true;
			  
		  return false;
	  }	  
	  
	  private static boolean isCard(String name){
		  if (name.equals("card")) 
			  return true;
		  return false;
	  }
	  
	  public void parseXHTMLHeaders(String[] args)
	  {

	    if (args.length == 0) {
	      System.err.println("Usage: java XHTMLOutliner url" );
	      return;
	    }
	    String input = args[0];

	    try {
	      URL u = new URL(input);
	      InputStream in = u.openStream();
	      XMLInputFactory factory = XMLInputFactory.newInstance();
	      XMLStreamReader parser = factory.createXMLStreamReader(in);
	        
	      int inHeader = 0;
	      for (int event = parser.next();  
	       event != XMLStreamConstants.END_DOCUMENT;
	       event = parser.next()) {
	        switch (event) {
	          case XMLStreamConstants.START_ELEMENT:
	            if (isHeader(parser.getLocalName())) {
	              inHeader++;
	            }
	            break;
	          case XMLStreamConstants.END_ELEMENT:
	            if (isHeader(parser.getLocalName())) {
	              inHeader--;
	              if (inHeader == 0) System.out.println();
	            }
	            break;
	          case XMLStreamConstants.CHARACTERS:
	            if (inHeader > 0)  System.out.print(parser.getText());
	            break;
	          case XMLStreamConstants.CDATA:
	            if (inHeader > 0)  System.out.print(parser.getText());
	            break;
	        } // end switch
	      } // end while
	      parser.close();
	    }
	    catch (XMLStreamException ex) {
	       System.out.println(ex);
	    }
	    catch (IOException ex) {
	      System.out.println("IOException while parsing " + input);
	    }

	  }

	   /**
	    * Determine if this is an XHTML heading element or not
	    * @param  name tag name
	    * @return boolean true if this is h1, h2, h3, h4, h5, or h6; 
	    *                 false otherwise
	    */
	    private static boolean isHeader(String name) {
	      if (name.equals("h1")) return true;
	      if (name.equals("h2")) return true;
	      if (name.equals("h3")) return true;
	      if (name.equals("h4")) return true;
	      if (name.equals("h5")) return true;
	      if (name.equals("h6")) return true;
	      return false;
	    }

	}