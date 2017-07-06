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
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.model.util.SnsUtils;

// TODO: move this to API -- do we really need to keep SnsUtils out of API?
public class NodeName implements Comparable<NodeName> {

    /**
     * Comparator that treats unindexed names as distinct (and less-than) the same name with index 1.
     */
    public static final Comparator<NodeName> UNINDEXED_FIRST_ORDER =
            Comparator.comparing(NodeName::getUnindexedName).thenComparingInt(NodeName::getIndex);

    // todo: split prefix? no obvious use case in HCM right now
    private final String name;
    private final int index;

    /**
     * Create a new instance with an existing unindexed name and an explicit index.
     * @param name an unindexed node name
     * @param index the desired new index
     */
    public NodeName(final String name, final int index) {
        this.name = name;
        this.index = index;

        if (name.contains("[")) {
            throw new IllegalArgumentException("Name should not already contain an index when adding an explicit index!");
        }
    }

    /**
     * Create a new instance by splitting a possibly-indexed name.
     * @param fullName a JCR node name with or without an index
     */
    public NodeName(final String fullName) {
        final Pair<String, Integer> split = SnsUtils.splitIndexedName(fullName);
        this.name = split.getLeft();
        this.index = split.getRight();

        if (index < 0) {
            throw new IllegalArgumentException("JCR Node index cannot be less than zero!");
        }
    }

    public String getUnindexedName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public boolean hasIndex() {
        return index != 0;
    }

    /**
     * @param newIndex the index for the new instance of JcrNodeName
     * @return a new JcrNodeName instance with the same name as this instance and the given index
     */
    public NodeName withIndex(final int newIndex) {
        return new NodeName(name, newIndex);
    }

    /**
     * @return if this name is unindexed, a variant with index 1; otherwise, this
     */
    public NodeName forceIndex() {
        if (hasIndex()) {
            return this;
        }
        else {
            return withIndex(1);
        }
    }

    /**
     * Treats unindexed name as equivalent to index of 1.
     */
    @Override
    public int compareTo(final NodeName o) {
        final int sVal = name.compareTo(o.getUnindexedName());
        if (sVal != 0) {
            return sVal;
        }

        // if both have zero-or-one index, they are treated as equal
        if ((index == 1 || index == 0)
                && (o.index == 1 || o.index == 0)) {
            return 0;
        }

        return Integer.compare(index, o.getIndex());
    }

    /**
     * Compare using {@link #UNINDEXED_FIRST_ORDER}.
     */
    public int compareUnindexedFirst(final NodeName o) {
        return UNINDEXED_FIRST_ORDER.compare(this, o);
    }

    /**
     * Treats unindexed name as equivalent to index of 1.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        final NodeName other;
        if (o instanceof String) {
            // allows for nodeName.equals("/other/path") and nodes.contains("/other/path"), etc.
            other = new NodeName((String) o);
        }
        else if (!(o instanceof NodeName)) {
            return false;
        }
        else {
            other = (NodeName) o;
        }

        if (!name.equals(other.name)) {
            return false;
        }

        if (index == other.index) {
            return true;
        }

        // zero and one are functionally the same
        return (index == 1 || index == 0)
                && (other.index == 1 || other.index == 0);
    }

    /**
     * Equals comparison consistent with {@link #UNINDEXED_FIRST_ORDER}.
     */
    public boolean equalsUnindexedSignificant(final Object o) {
        if (this == o) {
            return true;
        }

        final NodeName other;
        if (o instanceof String) {
            // allows for nodeName.equals("/other/path") and nodes.contains("/other/path"), etc.
            other = new NodeName((String) o);
        }
        else if (!(o instanceof NodeName)) {
            return false;
        }
        else {
            other = (NodeName) o;
        }

        if (!name.equals(other.name)) {
            return false;
        }

        return (index == other.index);
    }

    /**
     * Treats unindexed name as equivalent to index of 1 (matching equals() and compareTo()).
     * @return
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, (index == 0? 1: index));
    }

    @Override
    public String toString() {
        return name + (hasIndex()? ("[" + index + "]"): "");
    }
}
