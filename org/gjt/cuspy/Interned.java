package org.gjt.cuspy;

import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.lang.System; // workaround for javadoc not finding it

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
 *<P>
 * Objects that will always be interned can implement {@link Interned.Aware}.
 * Those that do can have very inexpensive {@link #equals(Object) equals()}
 * and {@link #hashCode() hashCode()} methods whose results (when the objects
 * are interned) will be equivalent to the real hash and equality tests
 * (which only {@link #intern(Object) intern()} needs to use).
 *@author <A HREF="mailto:chap@gjt.org">Chapman Flack</A>
 *@version $Id$
 */
public class Interned {
  /**Map used to implement a set by mapping object
     {@link Key Keys} to themselves.*/
  java.util.Map map;
  /**Queue on which the Java runtime system enters {@link Key Keys} once their
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
    k = new Key( o, queue);
    synchronized ( map ) {
      while ( null != ( v = (Key)queue.poll() ) )
	map.remove( v);
      v = (Key)map.get( k);
      if ( v != null ) {
	Object vo = v.get();
	if ( vo != null )
      	  return vo;
      }
      map.put( k, k);
    }
    return o;
  }
/**
 * Intern an object that implements the {@link Interned.Aware Aware} interface.
 * The object's {@link Interned.Aware#internHashCode() internHashCode()} and
 * {@link Interned.Aware#internEquals(Object) internEquals()} methods will be
 * used in preference to the usual {@link Object#hashCode() hashCode()} and
 * {@link Object#equals(Object) equals()} methods.
 * You can force an {@link Interned.Aware Aware} object to be treated as an
 * ordinary object for interning purposes by casting it to {@link Object} so the
 * compiler will select the more general
 * {@link Interned.Aware#intern(Object) intern(Object)} method instead.
 * You might want to do that if you have objects with very expensive
 * {@link Interned.Aware#internHashCode() internHashCode()} and
 * {@link Interned.Aware#internEquals(Object) internEquals()} methods, you have
 * one interning map that uses those methods, and you also want those objects
 * to participate in a less expensive interning map as well.
 *@param o An object implementing {@link Interned.Aware}.
 *@return o itself if no object
 * {@link Interned.Aware#internEquals(Object) equal} to it has been
 * interned before; otherwise returns the earlier-interned object.
 */  
  public Aware intern( Aware o) {
    Key k, v;
    k = new AwareKey( o, queue);
    synchronized ( map ) {
      while ( null != ( v = (Key)queue.poll() ) )
	map.remove( v);
      v = (Key)map.get( k);
      if ( v != null ) {
	Object vo = v.get();
	if ( vo != null )
      	  return (Aware)vo;
      }
      map.put( k, k);
    }
    return o;
  }
/**
 * Force o to be the representative of objects that equal o.
 * Any previously-interned object that equals o will be replaced.
 * It is the caller's responsibility to take care of any references
 * to the old representative still held by the program.
 *@param o The new representative
 */
  public void install( Object o) {
    Key k = new Key( o, queue);
    synchronized ( map ) {
      map.put( k, k);
    }
  }
/**
 * Force o to be the representative of objects that equal o.
 * The object will be treated as for
 * {@link #intern(Interned.Aware) intern(Aware)}.
 * Any previously-interned object that equals o will be replaced.
 * It is the caller's responsibility to take care of any references
 * to the old representative still held by the program.
 *@param o The new representative
 */
  public void install( Aware o) {
    Key k = new AwareKey( o, queue);
    synchronized ( map ) {
      map.put( k, k);
    }
  }
/**
 * A {@link WeakReference} whose {@link #hashCode() hashCode()} and
 * {@link #equals(Object) equals()} semantics are those of the referent object.
 */  
  private static class Key extends WeakReference {
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
 * even after the reference is cleared).
 */
    public int hashCode() { return hash; }
/**
 * True if <CODE>this</CODE> and <CODE>o</CODE> are the same Key, or
 * the objects they refer to are equal by the
 * {@link Object#equals(Object) equals()} method.
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
/**
 * Interface for objects that are "aware" they will be interned.  Such objects
 * have {@link #internHashCode()} and {@link #internEquals(Object)} methods
 * that implement the "real" contract for {@link #hashCode() hashCode()} and
 * {@link #equals(Object) equals()}, while the methods with the original
 * names implement
 * only the base {@link Object} semantics.
 * That is, {@link #equals(Object) equals()} should simply return
 * <CODE>this == other</CODE>, and {@link #hashCode() hashCode()} should
 * just return
 * <CODE>{@link System#identityHashCode(Object) System.identityHashCode}(this).
 * </CODE>
 * Once objects have been interned, those simplified methods will yield
 * behavior equivalent to the real ones, with less computation.
 */  
  public static interface Aware {
    /**The "real" hashCode.
     *@return A value fulfilling the contract for {@link #hashCode() hashCode()}
     * with respect to {@link #internEquals(Object)}.
     */
    public int internHashCode();
    /**The "real" equality test.
     *@param o An object
     *@return true if and only <CODE>this</CODE> and <CODE>o</CODE> are equal
     * in the usual sense for overriding {@link #equals(Object) equals()}
     * methods, false otherwise.
     */
    public boolean internEquals( Object o);
  }
/**
 * A {@link Interned.Key Key} whose referent object is
 * {@link Interned.Aware Aware} and
 * whose {@link #hashCode() hashCode()} and {@link #equals(Object) equals()}
 * semantics are those of the referent object's
 * {@link Interned.Aware#internHashCode() internHashCode()} and
 * {@link Interned.Aware#internEquals(Object) internEquals()} methods.
 */  
  private static final class AwareKey extends Key {
/**
 * Construct an AwareKey for an object; associate the key with a queue.
 *@param k An object to be interned.
 *@param q {@link ReferenceQueue} on which this AwareKey should be enqueued
 * when its referent is garbage-collected.
 */
    AwareKey( Aware k, ReferenceQueue q) {
      super( k, q);
      hash = k.internHashCode();
    }
/**
 * True if <CODE>this</CODE> and <CODE>o</CODE> are the same AwareKey, or
 * the objects they refer to are equal by the
 * {@link Interned.Aware#internEquals(Object) internEquals()} method.
 * The same-key check is required so the Map lookup and removal of a cleared
 * AwareKey will succeed.
 */
    public boolean equals( Object o) {
      if ( o == this ) return true;
      try {
      	Object ok = ((Key)o).get();
	Aware  tk = (Aware)get();
	return tk.internEquals( ok);
      }
      catch ( NullPointerException e ) { }
      catch ( ClassCastException e ) { }
      return false;
    }
  }
}
