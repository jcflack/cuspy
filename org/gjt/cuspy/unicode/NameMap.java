package org.gjt.cuspy.unicode;

import org.gjt.cuspy.Rethrown;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * Bidirectional map between Unicode characters and their official,
 * standardized Unicode names.  If you provide any sort of API, tool, or
 * language where characters might have to be specified, don't make your
 * users thumb through the
 * <A HREF="http://charts.unicode.org/Unicode.charts/normal/Unicode.html>Unicode charts</A>
 * to find out that, e.g., \u20a8
 * is the Indian currency symbol. Let them give the official name
 * <CODE>RUPEE SIGN</CODE>
 * and let this class look it up.  Don't make a user squint at a display to
 * distinguish similar characters like <CODE>GREEK CAPITAL LETTER PI</CODE> and
 * <CODE>N-ARY PRODUCT</CODE>.
 * Let them select the character of interest and use this class
 * to display the official name.  Make programs easier to write and easier
 * to read, and help the Unicode names start to become familiar to programmers.
 * Includes all Unicode 2.1 names including the Chinese-Japanese-Korean unified
 * ideographs and the Johab Hangul syllables.  Unicode 1.0 names are also
 * accepted where they were different.  All that adds only a 40 kB jar
 * to your project, so why not include it?
 *<P>
 * If your project involves a compiler or preprocessor, then of course you
 * would do all character mapping there and pay no run-time price.  If you will
 * be mapping character names at run-time, you may have to think about speed.
 * This class is designed to add the smallest possible memory footprint to your
 * project, at the cost of using the most compact data structure and simplest
 * lookup, a linear search starting at the low (ASCII, Latin1) characters.
 * The lookup is therefore fast for those very common cases, but may take up
 * to a few hundred ms for characters high in the Unicode range.  That may be
 * just fine for most applications where you would expect to do lookups only
 * for a few unfamiliar characters.  If the time is a concern, you can extend
 * NameMap with a subclass that does some form of caching appropriate to your
 * application.
 *@author <A HREF="mailto:chap@gjt.org">Chapman Flack</A>
 *@version $Id$
 */
public class NameMap {
  /**An instance of NameMap (the only one you'll ever need)*/
  public static final NameMap map = new NameMap();
  
  /**Prevent any other instances*/
  private NameMap() { }
  
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

  /**Will hold the gzipped name file in memory (<40kB) after first need*/
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
    byte[] buf = new byte [ 40000 ];
    int i = 0, j;
    for ( ;; ) {
      j = is.read( buf, i, buf.length - i);
      if ( j == -1 )
      	break;
      i += j;
    }
    data = new byte [ i ];
    System.arraycopy( buf, 0, data, 0, i);
    buf = null;
    System.gc();
  }
  
  /**
   * Find the character code for a given character name.
   * @param name The Unicode 2.1 name of a Unicode character.
   * If the character had a different name in Unicode 1.0, that name is also
   * accepted, so, e.g., <CODE>FULL STOP</CODE> and <CODE>PERIOD</CODE> both
   * work. C0 control names (e.g. <CODE>LINE FEED</CODE>) are also accepted.
   * @return The character code (as an <CODE>int</CODE> to allow for addition
   * of names in Unicode planes 1 and above).
   * @throws NoSuchCharException If <EM>name</EM> was a complete surprise.
   * @throws NoUnicodeDBException If data could not be read from the jar.
   */
  public int code( String name) throws NoSuchCharException {
    if ( name.startsWith( cjkPrefix) ) {
      try {
      	int c = Integer.parseInt( name.substring( cjkPrefix.length()), 16);
	if ( 0x4e00  <= c && c <=  0x9fff )
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
    if ( data == null ) {
      try {
      	init();
      }
      catch ( IOException e ) {
      	throw new NoUnicodeDBException( e);
      }
    }
    GZIPInputStream s;
    BufferedReader r;
    int code = 0;
    String line;
    try {
      s = new GZIPInputStream( new ByteArrayInputStream( data));
      r = new BufferedReader( new InputStreamReader( s, "8859_1"));
      while ( null != ( line = r.readLine() ) ) {
        if ( line.startsWith( "!") )
          code -= Integer.parseInt( line.substring( 1));
        else if ( line.startsWith( "+") )
          code += Integer.parseInt( line.substring( 1));
        else if ( line.equals( name) )
          return code;
	else
	  ++code;
      }
    }
    catch ( IOException e ) {
      throw new NoUnicodeDBException( e);
    }
    throw new NoSuchCharException( name);
  }
  
  /**
   * Return the name of a given Unicode character.
   * @param c A character code (as an <CODE>int</CODE> to allow for addition
   * of names in the Unicode planes 1 and above).
   * @return The name of the character. If it has more than one name, the
   * canonical Unicode 2.1 name is returned.  C0 controls (which don't have
   * canonical Unicode 2.1 names) have their earlier names returned.
   * @throws IllegalArgumentException If <EM>c</EM> is not between 0 and
   * 0x10ffff inclusive.
   * @throws NoSuchCharException If <EM>c</EM> is an undefined Unicode value.
   * @throws NoUnicodeDBException If data could not be read from the jar.
   */
  public String name( int c) throws NoSuchCharException {
    if ( 0  > c || c >  0x10ffff )
      throw new IllegalArgumentException( String.valueOf( c));
    if ( 0x4e00  <= c && c <=  0x9fff )
      return cjkPrefix + Integer.toHexString( c).toUpperCase();
    if ( SBase   <= c && c <   Send ) {
      int SIndex = c - SBase;
      int L = SIndex / NCount;
      int V = (SIndex % NCount) / TCount;
      int T = SIndex % TCount;
      return hsPrefix + choseong[L] + jungseong[V] + ((T>0)?jongseong[T-1]:"");
    }
    if ( data == null ) {
      try {
      	init();
      }
      catch ( IOException e ) {
      	throw new NoUnicodeDBException( e);
      }
    }
    GZIPInputStream s;
    BufferedReader r;
    int code = 0;
    String line;
    try {
      s = new GZIPInputStream( new ByteArrayInputStream( data));
      r = new BufferedReader( new InputStreamReader( s, "8859_1"));
      while ( null != ( line = r.readLine() ) ) {
        if ( line.startsWith( "!") )
          code -= Integer.parseInt( line.substring( 1));
        else if ( line.startsWith( "+") )
          code += Integer.parseInt( line.substring( 1));
        else if ( code == c )
          return line;
	else
	  ++code;
      }
    }
    catch ( IOException e ) {
      throw new NoUnicodeDBException( e);
    }
    throw new NoSuchCharException( String.valueOf( c));
  }
  
  /**Thrown when name data cannot be read from the jar. Should Never Happen.*/
  public static class NoUnicodeDBException extends Rethrown.RuntimeException {
    /**Construct from the original Throwable indicating the underlying problem*/
    NoUnicodeDBException( Throwable t) { super( t); }
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
