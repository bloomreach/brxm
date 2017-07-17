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
package org.onehippo.cm.model.impl.path;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;

import static java.util.stream.Collectors.joining;

/**
 * Represents a multi-segment JCR node path with possible same-named-sibling indices on each segment.
 * Note that this is essentially an analogy to the {@link java.nio.file.Path} API, and instances can be used in a way
 * familiar to users of that class.
 */
public class JcrPath implements Comparable<JcrPath>, Iterable<JcrPathSegment> {

    /**
     * A constant value representing the path of the JCR root node.
     */
    public static final JcrPath ROOT = new JcrPath(ImmutableList.of(), false, true);

    /**
     * Static factory for NodePath instances.
     * @see java.nio.file.Paths#get(String, String...)
     */
    public static JcrPath get(String path, final String... more) {

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
            path += Arrays.stream(more).map(StringUtils::stripToEmpty).map(s -> StringUtils.stripEnd(s, "/"))
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

        final ImmutableList<JcrPathSegment> names =
                Arrays.stream(segments).map(JcrPathSegment::get).collect(ImmutableList.toImmutableList());

        return new JcrPath(names, relative);
    }

    private final ImmutableList<JcrPathSegment> segments;
    private final boolean absolute;

    // private to guarantee ROOT is a constant
    private JcrPath(final ImmutableList<JcrPathSegment> segments, final boolean absolute) {
        this(segments, absolute, false);
    }

    // private to guarantee ROOT is a constant
    private JcrPath(final ImmutableList<JcrPathSegment> segments, final boolean absolute, final boolean isRoot) {
        // this check is mainly for internal purposes, to make sure we're not accidentally failing to return ROOT
        if (!isRoot && segments.size() == 0) {
            throw new IllegalArgumentException("Should use ROOT instead of constructing new NodePath with no segments!");
        }

        this.segments = segments;

        // todo: debug this -- for now, assume a leading slash (i.e. an absolute path)
        this.absolute = true;
    }

    /**
     * @return a constant value representing the path of the JCR root node
     */
    public JcrPath getRoot() {
        return ROOT;
    }

    /**
     * @return true iff this instance represents the path of the JCR root node
     */
    public boolean isRoot() {
        return this == ROOT;
    }

    /**
     * @return true if this path is an absolute path, represented in String form with a leading "/"
     */
    public boolean isAbsolute() {
        return absolute;
    }

    /**
     * @return the final segment of this {@link JcrPath}, which may represent a JCR Node or Property
     */
    public JcrPathSegment getLastSegment() {
        if (isRoot()) {
            // todo: should this return a constant NodePathSegment instead?
            throw new IllegalStateException("Root path has no path segments!");
        }

        return segments.get(segments.size()-1);
    }

    /**
     * @return a path representing the parent node of this path
     * @throws IllegalStateException iff {@link #isRoot()}
     */
    public JcrPath getParent() {
        if (isRoot()) {
            throw new IllegalStateException("Cannot get the parent of ROOT!");
        }

        if (segments.size() == 1) {
            return ROOT;
        }

        // todo: implement this in a more memory-efficient way with Lisp-style list objects
        return new JcrPath(segments.subList(0, segments.size()-1), absolute);
    }

    /**
     * @return the number of separate path segments are composed in this path
     */
    public int getSegmentCount() {
        return segments.size();
    }

    /**
     * @param index the zero-based index of the desired path segment
     * @return a {@link JcrPathSegment} representing the segment of this path at the index position
     */
    public JcrPathSegment getSegment(final int index) {
        return segments.get(index);
    }

    /**
     * @param beginIndex the zero-based index (inclusive) of the starting segment of the desired subpath
     * @param endIndex the zero-based index (exclusive) of the ending segment of the desired subpath
     * @return a path representing a specific portion of this path
     */
    public JcrPath subpath(final int beginIndex, final int endIndex) {
        if (beginIndex == 0 && endIndex == 0) {
            return ROOT;
        }

        return new JcrPath(segments.subList(beginIndex, endIndex), beginIndex == 0);
    }

    /**
     * @see Path#startsWith(String)
     */
    public boolean startsWith(final String other) {
        return startsWith(get(other));
    }

    /**
     * @see Path#startsWith(String)
     */
    public boolean startsWith(final JcrPathSegment other) {
        if (other.isRoot()) {
            return true;
        }

        return startsWith(new JcrPath(ImmutableList.of(other), false));
    }

    /**
     * @see Path#startsWith(Path)
     */
    public boolean startsWith(final JcrPath other) {
        return (other.getSegmentCount() <= segments.size())
                && Iterables.elementsEqual(this, other);
    }

    /**
     * @see Path#endsWith(String)
     */
    public boolean endsWith(final String other) {
        return endsWith(get(other));
    }

    /**
     * @see Path#endsWith(String)
     */
    public boolean endsWith(final JcrPathSegment other) {
        if (other.isRoot()) {
            return true;
        }

        return endsWith(new JcrPath(ImmutableList.of(other), false));
    }

    /**
     * @see Path#endsWith(Path)
     */
    public boolean endsWith(final JcrPath other) {
        return (other.getSegmentCount() <= segments.size())
                && Iterables.elementsEqual(
                        segments.subList(segments.size() - other.getSegmentCount(), segments.size()),
                        other);
    }

    /**
     * @see Path#resolve(String)
     */
    public JcrPath resolve(final String other) {
        return resolve(get(other));
    }

    /**
     * @see Path#resolve(String)
     */
    public JcrPath resolve(final JcrPathSegment other) {
        if (other.isRoot()) {
            return this;
        }

        return resolve(new JcrPath(ImmutableList.of(other), false));
    }

    /**
     * @see Path#resolve(Path)
     */
    public JcrPath resolve(final JcrPath other) {
        if (other.isRoot()) {
            return this;
        }
        if (isRoot() && other.isAbsolute()) {
            return other;
        }

        return new JcrPath(ImmutableList.copyOf(Iterables.concat(segments, other)), absolute);
    }

    /**
     * @see Path#resolveSibling(String)
     */
    public JcrPath resolveSibling(final String other) {
        // todo: reduce memory churn
        return getParent().resolve(other);
    }

    /**
     * @see Path#resolveSibling(String)
     */
    public JcrPath resolveSibling(final JcrPathSegment other) {
        if (other.isRoot()) {
            return getParent();
        }

        return resolveSibling(new JcrPath(ImmutableList.of(other), false));
    }

    /**
     * @see Path#resolveSibling(Path)
     */
    public JcrPath resolveSibling(final JcrPath other) {
        // todo: reduce memory churn
        return getParent().resolve(other);
    }

    /**
     * @see Path#relativize(Path)
     */
    public JcrPath relativize(final JcrPath other) {
        if (other.isRoot()) {
            return new JcrPath(segments, false);
        }

        if (other.startsWith(this)) {
            return other.subpath(segments.size(), other.getSegmentCount());
        }

        // generate relative paths up to the root and then work down again
        // todo: remove common ancestors
        return get(Streams.concat(Stream.generate(() -> "..").limit(segments.size()),
                    other.stream().map(JcrPathSegment::toString)).collect(joining("/")));
    }

    // todo: normalize

    /**
     * @return if {@link #isRoot()} or {@link #isAbsolute()}, this; otherwise, a new variant of this path for which
     * {@link #isAbsolute()} == true
     */
    public JcrPath toAbsolutePath() {
        if (absolute || isRoot()) {
            return this;
        }

        return new JcrPath(segments, true);
    }

    /**
     * @return a NodePath equivalent to this one, except that each segment has had forceIndex() applied to it
     */
    public JcrPath toFullyIndexedPath() {
        if (isRoot()) {
            return this;
        }

        ImmutableList<JcrPathSegment> indexedSegments =
                segments.stream().map(JcrPathSegment::forceIndex).collect(ImmutableList.toImmutableList());
        return new JcrPath(indexedSegments, absolute);
    }

    /**
     * @return a NodePath equivalent to this one, except that each segment has had suppressIndex() applied to it
     */
    public JcrPath toMinimallyIndexedPath() {
        if (isRoot()) {
            return this;
        }

        ImmutableList<JcrPathSegment> indexedSegments =
                segments.stream().map(JcrPathSegment::suppressIndex).collect(ImmutableList.toImmutableList());
        return new JcrPath(indexedSegments, absolute);
    }

    /**
     * @return an ordered, serial stream of NodePathSegments in the same order used by {@link #getSegment(int)}
     */
    public Stream<JcrPathSegment> stream() {
        return segments.stream();
    }

    @Override
    public Iterator<JcrPathSegment> iterator() {
        return segments.iterator();
    }

    /**
     * Compare paths by name segment, with unindexed segments considered to be identical to name with index 1.
     */
    @Override
    public int compareTo(final JcrPath o) {
        final int shorterLength = (segments.size() <= o.getSegmentCount())? segments.size(): o.getSegmentCount();

        for (int i = 0; i < shorterLength; i++) {
            final int curComp = segments.get(i).compareTo(o.getSegment(i));

            if (curComp != 0) {
                return curComp;
            }
        }

        // if all existing segments are equal, the shorter path is less than the longer path
        int sizeComp = Integer.compare(segments.size(), o.getSegmentCount());

        // ultimate tie-breaker is that relative paths are less-than absolute paths
        if (sizeComp == 0) {
            return Boolean.compare(absolute, o.isAbsolute());
        }

        return sizeComp;
    }

    /**
     * @return true iff all name segments are equal, according to {@link JcrPathSegment#equals(Object)}.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        final JcrPath other;
        if (obj instanceof String) {
            other = get((String) obj);
        }
        else if (obj instanceof JcrPathSegment) {
            other = new JcrPath(ImmutableList.of((JcrPathSegment) obj), true);
        }
        else if (!(obj instanceof JcrPath)) {
            return false;
        }
        else {
            other = (JcrPath) obj;
        }
        return Iterables.elementsEqual(segments, other) && (absolute == other.isAbsolute());
    }

    @Override
    public int hashCode() {
        return Objects.hash(segments, absolute);
    }

    @Override
    public String toString() {
        return segments.stream().map(JcrPathSegment::toString).collect(joining("/", absolute?"/":"", ""));
    }
}
