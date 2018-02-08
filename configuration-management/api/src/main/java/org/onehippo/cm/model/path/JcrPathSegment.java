/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.path;

import java.util.Comparator;

/**
 * Represents a segment of a {@link JcrPath}, which includes a node name String and possibly a same-named-sibling index.
 * Note that implementations of this class should implement a non-standard natural ordering and definitions of
 * {@link #equals(Object)} and {@link #hashCode()} which treat the index value 0 (representing unindexed names) and 1
 * (representing the first indexed same-named-sibling) as strictly equivalent. Client code that wishes to treat these
 * states as distinct must use the {@link #compareUnindexedFirst(JcrPathSegment)} and
 * {@link #equalsUnindexedSignificant(Object)} methods or the {@link #UNINDEXED_FIRST_ORDER} comparator.
 */
// TODO: support concept of property segment that cannot accept an index != 0
public interface JcrPathSegment extends Comparable<JcrPathSegment> {

    /**
     * Comparator that treats unindexed segments as distinct (and less-than) the same name with index 1.
     */
    Comparator<JcrPathSegment> UNINDEXED_FIRST_ORDER =
            Comparator.comparing(JcrPathSegment::getName).thenComparingInt(JcrPathSegment::getIndex);

    /**
     * @return true iff this JcrPathSegment is the singleton instance representing a root node
     */
    boolean isRoot();

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
     * @param newIndex the index for the new instance of {@link JcrPathSegment}
     * @return a new {@link JcrPathSegment} instance with the same name as this instance and the given index
     */
    JcrPathSegment withIndex(final int newIndex);

    /**
     * @return if this name is unindexed, a variant with index 1; otherwise, this
     */
    JcrPathSegment forceIndex();

    /**
     * @return if this name has index 1, a variant with no index (i.e. index 0); otherwise, this
     */
    JcrPathSegment suppressIndex();

    /**
     * Treats unindexed name as equivalent to index of 1.
     */
    @Override
    int compareTo(final JcrPathSegment o);

    /**
     * Compare using {@link #UNINDEXED_FIRST_ORDER}.
     */
    int compareUnindexedFirst(final JcrPathSegment o);

    /**
     * Treats unindexed name as equivalent to index of 1.
     */
    @Override
    boolean equals(final Object o);

    /**
     * Equals comparison consistent with {@link #UNINDEXED_FIRST_ORDER}.
     */
    boolean equalsUnindexedSignificant(final Object o);

    /**
     * Treats unindexed name as equivalent to index of 1 (matching equals() and compareTo()).
     */
    @Override
    int hashCode();
}
