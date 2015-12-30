package org.gjt.cuspy.unicode;

import org.gjt.cuspy.Rethrown;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

/**
 * Bidirectional map between Unicode characters and their official,
 * standardized Unicode names.  If you provide any sort of API, tool, or
 * language where characters might have to be specified, don't make your
 * users thumb through the
 * <A HREF="http://charts.unicode.org/Unicode.charts/normal/Unicode.html">Unicode charts</A>
 * to find out that, e.g., <STRONG>\u005cu20a8</STRONG>
 * is the Indian currency symbol. Let them give the official name
 * <STRONG>RUPEE SIGN</STRONG>
 * and let this class look it up.  Don't have users squinting at a display to
 * distinguish confusable characters like
 * <STRONG>GREEK CAPITAL LETTER PI</STRONG> and <STRONG>N-ARY PRODUCT</STRONG>.
 * Let them select the character of interest and use this class
 * to display the official name.  Make programs easier to write and easier
 * to read, and help the Unicode names start to become familiar to programmers.
 * Includes all Unicode 3.0 names including the Chinese-Japanese-Korean unified
 * ideographs and the Johab Hangul syllables.  Unicode 1.0 names are also
 * accepted where they were different.  All that adds only a 61 kB jar
 * to your project, so why not include it?
 *<P>
 * If your project involves a compiler or preprocessor, then of course you
 * would do all character mapping there and pay no run-time price.  If you will
 * be mapping character names at run-time, you may have to think about speed.
 * This class is designed to add a small memory footprint to your
 * project, at the cost of using a compact data structure and simple
 * lookup, a linear search starting at the low (ASCII, Latin1) characters.
 * The lookup is therefore fast for those very common cases, but slower
 * for characters high in the Unicode range.  On a SPARC ULTRA 1, the maximum
 * lookup time (for <STRONG>REPLACEMENT CHARACTER \u005cufffd</STRONG>) is
 * about 45 milliseconds.  The average time for successful lookups over all
 * names that are not algorithmically defined was 27 ms.  Times for code to name
 * and name to code mapping are similar.  The CJK ideographs and Hangul
 * syllables are mapped in constant time without table lookup and they outnumber
 * the looked-up characters by a large margin; including them
 * in the averages would have made the average time look much better.
 *<P>
 * These times may be just fine for most applications.
 * If the time is a concern, you can extend NameMap with a subclass that
 * overrides {@link #nameLookup(int) nameLookup} and
 * {@link #codeLookup(String) codeLookup} to do some form of caching
 * appropriate to your application.
 *<P>
 * Would you like to be able to write
 *<PRE>
 * char c = 'CLOCKWISE INTEGRAL';
 *</PRE>
 * in Java?  Log on to the
 *<A HREF="http://developer.java.sun.com/developer/">Java Developer
 * Connection</A> and cast one, two, or all three of your Bug Votes for
 *<A HREF="http://developer.java.sun.com/developer/bugParade/bugs/4311312.html">
 *Request for Enhancement #4311312</A>.
 *@author <A HREF="mailto:chap@gjt.org">Chapman Flack</A>
 *@version $Id$
 */
public class NameMap {
  /**An instance of NameMap (the only one you'll ever need)*/
  public static final NameMap map = new NameMap();
  
  /**Allow subclasses to instantiate*/
  protected NameMap() { }
  
  /**The prefix of the names of Chinese-Japanese-Korean ideographs*/
  private static final String cjkPrefix = "CJK UNIFIED IDEOGRAPH-";
  /**The prefix of the names of Hangul syllables*/
  private static final String hsPrefix  = "HANGUL SYLLABLE ";
  /**The short names of the Hangul choseong jamo*/
  private static final String[] choseong = { "G", "GG", "N", "D", "DD", "L",
    "M", "B", "BB", "S", "SS", "", "J", "JJ", "C", "K", "T", "P", "H" };
  /**The short names of the Hangul jungseong jamo*/
  private static final String[] jungseong = { "A", "AE", "YA", "YAE", "EO",
    "E", "YEO", "YE", "O", "WA", "WAE", "OE", "YO", "U", "WEO", "WE", "WI",
    "YU", "EU", "YI", "I" };
  /**The short names of the Hangul jongseong jamo*/
  private static final String[] jongseong = { "G", "GG", "GS", "N", "NJ", "NH",
    "D", "L", "LG", "LM", "LB", "LS", "LT", "LP", "LH", "M", "B", "BS", "S",
    "SS", "NG", "J", "C", "K", "T", "P", "H" };
  /**Base of the Hangul syllables area*/
  private static final char SBase = '\uac00';
  /**Next value after the Hangul syllables area*/
  private static final char Send  = (char)(11172 + SBase); // exclusive!
  /**Count of Leading (choseong) jamo*/
  private static final short LCount = 19;
  /**Count of Vocalic (jungseong) jamo*/
  private static final short VCount = 21;
  /**Count of Trailing (jongseong) jamo*/
  private static final short TCount = 28;
  /**Count of jungseong/jongseong combinations*/
  private static final short NCount = (short)(VCount * TCount);

  /**Will hold the deflated name file in memory (<56kB) after first need*/
  private static byte[] data;

  /**Populate the data array by reading the data file from the jar*/
  private static void init() throws IOException {
    ClassLoader cl = NameMap.class.getClassLoader();
    java.net.URL url;
    if ( cl != null )
      url = cl.getResource( "names.data");
    else
      url = ClassLoader.getSystemResource( "names.data"); // sop for jdk1.1
    InputStream is = url.openStream();
    byte[] buf = new byte [ 56791 ]; // one more than resource size
    int i = 0, j;
    for ( ; i < buf.length ; ) {
      j = is.read( buf, i, buf.length - i);
      if ( j == -1 )
      	break;
      i += j;
    }
    if ( i != buf.length - 1 )
      throw new IOException( "names.data wrong length");
    data = buf;
  }
  
  /**
   * Find the character code for a given character name.
   * @param name The Unicode 3.0 name of a Unicode character.
   * If the character had a different name in Unicode 1.0, that name is also
   * accepted, so, e.g., <CODE>FULL STOP</CODE> and <CODE>PERIOD</CODE> both
   * work. C0 control names (e.g. <CODE>LINE FEED</CODE>) are also accepted.
   * @return The character code (as an <CODE>int</CODE> to allow for addition
   * of names in Unicode planes 1 and above).
   * @throws NoSuchCharException If <EM>name</EM> was a complete surprise.
   * @throws NoUnicodeDBException If data could not be read from the jar.
   */
  public int code( String name) throws NoSuchCharException {
    int c;
    if ( name.startsWith( cjkPrefix) ) {
      try {
      	c = Integer.parseInt( name.substring( cjkPrefix.length()), 16);
	if ( 0x4e00  <= c && c <=  0x9fa5 )
	  return c;
	if ( 0x3400  <= c && c <=  0x4db5 )
	  return c;
      }
      catch ( NumberFormatException e ) { }
    }
    else if ( name.startsWith( hsPrefix) ) {
      String s = name.substring( hsPrefix.length());
      int L = 0, V = 0, T = 0;
      int i, longest = 0;
      
      for ( i = 0; i < choseong.length; ++i ) {
      	if ( s.startsWith( choseong[i]) ) {
	  if ( choseong[i].length() > longest ) {
	    L = i;
	    longest = choseong[i].length();
	  }
	}
      }
      s = s.substring( longest);
      longest = 0;
      for ( i = 0; i < jungseong.length; ++i ) {
      	if ( s.startsWith( jungseong[i]) ) {
	  if ( jungseong[i].length() > longest ) {
	    V = i;
	    longest = jungseong[i].length();
	  }
	}
      }
      if ( longest > 0 ) {
 	s = s.substring( longest);
	if ( s.length() > 0 ) {
 	  for ( i = 0; i < jongseong.length; ++i )
 	    if ( s.equals( jongseong[i]) ) {
	      T = i + 1;
	      break;
	    }
	}
 	if ( i < jongseong.length )
 	  return (char)((L * VCount + V) * TCount + T + SBase);
      }
    }
    c = codeLookup( name);
    if ( c == -1 )
      throw new NoSuchCharException( name);
    return c;
  }

  /**
   * Return the name of a given Unicode character.
   * @param c A character code (as an <CODE>int</CODE> to allow for addition
   * of names in the Unicode planes 1 and above).
   * @return The name of the character. If it has more than one name, the
   * canonical Unicode 3.0 name is returned.  C0 controls (which don't have
   * canonical Unicode 3.0 names) have their earlier names returned.
   * @throws IllegalArgumentException If <EM>c</EM> is not between 0 and
   * 0x10ffff inclusive.
   * @throws NoSuchCharException If <EM>c</EM> is an undefined Unicode value.
   * @throws NoUnicodeDBException If data could not be read from the jar.
   */
  public String name( int c) throws NoSuchCharException {
    if ( 0  > c || c >  0x10ffff )
      throw new IllegalArgumentException( String.valueOf( c));
    if ( 0x4e00  <= c && c <=  0x9fa5 )
      return cjkPrefix + Integer.toHexString( c).toUpperCase();
    if ( 0x3400  <= c && c <=  0x4db5 )
      return cjkPrefix + Integer.toHexString( c).toUpperCase();
    if ( SBase   <= c && c <   Send ) {
      int SIndex = c - SBase;
      int L = SIndex / NCount;
      int V = (SIndex % NCount) / TCount;
      int T = SIndex % TCount;
      return hsPrefix + choseong[L] + jungseong[V] + ((T>0)?jongseong[T-1]:"");
    }
    String n = nameLookup( c);
    if ( n != null )
      return n;
    throw new NoSuchCharException( Integer.toHexString( c));
  }
  
  /**Look up a character name in the compressed table, returning the code.
   * Should be called after checking for a CJK unified ideograph or Hangul
   * syllable, which are not in the table.
   * May be overridden if a faster table lookup is desired.
   *@param name The character name to look up.
   *@return The corresponding code, or -1 if no match was found in the table.
   *@throws NoUnicodeDBException if the table data could not be accessed
   */
  protected int codeLookup( String name) {
    if ( data == null ) {
      try {
      	init();
      }
      catch ( IOException e ) {
      	throw new NoUnicodeDBException( e);
      }
    }
    
    Inflater f = new Inflater( true);
    f.setInput( data);
    byte[] buf = new byte [ 512 ];
    int off = 0, got = 0;
    
    int len = name.length();
    byte[] b;
    try {
      b = name.getBytes( "8859_1");
    }
    catch ( UnsupportedEncodingException e ) {
      throw new NoUnicodeDBException( e);
    }
    int noff = -1;
    boolean inSkip = false;
    
    int code = 0;
    
    inflate: for ( ;; ) {
      off -= got;
      
      try {
        got = f.inflate( buf);
      }
      catch ( DataFormatException e ) {
        throw new NoUnicodeDBException( e);
      }
      
      if ( got == 0 )
        break;
      
      completion: while ( noff != -1 ) {
        int beg = noff;
        noff = len;
        if ( noff > got )
          noff = got;
        int i;
        for ( i = beg; i < noff; ++i ) {
          if ( buf [ off ] != b [ i ] ) {
            off += len - i;
            noff = -1;
            ++ code;
            break completion;
          }
          ++ off;
        }
        if ( i != len )
          continue inflate;
        f.end();
        return code;
      }
      
      if ( inSkip ) {
        code += buf [ off++ ] & 0xff;
        inSkip = false;
      }

      scan: while ( off < got ) {
        int bi = buf [ off++ ] & 0xff;
        if ( bi == 255 ) {
          -- code;
          continue scan;
        }
        if ( bi >= 91 ) {
          code += ( bi - 91 ) * 256;
          if ( off >= got ) {
            inSkip = true;
            continue inflate;
          }
          code += buf [ off++ ] & 0xff;
          continue scan;
        }

        if ( bi != len ) {
          ++ code;
          off += bi;
          continue scan;
        }
        noff = got - off;
        if ( noff > len )
          noff = len;
        int i;
        for ( i = 0; i < noff; ++i ) {
          if ( buf [ off ] != b [ i ] ) {
            off += len - i;
            noff = -1;
            ++ code;
            continue scan;
          }
          ++ off;
        }
        if ( i != len )
          continue inflate;
        f.end();
        return code;
      }
    }
    boolean fin = f.finished();
    f.end();
    if ( noff == -1  &&  ! inSkip  &&  fin )
      return -1;
    throw new NoUnicodeDBException();
  }
  
  /**Look up a character code in the compressed table, returning the name.
   * Should be called after checking for a CJK unified ideograph or Hangul
   * syllable, which are not in the table.
   * May be overridden if a faster table lookup is desired.
   *@param code The character code to look up.
   *@return The corresponding name, or null if no match was found in the table.
   *@throws NoUnicodeDBException if the table data could not be accessed
   */
  protected String nameLookup( int code) {
    if ( data == null ) {
      try {
      	init();
      }
      catch ( IOException e ) {
      	throw new NoUnicodeDBException( e);
      }
    }
    
    Inflater f = new Inflater( true);
    f.setInput( data);
    byte[] buf = new byte [ 512 ];
    int off = 0, got = 0, len = 0;
    int noff = 0;
    
    boolean inSkip = false;
    
    int c = 0;
    StringBuffer n = null;
    
    inflate: for ( ;; ) {
      off -= got;
      
      try {
        got = f.inflate( buf);
      }
      catch ( DataFormatException e ) {
        throw new NoUnicodeDBException( e);
      }
      
      if ( got == 0 )
        break;
      
      if ( n != null ) {
        int bl = len - noff;
        if ( bl > got )
          bl = got;
        try {
          n.append( new String( buf, off, bl, "8859_1"));
        }
        catch ( UnsupportedEncodingException e ) {
          throw new NoUnicodeDBException( e);
        }
        noff += bl;
        if ( noff == len ) {
          f.end();
          return n.toString();
        }
        off += bl;
        continue inflate;
      }
      
      if ( inSkip ) {
        c += buf [ off++ ] & 0xff;
        inSkip = false;
      }

      scan: while ( off < got ) {
        len = buf [ off++ ] & 0xff;
        if ( len == 255 ) {
          -- c;
          continue scan;
        }
        if ( len >= 91 ) {
          c += ( len - 91 ) * 256;
          if ( off >= got ) {
            inSkip = true;
            continue inflate;
          }
          c += buf [ off++ ] & 0xff;
          continue scan;
        }
        if ( c < code ) {
          ++ c;
          off += len;
          continue scan;
        }
        if ( c > code ) {
          f.end();
          return null;
        }
        int bl = got - off;
        if ( bl > len )
          bl = len;
        try {
          n = new StringBuffer( new String( buf, off, bl, "8859_1"));
        }
        catch ( UnsupportedEncodingException e ) {
          throw new NoUnicodeDBException( e);
        }
        if ( bl == len ) {
          f.end();
          return n.toString();
        }
        off += bl;
        noff = bl;
        continue inflate; 
      }
    }
    boolean fin = f.finished();
    f.end();
    if ( n == null  &&  ! inSkip  &&  fin )
      return null;
    throw new NoUnicodeDBException();
  }
  
  /**Thrown when name data cannot be read from the jar. Should Never Happen.*/
  public static class NoUnicodeDBException extends Rethrown.RuntimeException {
    /**Construct from the original Throwable indicating the underlying problem*/
    NoUnicodeDBException( Throwable t) { super( t, 1); }
    NoUnicodeDBException() { super( "Data table corrupt"); }
  }
  /**Thrown when name or code lookup fails*/
  public static class NoSuchCharException extends Rethrown.Exception {
    /**Construct from a descriptive string, the offending name, or code in hex*/
    NoSuchCharException( String s) { super( s); }
  }
  
  /**
   * A test driver.  java -jar NameMap.jar args...<BR>
   * where each <EM>arg</EM> is a character name (quoted to protect spaces) or
   * a hexadecimal number (without 0x prefix). Prints the number for each name
   * (in hex) and the name for each number.
   */
  public static void main( String[] args) throws Exception {
    for ( int i = 0; i < args.length; ++i ) {
      try {
      	int c = Integer.parseInt( args[i], 16);
      	System.out.println( map.name( c));
      }
      catch ( NumberFormatException e ) {
      	System.out.println(
	  Integer.toHexString( map.code( args[i])).toUpperCase());
      }
    }
  }
}
