/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
