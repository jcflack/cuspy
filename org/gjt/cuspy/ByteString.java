package org.gjt.cuspy;

import java.io.Serializable;
import java.io.IOException;
import java.io.CharConversionException;
import java.io.UnsupportedEncodingException;
// imports below just for benefit of javadoc links
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Mirror of {@link String} with all processing done in the byte domain.
 * Java's adoption of Unicode is both a blessing and a curse. The difference
 * between Unicode strings and their locally-encoded representations is not
 * clearly driven home in the docs.  Character encoding transformations are
 * done whenever characters are read from an {@link InputStreamReader}
 * or written to an {@link OutputStreamWriter}, and they are done automatically
 * by the {@link String} class whenever Strings are constructed from bytes
 * or bytes are extracted from Strings.
 *<P>
 * The encoding transformations are not entirely benign and friendly.  The
 * majority of them map the vast Unicode space into 128 or 256 encoded values,
 * so clearly encodings of Unicode characters as bytes can fail.
 * Failures can also occur in the other direction, as not every local
 * encoding maps all of its cells to defined Unicode characters.  The behavior
 * of Java's character encoding in such cases is to replace the character with
 * a question mark. This corruption of the data takes place silently, with
 * no exception or other indication that it has occurred. The problem is
 * rarely noticed by North Americans and Western Europeans using the Latin-1
 * encoding, because that encoding happens to be an identity map from bytes to
 * the first 256 Unicode values.  Java's 111 other supported character
 * encodings, however, are not.
 *<P>
 * If a Java application is designed for an environment in which all files
 * or strings it will encounter are known to adhere to a defined encoding,
 * there is no problem. But Java runtimes are often installed on operating
 * systems that do not address character encodings at a fundamental level.
 * On such systems, strings that cross the system service boundary, such as
 * file names, system call arguments, and the contents of log files, are
 * interpreted by the system as sequences of bytes, without explicit reference
 * to any character encoding.  While they usually represent character strings
 * to the user, and so a character encoding is implicit, the encoding is
 * applied superficially by the user's terminal, or by the user's choice of
 * font in a terminal emulator. Such a system may be called <EM>provincial</EM>:
 * although filled with assumptions about the correspondence of characters to
 * byte values, it is unaware of its assumptions, which are neither clearly
 * stated nor checked.
 *<P>
 * A Java program may be called <EM>provincial-safe</EM> if it can run on a
 * provincial system without failing or corrupting data if it encounters byte
 * sequences outside the expected encoding. This property is important for a
 * class of applications including administrative tools, log file analyzers,
 * and the like. The defining characteristic of a provincial-safe program can
 * be easily stated. Any program that takes bytes as input must assume a
 * character encoding if it will be interpreting the input as characters.
 * A provincial-safe program makes that assumption weakly, so that its correct
 * operation does not depend on all of the input conforming to the assumption.
 *<P>
 * The trick of writing a provincial-safe program can be illustrated by
 * comparing it to the usual Java Way Of Doing Things.  Imagine a tool that
 * will read input consisting of a list of file names on the local system,
 * and print only those names that end with the string ".java".  Because the
 * processing is in terms of characters, some encoding <EM>e</EM> must be
 * assumed.
 *<P>
 * In the usual Java Way Of Doing Things, the program would read strings or
 * character arrays from an {@link InputStreamReader} constructed with
 * encoding <EM>e</EM>, search them for the string ".java" (A Unicode string),
 * and write the matches to an {@link OutputStreamWriter} also constructed with
 * encoding <EM>e</EM>.  The effect is to transform the sequence of input bytes
 * into Unicode characters according to <EM>e</EM>, perform the substring
 * match in the Unicode domain, and transform the matching strings back to
 * byte sequences via <EM>e</EM>. If any file name contains byte sequences
 * that are not defined by the encoding <EM>e</EM>, they will be replaced by
 * question marks in the transformation process, probably an unacceptable
 * result.
 *<P>
 * A provincial-safe program would also make use of the assumed encoding
 * <EM>e</EM>, but only once: to transform the literal Unicode string ".java"
 * into bytes according to the assumed encoding. The program then reads
 * input bytes directly (using an {@link InputStream}), performs the substring
 * match in the untransformed byte domain, and writes the matches directly
 * using an {@link OutputStream}. Byte sequences in the input will not be
 * altered regardless of whether they are defined under <EM>e</EM>.
 *<P>
 * The two styles of programming must be chosen on careful consideration of
 * the application. The usual Java idiom is familiar, and should be used if
 * the input is sure to be properly encoded Unicode characters. But if an
 * application involves interfacing with a system that operates in the byte
 * domain, the interface does not offer encoding guarantees, and avoiding
 * data corruption is a priority, the provincial-safe idiom should be
 * considered.
 *<P>
 * Writing in the provincial-safe style has been uninviting because by working
 * in the byte domain one gives up the convenient string manipulation methods
 * offered by the {@link String} and {@link StringBuffer} classes.
 * Equivalent operations for the byte domain are now provided by this
 * ByteString class and its {@link Buffer} companion, substituting the word
 * 'byte' for 'char' and vice versa in all method names, and the type 'byte'
 * for 'char' and vice versa in method parameters and return types. There are
 * a few notable differences.
 *<UL>
 *<LI>Deprecated String and StringBuffer methods have not been included.
 *<LI>Many methods are provided in versions that return, or accept as arguments,
 * shorts or arrays of shorts, allowing programs to be written in terms of
 * unsigned values 0..255 instead of (0..127,-128..-1). The choice of short
 * rather than int exploits the fact that the Java compiler will not
 * introduce silent conversions between char and short in either direction,
 * so mistakes where chars are confused with unencoded bytes are less likely
 * to go undetected.  Unfortunately, integer literal arguments will also
 * require casts. To avoid excessive casting and improve readability,
 * literals that will be passed as arguments to these methods can be declared
 * as byte or short finals. (Integer literals, in range, can be
 * <EM>assigned</EM> to byte or short fields with no cast.)
 *<LI>Methods that construct ByteStrings from chars or Strings (analogous to
 * the String methods that construct from bytes), or convert between
 * ByteStrings and chars or Strings, are provided only in the
 * versions with an encoding-name parameter.
 * The versions that leave the encoding implicit have not been included.
 * However, the static final {@link #LOCAL_ENCODING} is initialized to the
 * platform's default encoding (even if permission to read the
 * <CODE>file.encoding</CODE> property is denied) and can be passed as the
 * encoding name.
 *<LI>All such methods verify that the original value is recovered by applying
 * the inverse encoding to the encoding result, and throw
 * {@link CharConversionException} otherwise.  This is a checked exception
 * that will require any code performing such conversions to provide an
 * explicit <CODE>catch</CODE> block or <CODE>throws</CODE> clause, so the
 * writer of provincial-safe code will be keenly aware of where encoding
 * assumptions are being made.
 *<LI>The <CODE>valueOf</CODE> methods for converting sundry Java types
 * have not been included; they can always be converted to Strings and thence
 * to ByteStrings.
 *<LI><CODE>toLowerCase</CODE>, <CODE>toUpperCase</CODE>, and
 * <CODE>trim</CODE> are not provided as they assume a character encoding.
 * If needed, the effects are achieved by converting to String (with an
 * explicitly specified encoding), applying the method, and converting back.
 *</UL>
 *@author <A HREF="mailto:chap@gjt.org">Chapman Flack</A>
 *@version $Id$
 */
public final class ByteString implements Serializable, Comparable {
  /**The name of the local platform's default encoding, even if we lack
   * permission to read the file.encoding property.*/
  public static final String LOCAL_ENCODING = new InputStreamReader(
    new InputStream() { public int read() { return -1; } }).getEncoding();
  /**Construct an empty ByteString.*/
  public ByteString() { image = new byte[0]; }
  /**Mirrors {@link String#String(char[])}.*/
  public ByteString( byte[] bytes) { image = (byte[])bytes.clone(); }
  /**
   * Shorthand for
   * {@link #ByteString(short[],int,int) ByteString}(bytes,0,bytes.length).
   */
  public ByteString( short[] bytes) { this( bytes, 0, bytes.length); }
  /**Private constructor using byte array without copying.*/
  private ByteString( byte[] b, byte[] dummy) { image = b; }
  /**Mirrors {@link String#String(String)}.*/
  public ByteString( ByteString value) { image = value.image; }
  /**Mirrors {@link String#String(char[],int,int)}.*/
  public ByteString( byte[] bytes, int offset, int length) {
    image = new byte[length];
    try {
      System.arraycopy( bytes, offset, image, 0, length);
    }
    catch ( ArrayIndexOutOfBoundsException e ) {
      throw new StringIndexOutOfBoundsException();
    }
  }
  /**
   * Mirrors {@link String#String(char[],int,int)}, but accepts an array of
   * <CODE>short</CODE> to be converted to <CODE>byte</CODE> by casting.
   * The conversion preserves only the low eight bits and may change the sign.
   * This constructor is for convenience in programs that manipulate bytes as
   * unsigned values.
   */
  public ByteString( short[] bytes, int offset, int length) {
    image = new byte[length];
    try {
      for ( int i = 0; i < length; ++ i )
        image [ i ] = (byte) bytes [ offset++ ];
    }
    catch ( ArrayIndexOutOfBoundsException e ) {
      throw new StringIndexOutOfBoundsException();
    }
  }
  /**Mirrors {@link String#String(StringBuffer)}.*/
  public ByteString( Buffer buffer) {
    synchronized ( buffer ) {
      image = new byte[buffer.firstFree];
      System.arraycopy( buffer.buf, 0, image, 0, buffer.firstFree);
    }
  }
  /**Construct a ByteString by converting a String according to the specified
   * encoding.
   *@param str String to convert.
   *@param enc <A HREF
="http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html">
   *Encoding</A> to use.
   *@throws UnsupportedEncodingException The encoding is unknown.
   *@throws CharConversionException The value can't be recoverably encoded.
   */
  public ByteString( String str, String enc)
  throws CharConversionException, UnsupportedEncodingException {
    image = str.getBytes( enc);
    if ( !str.equals( new String( image, enc)) )
      throw new CharConversionException( str);
  }
  /**Mirrors {@link String#String(byte[],String)}.
   *@param chars Array of characters to convert.
   *@param enc <A HREF
="http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html">
   *Encoding</A> to use.
   *@throws UnsupportedEncodingException The encoding is unknown.
   *@throws CharConversionException The value can't be recoverably encoded.
   */
  public ByteString( char[] chars, String enc)
  throws CharConversionException, UnsupportedEncodingException {
    this( new String( chars), enc);
  }
  /**Mirrors {@link String#String(byte[],int,int,String)}.
   *@param chars Array of characters to convert.
   *@param offset Index of first character in chars to convert.
   *@param length Number of characters to convert.
   *@param enc <A HREF
="http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html">
   *Encoding</A> to use.
   *@throws UnsupportedEncodingException The encoding is unknown.
   *@throws CharConversionException The value can't be recoverably encoded.
   */
  public ByteString( char[] chars, int offset, int length, String enc)
  throws CharConversionException, UnsupportedEncodingException {
    this( new String( chars, offset, length), enc);
  }
  
  private /*final*/ byte[] image;
  private static final Interned interned = new Interned();
  /**Mirrors {@link String#equals(Object)}.*/
  public boolean equals( Object anObject) {
    return anObject != null
        && anObject instanceof ByteString
        && 0 == compareTo( anObject);
  }
  /**Mirrors {@link String#hashCode()}.*/
  public int hashCode() {
    int code = 0;
    for ( int i = 0; i < image.length; ++i )
      code = 31*code + (0xff & image[i]);
    return code;
  }
  /**Mirrors {@link String#compareTo(Object)}.*/
  public int compareTo( Object o) {
    ByteString b = (ByteString)o;
    int i = 0;
    int lim = image.length <= b.image.length ? image.length : b.image.length;
    for ( i = 0; i < lim  &&  image[i] == b.image[i]; ++i ); //
    if ( i == lim ) {
      if ( image.length < b.image.length )
      	return -1;
      else if ( image.length > b.image.length )
      	return 1;
      else
      	return 0;
    }
    return ( image[i] < b.image[i] ) ? -1 : 1;
  }
  /**Mirrors {@link String#length()}.*/
  public int length() { return image.length; }
  /**Mirrors {@link String#charAt(int)}.
   *@return an <EM>unsigned</EM> value, 0 to 255
   */
  public short byteAt( int index) {
    try {
      return (short)(image[index] & 0xff);
    }
    catch ( ArrayIndexOutOfBoundsException e ) {
      throw new StringIndexOutOfBoundsException( index);
    }
  }
  /**Mirrors {@link String#getChars(int,int,char[],int)}.*/
  public void getBytes( int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
    try {
      while ( srcBegin < srcEnd )
      	dst[dstBegin++] = image[srcBegin++];
    }
    catch ( ArrayIndexOutOfBoundsException e ) {
      throw new StringIndexOutOfBoundsException();
    }
  }
  /**
   * Mirrors {@link String#getChars(int,int,char[],int)}, but returns the
   * bytes as <EM>unsigned</EM> values 0 to 255 in an array of
   * <CODE>short</CODE>.
   */
  public void getBytes( int srcBegin, int srcEnd, short[] dst, int dstBegin) {
    try {
      while ( srcBegin < srcEnd )
      	dst[dstBegin++] = (short)(image[srcBegin++] & 0xff);
    }
    catch ( ArrayIndexOutOfBoundsException e ) {
      throw new StringIndexOutOfBoundsException();
    }
  }
  /**Mirrors {@link String#toCharArray()}.*/
  public byte[] toByteArray() { return (byte[])image.clone(); }
  /**
   * Mirrors {@link String#toCharArray()}, but returns the
   * bytes as <EM>unsigned</EM> values 0 to 255 in an array of
   * <CODE>short</CODE>.
   */
  public short[] toShortArray() {
    short[] a = new short [ image.length ];
    for ( int i = image.length - 1; i >= 0; -- i )
      a [ i ] = (short)(image [ i ] & 0xff);
    return a;
  }
  /**Mirrors {@link String#getBytes(String)}.
   *@param enc <A HREF
="http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html">
   *Encoding</A> to use.
   *@throws UnsupportedEncodingException The encoding is unknown.
   *@throws CharConversionException The value can't be recoverably encoded.
   */
  public char[] getChars( String enc)
  throws UnsupportedEncodingException, CharConversionException {
    String s = new String( image, enc);
    byte[] b = s.getBytes( enc);
    if ( b.length != image.length )
      throw new CharConversionException( toString());
    for ( int i = 0; i < b.length; ++i )
      if ( b[i] != image[i] )
      	throw new CharConversionException( toString());
    return s.toCharArray();
  }
  /**Mirrors {@link String#regionMatches(int,String,int,int)}.*/
  public boolean regionMatches( int toffset, ByteString other, int ooffset,
    int len) {
    try {
      while ( len > 0 ) {
      	if ( image[toffset++] != other.image[ooffset++] )
	  return false;
	--len;
      }
      return true;
    }
    catch ( ArrayIndexOutOfBoundsException e ) { }
    return false;
  }
  /**Mirrors {@link String#startsWith(String,int)}.*/
  public boolean startsWith( ByteString prefix, int toffset) {
    return regionMatches( toffset, prefix, 0, prefix.image.length);
  }
  /**Mirrors {@link String#startsWith(String)}.*/
  public boolean startsWith( ByteString prefix) {
    return regionMatches( 0, prefix, 0, prefix.image.length);
  }
  /**Mirrors {@link String#endsWith(String)}.*/
  public boolean endsWith( ByteString suffix) {
    return regionMatches( image.length - suffix.image.length, suffix,
      0, suffix.image.length);
  }
  /**Mirrors {@link String#indexOf(int)}.*/
  public int indexOf( short b) {
    byte signed = (byte)b;
    for ( int i = 0; i < image.length; ++i )
      if ( image[i] == signed )
      	return i;
    return -1;
  }
  /**Mirrors {@link String#indexOf(int,int)}.*/
  public int indexOf( short b, int fromIndex) {
    byte signed = (byte)b;
    if ( fromIndex < 0 )
      fromIndex = 0;
    for ( ; fromIndex < image.length; ++fromIndex )
      if ( image[fromIndex] == signed )
      	return fromIndex;
    return -1;
  }
  /**Mirrors {@link String#lastIndexOf(int)}.*/
  public int lastIndexOf( short b) {
    int i;
    byte signed = (byte)b;
    for ( i = image.length - 1; i >= 0  &&  image[i] != signed; --i ); //
    return i;
  }
  /**Mirrors {@link String#lastIndexOf(int,int)}.*/
  public int lastIndexOf( short b, int fromIndex) {
    if ( fromIndex >= image.length )
      fromIndex = image.length - 1;
    else if ( fromIndex < 0 )
      return -1;
    byte signed = (byte)b;
    for ( ; fromIndex >= 0  &&  image[fromIndex] != b; --fromIndex ); //
    return fromIndex;
  }
  /**Mirrors {@link String#indexOf(String,int)}.*/
  public int indexOf( ByteString str, int fromIndex) {
    int lim = image.length - str.image.length;
    if ( fromIndex < 0 )
      fromIndex = 0;
    while ( fromIndex <= lim ) {
      if ( regionMatches( fromIndex, str, 0, str.image.length) )
      	return fromIndex;
      ++fromIndex;
    }
    return -1;
  }
  /**Mirrors {@link String#indexOf(String)}.*/
  public int indexOf( ByteString str) {
    int lim = image.length - str.image.length;
    int fromIndex = 0;
    while ( fromIndex <= lim ) {
      if ( regionMatches( fromIndex, str, 0, str.image.length) )
      	return fromIndex;
      ++fromIndex;
    }
    return -1;
  }
  /**Mirrors {@link String#lastIndexOf(String,int)}.*/
  public int lastIndexOf( ByteString str, int fromIndex) {
    if ( fromIndex > image.length - str.image.length )
      fromIndex = image.length - str.image.length;
    while ( fromIndex >= 0 ) {
      if ( regionMatches( fromIndex, str, 0, str.image.length) )
      	return fromIndex;
      --fromIndex;
    }
    return -1;
  }
  /**Mirrors {@link String#lastIndexOf(String)}.*/
  public int lastIndexOf( ByteString str) {
    int fromIndex = image.length - str.image.length;
    while ( fromIndex >= 0 ) {
      if ( regionMatches( fromIndex, str, 0, str.image.length) )
      	return fromIndex;
      --fromIndex;
    }
    return -1;
  }
  /**Mirrors {@link String#substring(int)}.*/
  public ByteString substring( int beginIndex) {
    return new ByteString( image, beginIndex, image.length - beginIndex);
  }
  /**Mirrors {@link String#substring(int,int)}.*/
  public ByteString substring( int beginIndex, int endIndex) {
    return new ByteString( image, beginIndex, endIndex - beginIndex);
  }
  /**Mirrors {@link String#concat(String)}.*/
  public ByteString concat( ByteString str) {
    if ( str.image.length == 0 )
      return this;
    byte[] b = new byte[image.length + str.image.length];
    getBytes( 0, image.length, b, 0);
    str.getBytes( 0, str.image.length, b, image.length);
    return new ByteString( b, b);
  }
  /**Mirrors {@link String#replace(char,char)}.*/
  public ByteString replace( short oldByte, short newByte) {
    byte[] b = (byte[])image.clone();
    byte ob = (byte)oldByte, nb = (byte)newByte;
    boolean any = false;
    for ( int i = 0 ; i < b.length ; ++i )
      if ( b[i] == ob ) {
        b[i] = nb;
        any = true;
      }
    if ( any )
      return new ByteString( b, b);
    else
      return this;
  }
  /**Mirrors {@link String#intern()}.*/
  public ByteString intern() {
    return (ByteString)interned.intern( this);
  }
  /**
   * Write this ByteString on an OutputStream.
   * OutputStream doesn't have a write() method that accepts
   * a ByteString, so instead of <CODE>stream.write( string)</CODE>,
   * just use <CODE>string.write( stream)</CODE>.
   *@param destination The output stream on which the string contents should
   * be written.
   */
  public void write( OutputStream destination)
  throws IOException {
    destination.write( image);
  }

/**
 * Mirror of {@link StringBuffer} that does all processing in the byte domain.
 * For rationale, see {@link ByteString}.
 */
  public static final class Buffer implements java.io.Serializable {
    private byte[] buf;
    private int firstFree;
    /**Construct a ByteString.Buffer with no bytes in it and an initial
     * capacity of 16 bytes.
     */
    public Buffer() { this( 16); }
    /**Mirrors {@link StringBuffer#StringBuffer(int)}.*/
    public Buffer( int length) {
      buf = new byte[length];
      firstFree = 0;
    }
    /**Mirrors {@link StringBuffer#StringBuffer(String)}.*/
    public Buffer( ByteString str) {
      buf = new byte[16 + str.image.length];
      str.getBytes( 0, str.image.length, buf, 0);
      firstFree = str.image.length;
    }
    
    /**Mirrors {@link StringBuffer#length()}.*/
    public int length() { return firstFree; }
    /**Mirrors {@link StringBuffer#capacity()}.*/
    public int capacity() { return buf.length; }
    /**Mirrors {@link StringBuffer#ensureCapacity(int)}.*/
    public synchronized void ensureCapacity( int minimumCapacity) {
      if ( buf.length >= minimumCapacity )
      	return;
      int twice = (buf.length + 1) << 1;
      byte[] newBuf = new byte[(twice>minimumCapacity)?twice:minimumCapacity];
      System.arraycopy( buf, 0, newBuf, 0, firstFree);
      buf = newBuf;
    }
    /**Mirrors {@link StringBuffer#setLength(int)}.*/
    public synchronized void setLength( int newLength) {
      if ( newLength < 0 )
      	throw new StringIndexOutOfBoundsException();
      if ( buf.length < newLength )
      	ensureCapacity( newLength);
      while ( firstFree < newLength ) {
	buf[firstFree] = 0;
	++firstFree;
      }
      firstFree = newLength;
    }
    /**
     * Mirrors {@link StringBuffer#charAt(int)}.
     *@return an unsigned value, 0 to 255
     */
    public synchronized short byteAt( int index) {
      if ( firstFree <= index  ||  index < 0 )
      	throw new StringIndexOutOfBoundsException( index);
      return (short)(buf[index] & 0xff);
    }
    /**Mirrors {@link StringBuffer#getChars(int,int,char[],int)}.*/
    public synchronized void getBytes( int srcBegin, int srcEnd,
      byte[] dst, int dstBegin) {
      if ( firstFree < srcEnd  ||  srcEnd < srcBegin )
        throw new StringIndexOutOfBoundsException();
      try {
      	System.arraycopy( buf, srcBegin, dst, dstBegin, srcEnd - srcBegin);
      }
      catch ( IndexOutOfBoundsException e ) {
      	throw new StringIndexOutOfBoundsException();
      }
    }
    /**
     * Mirrors {@link StringBuffer#getChars(int,int,char[],int)}, but returns
     * unsigned values 0 to 255 in an array of <CODE>short</CODE>.
     */
    public synchronized void getBytes( int srcBegin, int srcEnd,
      short[] dst, int dstBegin) {
      if ( firstFree < srcEnd  ||  srcEnd < srcBegin )
        throw new StringIndexOutOfBoundsException();
      try {
      	for ( int i = srcBegin; i < srcEnd; ++i )
          dst [ dstBegin++ ] = (short)(buf [ i ] & 0xff);
      }
      catch ( IndexOutOfBoundsException e ) {
      	throw new StringIndexOutOfBoundsException();
      }
    }
    /**Mirrors {@link StringBuffer#setCharAt(int,char)}.*/
    public synchronized void setByteAt( int index, short b) {
      if ( index >= firstFree  ||  index < 0 )
      	throw new StringIndexOutOfBoundsException( index);
      buf[index] = (byte)b;
    }
    /**Mirrors {@link StringBuffer#append(char)}.*/
    public synchronized Buffer append( short b) {
      ensureCapacity( firstFree + 1);
      buf [ firstFree++ ] = (byte)b;
      return this;
    }
    /**Mirrors {@link StringBuffer#append(String)}.*/
    public synchronized Buffer append( ByteString str) {
      ensureCapacity( firstFree + str.image.length);
      System.arraycopy( str.image, 0, buf, firstFree, str.image.length);
      firstFree += str.image.length;
      return this;
    }
    /**Mirrors {@link StringBuffer#append(char[])}.*/
    public synchronized Buffer append( byte[] str) {
      ensureCapacity( firstFree + str.length);
      System.arraycopy( str, 0, buf, firstFree, str.length);
      firstFree += str.length;
      return this;
    }
    /**Shorthand for {@link #append(short[],int,int) append}(str,0,str.length).
     */
    public synchronized Buffer append( short[] str) {
      return append( str, 0, str.length);
    }
    /**Mirrors {@link StringBuffer#append(char[],int,int)}.*/
    public synchronized Buffer append( byte[] str, int offset, int len) {
      ensureCapacity( firstFree + len);
      System.arraycopy( str, offset, buf, firstFree, len);
      firstFree += len;
      return this;
    }
    /**
     * Mirrors {@link StringBuffer#append(char[],int,int)}, but accepts an
     * array of <CODE>short</CODE> to be converted to <CODE>byte</CODE> by
     * casting. The conversion preserves only the low eight bits and may change
     * the sign. This method is for convenience in programs that manipulate
     * unsigned bytes.
     */
    public synchronized Buffer append( short[] str, int offset, int len) {
      ensureCapacity( firstFree + len);
      while ( len-- > 0 )
        buf [ firstFree++ ] = (byte) str [ offset++ ];
      return this;
    }
    /**Mirrors {@link StringBuffer#delete(int,int)}.*/
    public synchronized Buffer delete( int start, int end) {
      if ( end > firstFree )
      	end = firstFree;
      if ( end < start || start < 0 )
      	throw new StringIndexOutOfBoundsException( start);
      if ( start < end ) {
      	System.arraycopy( buf, end, buf, start, firstFree - end);
	firstFree -= end - start;
      }
      return this;
    }
    /**Mirrors {@link StringBuffer#deleteCharAt(int)}.*/
    public synchronized Buffer deleteByteAt( int index) {
      if ( firstFree <= index || index < 0 )
      	throw new StringIndexOutOfBoundsException( index);
      --firstFree;
      if ( index < firstFree )
      	System.arraycopy( buf, index+1, buf, index, firstFree - index);
      return this;
    }
    /**Mirrors {@link StringBuffer#replace(int,int,String)}.*/
    public synchronized Buffer replace( int start, int end, ByteString str) {
      if ( end > firstFree )
      	end = firstFree;
      if ( end < start || start < 0 )
      	throw new StringIndexOutOfBoundsException( start);
      int delta = str.image.length - end + start;
      if ( delta > 0 )
      	ensureCapacity( firstFree + delta);
      System.arraycopy( buf, end, buf, end+delta, firstFree - end);
      System.arraycopy( str.image, 0, buf, start, str.image.length);
      return this;
    }
    /**Mirrors {@link StringBuffer#substring(int)}.*/
    public synchronized ByteString substring( int start) {
      return new ByteString( buf, start, firstFree - start);
    }
    /**Mirrors {@link StringBuffer#substring(int,int)}.*/
    public synchronized ByteString substring( int start, int end) {
      return new ByteString( buf, start, end - start);
    }
    /**Mirrors {@link StringBuffer#insert(int,char[],int,int)}.*/
    public synchronized Buffer
      insert( int index, byte[] str, int offset, int len) {
      if ( firstFree < index || index < 0 || offset < 0 || len < 0
      	|| (offset + len) > str.length )
	throw new StringIndexOutOfBoundsException();
      ensureCapacity( firstFree + len);
      System.arraycopy( buf, index, buf, index + len, firstFree - index);
      System.arraycopy( str, offset, buf, index, len);
      firstFree += len;
      return this;
    }
    /**
     * Mirrors {@link StringBuffer#insert(int,char[],int,int)}, but accepts an
     * array of <CODE>short</CODE> to be converted to <CODE>byte</CODE> by
     * casting. The conversion preserves only the low eight bits and may change
     * the sign. This method is for convenience in programs that manipulate
     * unsigned bytes.
     */
    public synchronized Buffer
      insert( int index, short[] str, int offset, int len) {
      if ( firstFree < index || index < 0 || offset < 0 || len < 0
      	|| (offset + len) > str.length )
	throw new StringIndexOutOfBoundsException();
      ensureCapacity( firstFree + len);
      System.arraycopy( buf, index, buf, index + len, firstFree - index);
      firstFree += len;
      while ( len-- > 0 )
        buf [ index++ ] = (byte) str [ offset++ ];
      return this;
    }
    /**Mirrors {@link StringBuffer#insert(int,String)}.*/
    public synchronized Buffer insert( int offset, ByteString str) {
      if ( firstFree < offset  ||  offset < 0 )
      	throw new StringIndexOutOfBoundsException( offset);
      ensureCapacity( firstFree + str.image.length);
      System.arraycopy( buf, offset, buf, offset + str.image.length,
      	      	      	firstFree - offset);
      System.arraycopy( str.image, 0, buf, offset, str.image.length);
      firstFree += str.image.length;
      return this;
    }
    /**Mirrors {@link StringBuffer#insert(int,char[])}.*/
    public synchronized Buffer insert( int offset, byte[] str) {
      if ( firstFree < offset  ||  offset < 0 )
      	throw new StringIndexOutOfBoundsException( offset);
      ensureCapacity( firstFree + str.length);
      System.arraycopy( buf, offset, buf, offset + str.length,
      	      	      	firstFree - offset);
      System.arraycopy( str, 0, buf, offset, str.length);
      firstFree += str.length;
      return this;
    }
    /**
     * Shorthand for
     * {@link #insert(int,short[],int,int) insert}(offset,str,0,str.length).
     */
    public synchronized Buffer insert( int offset, short[] str) {
      return insert( offset, str, 0, str.length);
    }
    /**Mirrors {@link StringBuffer#insert(int,char)}.*/
    public synchronized Buffer insert( int offset, short b) {
      if ( firstFree < offset  ||  offset < 0 )
      	throw new StringIndexOutOfBoundsException( offset);
      ensureCapacity( firstFree + 1);
      System.arraycopy( buf, offset, buf, offset + 1, firstFree - offset);
      buf[offset] = (byte)b;
      return this;
    }
    /**Mirrors {@link StringBuffer#reverse()}.*/
    public synchronized Buffer reverse() {
      int a = 0;
      int b = firstFree - 1;
      byte t;
      while ( a < b ) {
      	t = buf[a];
	buf[a++] = buf[b];
	buf[b--] = t;
      }
      return this;
    }
    /**
     * Write this ByteString.Buffer on an OutputStream.
     * OutputStream doesn't have a write() method that accepts
     * a ByteString.Buffer, so instead of <CODE>stream.write( buf)</CODE>,
     * just use <CODE>buf.write( stream)</CODE>.
     *@param destination The output stream on which the buffer contents should
     * be written.
     */
    public synchronized void write( OutputStream destination)
    throws IOException {
      destination.write( buf, 0, firstFree);
    }
    /**Returns, as a String, a hexadecimal representation of the contents.
     */
    public synchronized String toString() {
      return ByteString.hex( buf, firstFree);
    }
  }
  
  /**Returns, as a String, a hexadecimal representation of the contents.
   */
  public String toString() {
    return hex( image, image.length);
  }
  private static String hex( byte[] b, int len) {
    StringBuffer sb = new StringBuffer( 3 * len);
    char[] buf = { ' ', ' ', ' ' };
    for ( int i = 0; i < len; ++i ) {
      buf[0] = Character.forDigit( (b[i]>>4)&0xf, 16);
      buf[1] = Character.forDigit( b[i]&0xf, 16);
      sb.append( buf);
    }
    if ( len >  0 )
      sb.setLength( 3 * len - 1);
    return new String( sb);
  }
}
