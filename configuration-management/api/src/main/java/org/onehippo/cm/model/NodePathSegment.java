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
package org.onehippo.cm.model;

import java.util.Comparator;

/**
 * Represents a segment of a {@link NodePath}, which includes a node name String and possibly a same-named-sibling index.
 */
public interface NodePathSegment extends Comparable<NodePathSegment> {
    /**
     * Comparator that treats unindexed segments as distinct (and less-than) the same name with index 1.
     */
    Comparator<NodePathSegment> UNINDEXED_FIRST_ORDER =
            Comparator.comparing(NodePathSegment::getName).thenComparingInt(NodePathSegment::getIndex);

    /**
     * @return the String value of the full node name, without possible same-named-sibling index
     */
    String getName();

    /**
     * @return the int value of the same-named-sibling index, or 0 if !{@link #hasIndex()}
     */
    int getIndex();

    /**
     * @return does this name have a same-named-sibling index?
     */
    boolean hasIndex();

    /**
     * @param newIndex the index for the new instance of {@link NodePathSegment}
     * @return a new {@link NodePathSegment} instance with the same name as this instance and the given index
     */
    NodePathSegment withIndex(int newIndex);

    /**
     * @return if this name is unindexed, a variant with index 1; otherwise, this
     */
    NodePathSegment forceIndex();

    /**
     * Treats unindexed name as equivalent to index of 1.
     */
    @Override
    int compareTo(NodePathSegment o);

    /**
     * Compare using {@link #UNINDEXED_FIRST_ORDER}.
     */
    int compareUnindexedFirst(NodePathSegment o);

    /**
     * Treats unindexed name as equivalent to index of 1.
     */
    @Override
    boolean equals(Object o);

    /**
     * Equals comparison consistent with {@link #UNINDEXED_FIRST_ORDER}.
     */
    boolean equalsUnindexedSignificant(Object o);

    /**
     * Treats unindexed name as equivalent to index of 1 (matching equals() and compareTo()).
     */
    @Override
    int hashCode();
}
