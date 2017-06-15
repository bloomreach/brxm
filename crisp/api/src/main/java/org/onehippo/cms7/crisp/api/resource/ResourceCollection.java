package org.onehippo.cms7.crisp.api.resource;

import java.util.Collection;

/**
 * Read-only {@link Iterable} interface with some read-only Collection/List operations such as <code>size()</code>
 * or <code>get(int)</code>.
 */
public interface ResourceCollection extends Iterable<Resource> {

    /**
     * Returns a read-only collection over elements of type {@code Resource}.
     * <P>
     * This method is provided for templating language such as Freemarker
     * because Freemarker doesn't allow to list or iterator an <code>Iterable</code> directly as of v2.3.x. for
     * instance.
     * </P>
     *
     * @return a resource collection.
     */
    Collection<Resource> getCollection();

    /**
     * Returns the number of elements in this resource collection. If this resource collection
     * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of elements in this resource collection
     */
    int size();

    /**
     * Returns the resource element at the specified position in this resource collection.
     *
     * @param index index of the resource element to return
     * @return the resource element at the specified position in this resource collection
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    Resource get(int index);

}
