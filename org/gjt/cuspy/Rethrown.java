package org.gjt.cuspy;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * A base for defining exceptions that can encapsulate prior exceptions.
 * Consider the common situation where a low-level exception is translated
 * into one specified by a higher-level interface.  For example, the public
 * interface for <CODE>getDoojigger()</CODE>:
 *<PRE>
 *
 *   public Doojigger getDoojigger(String name) throws NoSuchDoojiggerException;
 *
 *</PRE>
 * The <EM>implementation</EM> of <CODE>getDoojigger()</CODE> may contain
 * something like this:
 *<PRE>
 *
 *   try {
 *     in = new FileInputStream( djNameToPathName( name));
 *     dj = loadDoojigger( in);
 *   }
 *   catch ( IOException e ) {
 *      throw new NoSuchDoojiggerException( "Can't load doojigger "+name);
 *   }
 *
 *</PRE>
 * This design protects the larger application from having to know about the
 * particular mechanism needed to load doojiggers, or the sorts of specific
 * exceptions they might throw.  A later version of getDoojigger might load
 * doojiggers over a network connection, and the sorts of exceptions it would
 * encounter would be different from the IOExceptions relating to files, but
 * the caller only needs to catch the documented NoSuchDoojiggerException.
 * <P>
 * The <EM>problem</EM> is, when a doojigger isn't found, the high-level
 * NoSuchDoojiggerException does not tell the user very much about what went
 * wrong.  Was the file not found?  Where should it have been?  Notice the
 * exception shows the high-level doojigger name, not the path name that
 * wasn't found.  The path name might reveal a simple problem; perhaps the
 * DJPATH is set wrong.  Or was the file unreadable?  Perhaps there is a flaw
 * in loadDoojigger.  What line of code caused the original exception?  The
 * stack trace of the NoSuchDoojiggerException only goes back to the
 * <CODE>throw</CODE> statement in <CODE>getDoojigger()</CODE>.  The original
 * exception may have been thrown by a method umpteen stack frames down from
 * there.
 * <P>
 * A solution is to define your application's hierarchy of exceptions, including
 * NoSuchDoojiggerException, so the topmost extends
 * {@link Rethrown.Exception}.  Change the <CODE>throw</CODE> statement to read:
 *<PRE>
 *
 *     throw new NoSuchDoojiggerException( e, "Can't load doojigger "+name);
 *
 *</PRE>
 * How does this affect the calling program? Very little! The exceptions have
 * a few new methods, like {@link #get(int) get} for retrieving the original
 * exception and new versions of {@link #getMessage(int) getMessage} and
 * {@link #printStackTrace(PrintWriter,int) printStackTrace} with a parameter
 * to control the amount of detail desired. But the program doesn't need to
 * know or care about these.
 * <P>
 * The program will catch NoSuchDoojiggerExceptions the same as always.
 * When it calls {@link #toString()} or {@link #getMessage()}, or if the
 * exception goes uncaught and the Java runtime calls
 * {@link Throwable#printStackTrace() printStackTrace()},
 * everything will look the same.
 * <P>
 * <STRONG>Unless</STRONG>...the user runs the application with
 *<PRE>
 *   -Dorg.gjt.cuspy.Rethrown.displayDepth=n
 *</PRE>
 * on the Java command line, where <EM>n</EM> is some integer greater than zero.
 * In that case, exception strings and stack traces will reflect the top
 * exception itself and up to <EM>n</EM> prior exceptions that led to it.
 * By setting org.gjt.cuspy.Rethrown.displayDepth to a huge integer, the user
 * can get full information about what went wrong, and the displayDepth can be
 * adjusted without modifying any code (or even having the code).
 * <P>
 * Another application for rethrown exceptions crops up when you need to get
 * exceptions through a layer of code that doesn't know about them.
 * For example, you are presented with the following API:
 *<PRE>
 *
 *   public void crunchNugget( Nugget n);
 *
 *</PRE>
 * and you will be passing it your own subclass of Nugget.  It just happens
 * that your Nugget subclass implementation does some I/O and it might
 * encounter I/O exceptions, and the rest of your program needs to know
 * about them.  But the API for crunchNugget doesn't include a
 * <CODE>throws</CODE> clause for <CODE>IOException</CODE>, and you lack
 * either access to the code, or the permission or ambition to change it.
 * <P>
 * Instead, define a new exception <CODE>NuggetIOException</CODE> that
 * extends {@link Rethrown.RuntimeException}.  Within your nugget, wrap
 * the operations that could throw IOException in a <CODE>try</CODE> block:
 *<PRE>
 *
 *   try { ... } catch ( IOException e ) { throw new NuggetIOException(e); }
 *
 *</PRE>
 * Then wrap the call to crunchNugget:
 *<PRE>
 *
 *   try {
 *     crunchNugget( myNug);
 *   } catch ( NuggetIOException e ) { throw (IOException)e.get( 1); }
 *
 *</PRE>
 * and the original IOException, in all its detail, is back in flight to your
 * existing IOException handler. This works because exceptions that extend
 * RuntimeException don't need to be declared in <CODE>throws</CODE> clauses.
 * Just be sure your outer <CODE>try</CODE> block catches <EM>only</EM> the
 * specific subclass of <CODE>Rethrown.RuntimeException</CODE> you defined
 * for this purpose, <CODE>NuggetIOException</CODE> in this case. If you
 * broadly catch <CODE>RuntimeException</CODE> or
 * <CODE>Rethrown.RuntimeException</CODE> you will interfere with other
 * classes' ability to use the same technique to tunnel their exceptions
 * through your code.
 * To encourage good practice, {@link Rethrown.Exception},
 * {@link Rethrown.RuntimeException}, and
 * {@link Rethrown.Error} are all declared
 * <CODE>abstract</CODE> so you have to subclass
 * them to use them.
 *@author <A HREF="mailto:chap@gjt.org">Chapman Flack</A>
 *@version $Id$
 */
public interface Rethrown {
/**The name of the depth property.
 */
  static final String depthProp = "org.gjt.cuspy.Rethrown.displayDepth";
/**See {@link Throwable#fillInStackTrace()}.
 */
  Throwable fillInStackTrace();
/**Get the <EM>n</EM>th prior exception.
 *@param n 0 refers to this object itself, 1 to the exception immediately
 * wrapped in it. get( Integer.MAX_VALUE) should return the original exception
 * that started it all, even after a ridiculous number of rethrows.
 *@return The indexed exception, or the earliest one if &lt; <EM>n</EM>.
 */
  Throwable get( int n);
/**See {@link Throwable#getLocalizedMessage()}.
 */
  String getLocalizedMessage();
/**Get the localized message with detail to depth <EM>n</EM>.
 * Like {@link Throwable#getLocalizedMessage}, if this method is not overridden
 * by a subclass to do actual localization, it is equivalent to
 * {@link #getMessage(int)}.
 *@param n How many prior exception messages to include in the returned string.
 *@return The message string.
 */
  String getLocalizedMessage( int n);
/**Overrides {@link Throwable#getMessage()}.
 * Equivalent to {@link #getMessage(int)} passing the value of the
 * <CODE>org.gjt.cuspy.Rethrown.displayDepth</CODE> property (0 by default).
 */
  String getMessage();
/**Get the exception message with detail to depth <EM>n</EM>.
 *@param n How many prior exception messages to include in the returned string.
 *@return The message string.
 */
  String getMessage( int n);
/**Overrides {@link Throwable#printStackTrace(PrintWriter)}.
 * Equivalent to {@link #printStackTrace(PrintWriter,int)} passing the value
 * of the <CODE>org.gjt.cuspy.Rethrown.displayDepth</CODE> property
 * (0 by default).
 * The implementing classes also override
 * {@link Throwable#printStackTrace(PrintStream)} and
 * {@link Throwable#printStackTrace()}, but they are not advertised in this
 * interface because {@link java.io.PrintStream PrintStream} is deprecated.
 *@param s The stack trace is written to this <CODE>PrintWriter</CODE>.
 */
  void printStackTrace( PrintWriter s);
/**Print the stack trace with detail back to the <EM>n</EM>th prior exception.
 *@param s The stack trace is written to this <CODE>PrintWriter</CODE>.
 *@param n 0 gets the stack trace only back to the <CODE>throw</CODE> that
 * threw this exception itself, 1 gets back to the <CODE>throw</CODE> of
 * the immediately contained exception.
 * <CODE>printStackTrace( s, Integer.MAX_VALUE)</CODE> prints the mosr detailed
 * trace possible.
 */
  void printStackTrace( PrintWriter s, int n);
/**Only here because needed for overriding
 * {@link Throwable#printStackTrace(PrintStream)}.
 *@deprecated Use {@link #printStackTrace(PrintWriter,int)}.
 */
  void printStackTrace( PrintStream s, int n);
/**Overrides {@link Throwable#toString()}.
 * Equivalent to {@link #toString(int)} passing the value of the
 * <CODE>org.gjt.cuspy.Rethrown.displayDepth</CODE> property (0 by default).
 */
  String toString();
/**Get a short string description of this exception and up to <EM>n</EM>
 * prior exceptions.
 *@param n 0 gets the description only of this exception itself, 1 the
 * immediately prior contained exception.
 * <CODE>toString( Integer.MAX_VALUE)</CODE> gets the most detailed string
 * possible.
 */
  String toString( int n);

/**Base class for a checked exception that implements {@link Rethrown}.
 * Extend this to create an exception that must be specified in
 * <CODE>throws</CODE> clauses whereever it may be thrown.
 */
  public abstract class Exception
    extends java.lang.Exception
    implements Rethrown {
    
/**The encapsulated Throwable object.
 */
    protected final Throwable last;
/**Not of interest unless a subclass does hairy reformatting.
 * Set <CODE>true</CODE> in a <CODE>try/finally</CODE> block to ensure
 * it gets set back to false, causes the no-<EM>n</EM> versions of
 * {@link Rethrown#getMessage() getMessage},
 * {@link Rethrown#getLocalizedMessage() getLocalizedMessage},
 * {@link Rethrown#printStackTrace(PrintWriter) Rethrown.printStackTrace},
 * and {@link Rethrown#toString() toString} to revert to their non-overridden
 * {@link Throwable} behavior for the duration of the block.
 */
    protected transient boolean immediate = false;
/**Display depth to use for this exception (max of defaultDepth and depth
 * passed to constructor).
 */
    protected final int displayDepth;
/**Cached value of the <CODE>Rethrown.displayDepth</CODE> property.
 */
    protected static final int defaultDepth;
/**The string used to separate information on the current exception from
 * information on the prior exception that caused it.
 * Initialized to <CODE>line.separator</CODE> + "upon ". 
 */
    protected static String separator;
    static {
      int javacPacifier = 0;
      String sep = "\r\n";
      try { javacPacifier = Integer.getInteger( depthProp, 0).intValue(); }
      catch ( SecurityException e ) { }
      finally { defaultDepth = javacPacifier; }
      try { sep = System.getProperty( "line.separator"); }
      catch ( SecurityException e ) { }
      finally { separator = sep + "upon "; }
    }
    
/**Constructs a Rethrown.Exception with no prior exception,
 * <CODE>null</CODE> message string, and default display depth.
 */
    public Exception() { this( (Throwable)null, null, 0); }
/**Constructs a Rethrown.Exception with no prior exception and a message,
 * and default display depth.
 *@param message Descriptive string saved for later retrieval.
 */
    public Exception( String message) { this( (Throwable)null, message, 0); }
/**Constructs a Rethrown.Exception from prior exception <EM>t</EM> and
 * a <CODE>null</CODE> message string, with default display depth.
 *@param t Prior exception whose details will be shown by the new object
 * for display depth 1 or more.
 */
    public Exception( Throwable t) { this( t, null, 0); }
/**Constructs a Rethrown.Exception from prior exception <EM>t</EM> and
 * a message.
 *@param t Prior exception whose details will be shown by the new object
 * for display depth 1 or more.
 *@param message Descriptive string saved for later retrieval.
 */
    public Exception( Throwable t, String message) { this( t, message, 0); }
/**Constructs a Rethrown.Exception from prior exception <EM>t</EM>, a
 * <CODE>null</CODE> message string, and a display depth.
 *@param t Prior exception whose details will be shown by the new object for
 * display depth 1 or more.
 *@depth How many prior exceptions to include in detail output.
 * The depth used is the max of this parameter and the default depth from the
 * Rethrown.displayDepth property.
 */
    public Exception( Throwable t, int depth) { this( t, null, depth); }
/**Constructs a Rethrown.Exception from prior exception <EM>t</EM>, a message,
 * and a display depth.
 *@param t Prior exception whose details will be shown by the new object for
 * display depth 1 or more.
 *@param message Descriptive string saved for later retrieval.
 *@depth How many prior exceptions to include in detail output.
 * The depth used is the max of this parameter and the default depth from the
 * Rethrown.displayDepth property.
 */
    public Exception( Throwable t, String message, int depth) {
      super( message);
      last = t;
      displayDepth = Math.max( depth, defaultDepth);
    }
    
/**See {@link Rethrown#get(int)}.
 */
    public Throwable get( int n) {
      return n < 1
           ? this
      	   : last instanceof Rethrown
	   ? ((Rethrown)last).get( n-1)
	   : last;
    }
/**See {@link Rethrown#getLocalizedMessage(int)}.
 */
    public String getLocalizedMessage( int n) { return getMessage( n); }
/**See {@link Rethrown#getMessage}.
 */
    public String getMessage() {
      synchronized ( this ) {
      	if ( immediate )
	  return super.getMessage();
      }
      return getMessage( displayDepth);
    }
/**See {@link Rethrown#getMessage(int)}.
 */
    public String getMessage( int n) {
      String m;
      synchronized ( this ) {
        boolean old = immediate;
        try {
          immediate = true;
          m = super.getMessage();
        }
        finally {
          immediate = old;
        }
      }
      if ( n < 1  ||  last == null )
	return m;
      m += separator;
      return last instanceof Rethrown
	   ? m + ((Rethrown)last).getMessage( n-1)
	   : m + last.getMessage();
    }
/**@deprecated See {@link Rethrown#printStackTrace(PrintWriter)}.
 */
    public void printStackTrace() {
      synchronized ( this ) {
 	if ( immediate ) {
 	  super.printStackTrace();
	  return;
	}
      }
      printStackTrace( System.err, displayDepth);
    }
/**@deprecated See {@link Rethrown#printStackTrace(PrintWriter)}.
 */
    public void printStackTrace( PrintStream s) {
      synchronized ( this ) {
      	if ( immediate ) {
      	  super.printStackTrace( s);
	  return;
	}
      }
      printStackTrace( s, displayDepth);
    }
/**See {@link Rethrown#printStackTrace(PrintWriter)}.
 */
    public void printStackTrace( PrintWriter s) {
      synchronized ( this ) {
      	if ( immediate ) {
      	  super.printStackTrace( s);
	  return;
	}
      }
      printStackTrace( s, displayDepth);
    }
/**See {@link Rethrown#printStackTrace(PrintWriter,int)}.
 */
    public void printStackTrace( PrintWriter s, int n) {
      if ( n < 1  ||  last == null ) {
      	synchronized ( this ) {
	  boolean old = immediate;
	  try {
	    immediate = true;
	    super.printStackTrace( s);
	  }
	  finally {
	    immediate = old;
	  }
	}
      }
      else {
      	s.print( toString( 0));
	s.print( separator);
 	if ( last instanceof Rethrown )
 	  ((Rethrown)last).printStackTrace( s, n-1);
 	else
 	  last.printStackTrace( s);
      }
    }
/**@deprecated See {@link Rethrown#printStackTrace(PrintWriter,int)}.
 */
    public void printStackTrace( PrintStream s, int n) {
      if ( n < 1  ||  last == null ) {
      	synchronized ( this ) {
	  boolean old = immediate;
	  try {
	    immediate = true;
	    super.printStackTrace( s);
	  }
	  finally {
	    immediate = old;
	  }
	}
      }
      else {
      	s.print( toString( 0));
	s.print( separator);
 	if ( last instanceof Rethrown )
 	  ((Rethrown)last).printStackTrace( s, n-1);
 	else
 	  last.printStackTrace( s);
      }
    }
/**See {@link Rethrown#toString()}.
 */
    public String toString() {
      synchronized ( this ) {
      	if ( immediate )
      	  return super.toString();
      }
      return toString( displayDepth);
    }
/**See {@link Rethrown#toString(int)}.
 */
    public String toString( int n) {
      String m;
      synchronized ( this ) {
      	boolean old = immediate;
	try {
	  immediate = true;
	  m = super.toString();
	}
	finally {
	  immediate = old;
	}
      }
      if ( n < 1  ||  last == null )
      	return m;
      m += separator;
      return last instanceof Rethrown
           ? m + ((Rethrown)last).toString( n-1)
	   : m + last.toString();
    }
  }

/**Base class for an unchecked exception that implements {@link Rethrown}.
 * Extend this to create an exception that does not need to be declared
 * in <CODE>throws</CODE> clauses. Extend this in preference to
 * {@link Rethrown.Error} if the exceptions involved, while unusual, are
 * things a user or administrator could reasonably be expected to fix
 * (e.g. by changing a path, fixing a network connection, obtaining a needed
 * file). Extend {@link Rethrown.Error} instead for conditions that should
 * never occur unless the sources or the Java runtime contain flaws.
 */
  public abstract class RuntimeException
    extends java.lang.RuntimeException
    implements Rethrown {
    
/**The encapsulated Throwable object.
 */
    protected final Throwable last;
/**Not of interest unless a subclass does hairy reformatting.
 * Set <CODE>true</CODE> in a <CODE>try/finally</CODE> block to ensure
 * it gets set back to false, causes the no-<EM>n</EM> versions of
 * {@link Rethrown#getMessage() getMessage},
 * {@link Rethrown#getLocalizedMessage() getLocalizedMessage},
 * {@link Rethrown#printStackTrace(PrintWriter) Rethrown.printStackTrace},
 * and {@link Rethrown#toString() toString} to revert to their non-overridden
 * {@link Throwable} behavior for the duration of the block.
 */
    protected transient boolean immediate = false;
/**Display depth to use for this exception (max of defaultDepth and depth
 * passed to constructor).
 */
    protected final int displayDepth;
/**Cached value of the <CODE>Rethrown.displayDepth</CODE> property.
 */
    protected static final int defaultDepth;
/**The string used to separate information on the current exception from
 * information on the prior exception that caused it.
 * Initialized to <CODE>line.separator</CODE> + "upon ". 
 */
    protected static String separator;
    static {
      int javacPacifier = 0;
      String sep = "\r\n";
      try { javacPacifier = Integer.getInteger( depthProp, 0).intValue(); }
      catch ( SecurityException e ) { }
      finally { defaultDepth = javacPacifier; }
      try { sep = System.getProperty( "line.separator"); }
      catch ( SecurityException e ) { }
      finally { separator = sep + "upon "; }
    }
    
/**Constructs a Rethrown.RuntimeException with no prior exception
 * and <CODE>null</CODE> message string.
 */
    public RuntimeException() { this( (Throwable)null, null, 0); }
/**Constructs a Rethrown.RuntimeException with no prior exception and a message,
 * and default display depth.
 *@param message Descriptive string saved for later retrieval.
 */
    public RuntimeException( String message) {
      this( (Throwable)null, message, 0);
    }
/**Constructs a Rethrown.RuntimeException from prior exception <EM>t</EM> and
 * a <CODE>null</CODE> message string, with default display depth.
 *@param t Prior exception whose details will be shown by the new object
 * for display depth 1 or more.
 */
    public RuntimeException( Throwable t) { this( t, null, 0); }
/**Constructs a Rethrown.RuntimeException from prior exception <EM>t</EM> and
 * a message.
 *@param t Prior exception whose details will be shown by the new object
 * for display depth 1 or more.
 *@param message Descriptive string saved for later retrieval.
 */
    public RuntimeException( Throwable t, String message) {
      this( t, message, 0);
    }
/**Constructs a Rethrown.RuntimeException from prior exception <EM>t</EM>, a
 * <CODE>null</CODE> message string, and a display depth.
 *@param t Prior exception whose details will be shown by the new object for
 * display depth 1 or more.
 *@depth How many prior exceptions to include in detail output.
 * The depth used is the max of this parameter and the default depth from the
 * Rethrown.displayDepth property.
 */
    public RuntimeException( Throwable t, int depth) { this( t, null, depth); }
/**Constructs a Rethrown.RuntimeException from prior exception <EM>t</EM>,
 * a message, and a display depth.
 *@param t Prior exception whose details will be shown by the new object for
 * display depth 1 or more.
 *@param message Descriptive string saved for later retrieval.
 *@depth How many prior exceptions to include in detail output.
 * The depth used is the max of this parameter and the default depth from the
 * Rethrown.displayDepth property.
 */
    public RuntimeException( Throwable t, String message, int depth) {
      super( message);
      last = t;
      displayDepth = Math.max( depth, defaultDepth);
    }
    
/**See {@link Rethrown#get(int)}.
 */
    public Throwable get( int n) {
      return n < 1
           ? this
      	   : last instanceof Rethrown
	   ? ((Rethrown)last).get( n-1)
	   : last;
    }
/**See {@link Rethrown#getLocalizedMessage(int)}.
 */
    public String getLocalizedMessage( int n) { return getMessage( n); }
/**See {@link Rethrown#getMessage}.
 */
    public String getMessage() {
      synchronized ( this ) {
      	if ( immediate )
      	  return super.getMessage();
      }
      return getMessage( displayDepth);
    }
/**See {@link Rethrown#getMessage(int)}.
 */
    public String getMessage( int n) {
      String m;
      synchronized ( this ) {
        boolean old = immediate;
        try {
          immediate = true;
          m = super.getMessage();
        }
        finally {
          immediate = old;
        }
      }
      if ( n < 1  ||  last == null )
	return m;
      m += separator;
      return last instanceof Rethrown
	   ? m + ((Rethrown)last).getMessage( n-1)
	   : m + last.getMessage();
    }
/**@deprecated See {@link Rethrown#printStackTrace(PrintWriter)}.
 */
    public void printStackTrace() {
      synchronized ( this ) {
      	if ( immediate ) {
      	  super.printStackTrace();
	  return;
	}
      }
      printStackTrace( System.err, displayDepth);
    }
/**@deprecated See {@link Rethrown#printStackTrace(PrintWriter)}.
 */
    public void printStackTrace( PrintStream s) {
      synchronized ( this ) {
      	if ( immediate ) {
      	  super.printStackTrace( s);
	  return;
	}
      }
      printStackTrace( s, displayDepth);
    }
/**See {@link Rethrown#printStackTrace(PrintWriter)}.
 */
    public void printStackTrace( PrintWriter s) {
      synchronized ( this ) {
      	if ( immediate ) {
      	  super.printStackTrace( s);
	  return;
	}
      }
      printStackTrace( s, displayDepth);
    }
/**See {@link Rethrown#printStackTrace(PrintWriter,int)}.
 */
    public void printStackTrace( PrintWriter s, int n) {
      if ( n < 1  ||  last == null ) {
      	synchronized ( this ) {
	  boolean old = immediate;
	  try {
	    immediate = true;
	    super.printStackTrace( s);
	  }
	  finally {
	    immediate = old;
	  }
	}
      }
      else {
      	s.print( toString( 0));
	s.print( separator);
 	if ( last instanceof Rethrown )
 	  ((Rethrown)last).printStackTrace( s, n-1);
 	else
 	  last.printStackTrace( s);
      }
    }
/**@deprecated See {@link Rethrown#printStackTrace(PrintWriter,int)}.
 */
    public void printStackTrace( PrintStream s, int n) {
      if ( n < 1  ||  last == null ) {
      	synchronized ( this ) {
	  boolean old = immediate;
	  try {
	    immediate = true;
	    super.printStackTrace( s);
	  }
	  finally {
	    immediate = old;
	  }
	}
      }
      else {
      	s.print( toString( 0));
	s.print( separator);
 	if ( last instanceof Rethrown )
 	  ((Rethrown)last).printStackTrace( s, n-1);
 	else
 	  last.printStackTrace( s);
      }
    }
/**See {@link Rethrown#toString()}.
 */
    public String toString() {
      synchronized ( this ) {
      	if ( immediate )
      	  return super.toString();
      }
      return toString( displayDepth);
    }
/**See {@link Rethrown#toString(int)}.
 */
    public String toString( int n) {
      String m;
      synchronized ( this ) {
      	boolean old = immediate;
	try {
	  immediate = true;
	  m = super.toString();
	}
	finally {
	  immediate = old;
	}
      }
      if ( n < 1  ||  last == null )
      	return m;
      m += separator;
      return last instanceof Rethrown
           ? m + ((Rethrown)last).toString( n-1)
	   : m + last.toString();
    }
  }

/**Base class for an unchecked exception that implements {@link Rethrown}.
 * Extend this to create an exception that does not have to be declared
 * in <CODE>throws</CODE> clauses because it is unexpected and suggests a
 * flaw in program source or the Java runtime.
 * If the exception represents a condition a user or administrator could be
 * reasonably expected to fix as a matter of course (say, by correcting a
 * path or obtaining a needed file), extend
 * {@link Rethrown.RuntimeException} instead.
 */
  public abstract class Error
    extends java.lang.Error
    implements Rethrown {
    
/**The encapsulated Throwable object.
 */
    protected final Throwable last;
/**Not of interest unless a subclass does hairy reformatting.
 * Set <CODE>true</CODE> in a <CODE>try/finally</CODE> block to ensure
 * it gets set back to false, causes the no-<EM>n</EM> versions of
 * {@link Rethrown#getMessage() getMessage},
 * {@link Rethrown#getLocalizedMessage() getLocalizedMessage},
 * {@link Rethrown#printStackTrace(PrintWriter) Rethrown.printStackTrace},
 * and {@link Rethrown#toString() toString} to revert to their non-overridden
 * {@link Throwable} behavior for the duration of the block.
 */
    protected transient boolean immediate = false;
/**Display depth to use for this exception (max of defaultDepth and depth
 * passed to constructor).
 */
    protected final int displayDepth;
/**Cached value of the <CODE>Rethrown.displayDepth</CODE> property.
 */
    protected static final int defaultDepth;
/**The string used to separate information on the current exception from
 * information on the prior exception that caused it.
 * Initialized to <CODE>line.separator</CODE> + "upon ". 
 */
    protected static String separator;
    static {
      int javacPacifier = 0;
      String sep = "\r\n";
      try { javacPacifier = Integer.getInteger( depthProp, 0).intValue(); }
      catch ( SecurityException e ) { }
      finally { defaultDepth = javacPacifier; }
      try { sep = System.getProperty( "line.separator"); }
      catch ( SecurityException e ) { }
      finally { separator = sep + "upon "; }
    }
    
/**Constructs a Rethrown.Error with no prior exception
 * and <CODE>null</CODE> message string.
 */
    public Error() { this( (Throwable)null, null, 0); }
/**Constructs a Rethrown.Error with no prior exception and a message,
 * and default display depth.
 *@param message Descriptive string saved for later retrieval.
 */
    public Error( String message) { this( (Throwable)null, message, 0); }
/**Constructs a Rethrown.Error from prior exception <EM>t</EM> and
 * a <CODE>null</CODE> message string, with default display depth.
 *@param t Prior exception whose details will be shown by the new object
 * for display depth 1 or more.
 */
    public Error( Throwable t) { this( t, null, 0); }
/**Constructs a Rethrown.Error from prior exception <EM>t</EM> and
 * a message.
 *@param t Prior exception whose details will be shown by the new object
 * for display depth 1 or more.
 *@param message Descriptive string saved for later retrieval.
 */
    public Error( Throwable t, String message) { this( t, message, 0); }
/**Constructs a Rethrown.Error from prior exception <EM>t</EM>, a
 * <CODE>null</CODE> message string, and a display depth.
 *@param t Prior exception whose details will be shown by the new object for
 * display depth 1 or more.
 *@depth How many prior exceptions to include in detail output.
 * The depth used is the max of this parameter and the default depth from the
 * Rethrown.displayDepth property.
 */
    public Error( Throwable t, int depth) { this( t, null, depth); }
/**Constructs a Rethrown.Error from prior exception <EM>t</EM>, a message,
 * and a display depth.
 *@param t Prior exception whose details will be shown by the new object for
 * display depth 1 or more.
 *@param message Descriptive string saved for later retrieval.
 *@depth How many prior exceptions to include in detail output.
 * The depth used is the max of this parameter and the default depth from the
 * Rethrown.displayDepth property.
 */
    public Error( Throwable t, String message, int depth) {
      super( message);
      last = t;
      displayDepth = Math.max( depth, defaultDepth);
    }
    
/**See {@link Rethrown#get(int)}.
 */
    public Throwable get( int n) {
      return n < 1
           ? this
      	   : last instanceof Rethrown
	   ? ((Rethrown)last).get( n-1)
	   : last;
    }
/**See {@link Rethrown#getLocalizedMessage(int)}.
 */
    public String getLocalizedMessage( int n) { return getMessage( n); }
/**See {@link Rethrown#getMessage}.
 */
    public String getMessage() {
      synchronized ( this ) {
      	if ( immediate )
      	  return super.getMessage();
      }
      return getMessage( displayDepth);
    }
/**See {@link Rethrown#getMessage(int)}.
 */
    public String getMessage( int n) {
      String m;
      synchronized ( this ) {
        boolean old = immediate;
        try {
          immediate = true;
          m = super.getMessage();
        }
        finally {
          immediate = old;
        }
      }
      if ( n < 1  ||  last == null )
	return m;
      m += separator;
      return last instanceof Rethrown
	   ? m + ((Rethrown)last).getMessage( n-1)
	   : m + last.getMessage();
    }
/**@deprecated See {@link Rethrown#printStackTrace(PrintWriter)}.
 */
    public void printStackTrace() {
      synchronized ( this ) {
      	if ( immediate ) {
      	  super.printStackTrace();
	  return;
	}
      }
      printStackTrace( System.err, displayDepth);
    }
/**@deprecated See {@link Rethrown#printStackTrace(PrintWriter)}.
 */
    public void printStackTrace( PrintStream s) {
      synchronized ( this ) {
      	if ( immediate ) {
      	  super.printStackTrace( s);
	  return;
	}
      }
      printStackTrace( s, displayDepth);
    }
/**See {@link Rethrown#printStackTrace(PrintWriter)}.
 */
    public void printStackTrace( PrintWriter s) {
      synchronized ( this ) {
      	if ( immediate ) {
      	  super.printStackTrace( s);
	  return;
	}
      }
      printStackTrace( s, displayDepth);
    }
/**See {@link Rethrown#printStackTrace(PrintWriter,int)}.
 */
    public void printStackTrace( PrintWriter s, int n) {
      if ( n < 1  ||  last == null ) {
      	synchronized ( this ) {
	  boolean old = immediate;
	  try {
	    immediate = true;
	    super.printStackTrace( s);
	  }
	  finally {
	    immediate = old;
	  }
	}
      }
      else {
      	s.print( toString( 0));
	s.print( separator);
 	if ( last instanceof Rethrown )
 	  ((Rethrown)last).printStackTrace( s, n-1);
 	else
 	  last.printStackTrace( s);
      }
    }
/**@deprecated See {@link Rethrown#printStackTrace(PrintWriter,int)}.
 */
    public void printStackTrace( PrintStream s, int n) {
      if ( n < 1  ||  last == null ) {
      	synchronized ( this ) {
	  boolean old = immediate;
	  try {
	    immediate = true;
	    super.printStackTrace( s);
	  }
	  finally {
	    immediate = old;
	  }
	}
      }
      else {
      	s.print( toString( 0));
	s.print( separator);
 	if ( last instanceof Rethrown )
 	  ((Rethrown)last).printStackTrace( s, n-1);
 	else
 	  last.printStackTrace( s);
      }
    }
/**See {@link Rethrown#toString()}.
 */
    public String toString() {
      synchronized ( this ) {
      	if ( immediate )
      	  return super.toString();
      }
      return toString( displayDepth);
    }
/**See {@link Rethrown#toString(int)}.
 */
    public String toString( int n) {
      String m;
      synchronized ( this ) {
      	boolean old = immediate;
	try {
	  immediate = true;
	  m = super.toString();
	}
	finally {
	  immediate = old;
	}
      }
      if ( n < 1  ||  last == null )
      	return m;
      m += separator;
      return last instanceof Rethrown
           ? m + ((Rethrown)last).toString( n-1)
	   : m + last.toString();
    }
  }
}
