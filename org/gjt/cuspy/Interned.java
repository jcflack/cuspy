package org.gjt.cuspy;

import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * A canonicalizing map. An instance of Interned provides the method
 * {@link #intern(Object)} that either returns its parameter, or returns
 * a reference to an earlier-interned object that {@link #equals(Object) equals}
 * the parameter. That is, by calling <CODE>intern()</CODE> on newly-created
 * <CODE>Foo</CODE> objects, and keeping only the returned references, one can
 * enforce a convention that two <CODE>Foo</CODE> objects are
 * {@link #equals(Object) equal} if and only if they are ==.
 *<P>
 * The map internally employs {@link WeakReference weak references} so the
 * objects entered in the map can be garbage-collected as soon as no more
 * normal references to them are held by the program.  When referenced objects
 * are garbage-collected, the corresponding entries are efficiently removed
 * from the map on the next call of <CODE>intern()</CODE>.
 * The map is similar to {@link WeakHashMap}, but a WeakHashMap
 * is concerned with allowing its <EM>keys</EM> to be garbage-collected,
 * while Interned permits garbage collection of the <EM>values</EM>.
 *@author <A HREF="mailto:chap@gjt.org">Chapman Flack</A>
 *@version $Id$
 */
public class Interned {
  /**Map used to implement a set by mapping object
     {@link Key}s to themselves.*/
  java.util.Map map;
  /**Queue on which the Java runtime system enters {@link Key}s once their
     referents have been swept up.*/
  ReferenceQueue queue = new ReferenceQueue();
/**
 * Construct an Interned map with default initial size.
 */
  public Interned() { map = new HashMap(); }
/**
 * Construct an Interned map with specified initial size.
 *@param size Number of entries expected to be held at any one time.
 */
  public Interned( int size) { map = new HashMap( size); }
/**
 * Intern an object.
 *@param o An object.
 *@return o itself if no object {@link #equals(Object) equal} to it has been
 * interned before; otherwise returns the earlier-interned object.
 */  
  public Object intern( Object o) {
    Key k, v;
    while ( null != ( k = (Key)queue.poll() ) )
      map.remove( k);
    k = new Key( o, queue);
    v = (Key)map.get( k);
    if ( v != null ) {
      Object vo = v.get();
      if ( vo != null )
      	return vo;
    }
    map.put( k, k);
    return o;
  }
/**
 * A {@link WeakReference} whose {@link #hashCode()} and
 * {@link #equals(Object)} semantics are those of the referent object.
 */  
  private static final class Key extends WeakReference {
    /**Cached hash of referent.*/
    int hash;
/**
 * Construct a Key for an object; associate the key with a queue.
 *@param k An object to be interned.
 *@param q {@link ReferenceQueue} on which this Key should be enqueued
 * when its referent is garbage-collected.
 */
    Key( Object k, ReferenceQueue q) {
      super( k, q);
      hash = k.hashCode();
    }
/**
 * The hash of the original referent (cached, so Map lookup will succeed
 * even after the reference is cleared.
 */
    public int hashCode() { return hash; }
/**
 * True if <CODE>this</CODE> and <CODE>o</CODE> are the same Key, or
 * the objects they refer to are equal by the <CODE>equals()</CODE> method.
 * The same-key check is required so the Map lookup and removal of a cleared
 * Key will succeed.
 */
    public boolean equals( Object o) {
      if ( o == this ) return true;
      try {
      	Object ok = ((Key)o).get();
	Object tk = get();
	return tk.equals( ok);
      }
      catch ( NullPointerException e ) { }
      catch ( ClassCastException e ) { }
      return false;
    }
  }
}
