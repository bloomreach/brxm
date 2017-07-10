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
package org.onehippo.cm.model.impl;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.model.NodePathSegment;
import org.onehippo.cm.model.util.SnsUtils;

public class NodePathSegmentImpl implements NodePathSegment {

    public static final NodePathSegment ROOT_NAME = new NodePathSegmentImpl("", 0);

    // todo: is it true that node indices cannot have leading zeros?
    private static Pattern pattern = Pattern.compile("^([^\\[\\]]+)(\\[([1-9][0-9]*)])?$");

    public static NodePathSegment get(final String fullName) {
        if (fullName == null) {
            throw new IllegalArgumentException("Name must not be null!");
        }

        if (fullName.equals("") || fullName.equals("/")) {
            return ROOT_NAME;
        }
        else {
            return new NodePathSegmentImpl(fullName);
        }
    }

    public static NodePathSegment get(final String name, final int index) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null!");
        }

        if (name.equals("") || name.equals("/")) {
            return ROOT_NAME;
        }
        else {
            return new NodePathSegmentImpl(name, index);
        }
    }

    // todo: split prefix? no obvious use case in HCM right now
    /**
     * Implementation note: Node name is interned for optimized memory usage.
     */
    private final String name;
    private final int index;

    /**
     * Create a new instance with an existing unindexed name and an explicit index.
     * @param name an unindexed node name
     * @param index the desired new index
     */
    private NodePathSegmentImpl(final String name, final int index) {
        this.name = name.intern();
        this.index = index;
        checkArgs();
    }

    /**
     * Create a new instance by splitting a possibly-indexed name.
     * @param fullName a JCR node name with or without an index
     */
    private NodePathSegmentImpl(final String fullName) {
        final Pair<String, Integer> split = splitIndexedName(fullName);
        this.name = split.getLeft().intern();
        this.index = split.getRight();
        checkArgs();
    }

    // TODO: remove duplicated code in SnsUtils once we've fully converted to using this API instead
    private static Pair<String, Integer> splitIndexedName(final String name) {
        final Matcher matcher = pattern.matcher(name);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Name '" + name + "' is not a valid indexed name");
        }
        try {
            if (matcher.group(2) == null) {
                return Pair.of(matcher.group(1), 0);
            }
            final int index = Integer.parseInt(matcher.group(3));
            return Pair.of(matcher.group(1), index);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Name '" + name + "' is not a valid indexed name");
        }
    }

    private void checkArgs() {
        if (name.contains("[")) {
            throw new IllegalArgumentException("Name should not already contain an index when adding an explicit index!");
        }

        if (index < 0) {
            throw new IllegalArgumentException("Segment index cannot be less than zero!");
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public boolean hasIndex() {
        return index != 0;
    }

    @Override
    public NodePathSegment withIndex(final int newIndex) {
        if (isRoot()) {
            throw new IllegalStateException("Root name cannot have an index!");
        }

        if (this.index == newIndex) {
            return this;
        }
        else {
            return new NodePathSegmentImpl(name, newIndex);
        }
    }

    @Override
    public NodePathSegment forceIndex() {
        if (hasIndex() || isRoot()) {
            return this;
        }
        else {
            return withIndex(1);
        }
    }

    @Override
    public NodePathSegment suppressIndex() {
        if (index == 1) {
            return withIndex(0);
        }
        else {
            return this;
        }
    }

    @Override
    public boolean isRoot() {
        return this == ROOT_NAME;
    }

    @Override
    public int compareTo(final NodePathSegment o) {
        final int sVal = name.compareTo(o.getName());
        if (sVal != 0) {
            return sVal;
        }

        // if both have zero-or-one index, they are treated as equal
        if ((index == 1 || index == 0)
                && (o.getIndex() == 1 || o.getIndex() == 0)) {
            return 0;
        }

        return Integer.compare(index, o.getIndex());
    }

    /**
     * Compare using {@link #UNINDEXED_FIRST_ORDER}.
     */
    @Override
    public int compareUnindexedFirst(final NodePathSegment o) {
        return UNINDEXED_FIRST_ORDER.compare(this, o);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        final NodePathSegment other;
        if (o instanceof String) {
            // allows for nodeName.equals("/other/path") and nodes.contains("/other/path"), etc.
            other = NodePathSegmentImpl.get((String) o);
        }
        else if (!(o instanceof NodePathSegment)) {
            return false;
        }
        else {
            other = (NodePathSegment) o;
        }

        if (!name.equals(other.getName())) {
            return false;
        }

        if (index == other.getIndex()) {
            return true;
        }

        // zero and one are functionally the same
        return (index == 1 || index == 0)
                && (other.getIndex() == 1 || other.getIndex() == 0);
    }

    @Override
    public boolean equalsUnindexedSignificant(final Object o) {
        if (this == o) {
            return true;
        }

        final NodePathSegment other;
        if (o instanceof String) {
            // allows for nodeName.equals("/other/path") and nodes.contains("/other/path"), etc.
            other = NodePathSegmentImpl.get((String) o);
        } else if (!(o instanceof NodePathSegment)) {
            return false;
        } else {
            other = (NodePathSegment) o;
        }

        return name.equals(other.getName()) && (index == other.getIndex());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, (index == 0? 1: index));
    }

    @Override
    public String toString() {
        return name + (hasIndex()? ("[" + index + "]"): "");
    }
}
