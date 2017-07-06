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

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

/**
 * Represents a multi-segment JCR node path.
 */
public class NodePath implements Comparable<NodePath>, Iterable<NodeName> {

    public static final NodePath ROOT = new NodePath(ImmutableList.of(), false);

    public static final NodePath getRoot() {
        return ROOT;
    }

    public static NodePath get(String path, final String... more) {

        // do this check now to check for null or pure whitespace
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("Path string must not be blank!");
        }

        // "/node/" => "/node"; "///" => ""
        if (path.endsWith("/") && !path.equals("/")) {
            path = StringUtils.stripEnd(path, "/");
        }

        // collapse "more" into a single path, so that this will work: get("/", "  ", "/parent/child", "/grandchild/");
        if (ArrayUtils.isNotEmpty(more)) {
            path += stream(more).map(StringUtils::stripToEmpty).map(s -> StringUtils.stripEnd(s, "/"))
                    .filter(StringUtils::isNotBlank).collect(joining("/", "/", ""));
        }

        // root is special
        if (path.equals("/")) {
            return ROOT;
        }

        // do this check now to detect weirdness like "////"
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("Path string must contain non-empty segments!");
        }

        final boolean relative = !path.startsWith("/");
        final String[] segments = StringUtils.strip(path, "/").split("/");

        final ImmutableList<NodeName> names =
                stream(segments).map(NodeName::new).collect(ImmutableList.toImmutableList());

        return new NodePath(names, relative);
    }

    public final ImmutableList<NodeName> names;
    public final boolean absolute;

    private NodePath(final ImmutableList<NodeName> names, final boolean absolute) {
        this.names = names;
        this.absolute = absolute;
    }

    public boolean isAbsolute() {
        return absolute;
    }

    public NodeName getLastName() {
        if (this == ROOT) {
            // todo: should this return a constant NodeName instead?
            throw new IllegalStateException("Root path has no node names");
        }

        return names.get(names.size()-1);
    }

    public NodePath getParent() {
        // todo: implement this in a more memory-efficient way with Lisp-style list objects
        return new NodePath(names.subList(0, names.size()-1), absolute);
    }

    public int getNameCount() {
        return names.size();
    }

    public NodeName getName(final int index) {
        return names.get(index);
    }

    public NodePath subpath(final int beginIndex, final int endIndex) {
        return new NodePath(names.subList(beginIndex, endIndex), beginIndex == 0);
    }

    public boolean startsWith(final String other) {
        return startsWith(get(other));
    }

    public boolean startsWith(final NodePath other) {
        if (other.getNameCount() > names.size()) {
            return false;
        }

        return names.subList(0, other.getNameCount()).equals(other.names);
    }

    public boolean endsWith(final NodePath other) {
        if (other.getNameCount() > names.size()) {
            return false;
        }

        return names.subList(names.size()-other.getNameCount(), names.size()).equals(other.names);
    }

    public NodePath resolve(final String other) {
        return resolve(get(other));
    }

    public NodePath resolve(final NodePath other) {
        return new NodePath(ImmutableList.copyOf(Iterables.concat(names, other.names)), absolute);
    }

    public NodePath resolveSibling(final String other) {
        // todo: reduce memory churn
        return getParent().resolve(other);
    }

    public NodePath resolveSibling(final NodePath other) {
        // todo: reduce memory churn
        return getParent().resolve(other);
    }

    // todo: normalize

    public NodePath relativize(final NodePath other) {
        if (other == ROOT) {
            return new NodePath(names, false);
        }

        if (other.startsWith(this)) {
            return other.subpath(names.size(), other.getNameCount());
        }

        // generate relative paths up to the root and then work down again
        // todo: remove common ancestors
        return get(Streams.concat(Stream.generate(() -> "..").limit(names.size()),
                    other.names.stream().map(NodeName::toString)).collect(joining("/")));
    }

    public NodePath toAbsolutePath() {
        if (absolute) {
            return this;
        }

        return new NodePath(names, true);
    }

    public Iterator<NodeName> iterator() {
        return names.iterator();
    }

    /**
     * Compare paths by name segment, with unindexed names considered to be identical to name with index 1.
     */
    @Override
    public int compareTo(final NodePath o) {
        final int shorterLength = (names.size() <= o.names.size())? names.size(): o.names.size();

        for (int i = 0; i < shorterLength; i++) {
            final int curComp = names.get(i).compareTo(o.names.get(i));

            if (curComp != 0) {
                return curComp;
            }
        }

        // if all existing names are equal, the shorter path is less than the longer path
        int sizeComp = Integer.compare(names.size(), o.names.size());

        // ultimate tie-breaker is that relative paths are less-than absolute paths
        if (sizeComp == 0) {
            return Boolean.compare(absolute, o.absolute);
        }

        return sizeComp;
    }

    /**
     * @return true iff all name segments are equal, according to {@link NodeName#equals(Object)}.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof NodePath)) {
            return false;
        }

        final NodePath other = (NodePath) obj;
        return names.equals(other.names) && (absolute == other.absolute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(names, absolute);
    }

    @Override
    public String toString() {
        return names.stream().map(NodeName::toString).collect(joining("/", absolute?"/":"", ""));
    }
}
