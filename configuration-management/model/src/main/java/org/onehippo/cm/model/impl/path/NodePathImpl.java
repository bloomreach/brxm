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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.path.NodePath;
import org.onehippo.cm.model.path.NodePathSegment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;

import static java.util.stream.Collectors.joining;

public class NodePathImpl implements NodePath {

    public static final NodePath ROOT = new NodePathImpl(ImmutableList.of(), false, true);

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

        final ImmutableList<NodePathSegment> names =
                Arrays.stream(segments).map(NodePathSegmentImpl::get).collect(ImmutableList.toImmutableList());

        return new NodePathImpl(names, relative);
    }

    public final ImmutableList<NodePathSegment> segments;
    public final boolean absolute;

    private NodePathImpl(final ImmutableList<NodePathSegment> segments, final boolean absolute) {
        this(segments, absolute, false);
    }

    private NodePathImpl(final ImmutableList<NodePathSegment> segments, final boolean absolute, final boolean isRoot) {
        // this check is mainly for internal purposes, to make sure we're not accidentally failing to return ROOT
        if (!isRoot && segments.size() == 0) {
            throw new IllegalArgumentException("Should use ROOT instead of constructing new NodePath with no segments!");
        }

        this.segments = segments;

        // todo: debug this -- for now, assume a leading slash (i.e. an absolute path)
        this.absolute = true;
    }

    @Override
    public NodePath getRoot() {
        return ROOT;
    }

    @Override
    public boolean isRoot() {
        return this == ROOT;
    }

    @Override
    public boolean isAbsolute() {
        return absolute;
    }

    @Override
    public NodePathSegment getLastSegment() {
        if (this == ROOT) {
            // todo: should this return a constant NodePathSegment instead?
            throw new IllegalStateException("Root path has no path segments!");
        }

        return segments.get(segments.size()-1);
    }

    @Override
    public NodePath getParent() {
        if (this == ROOT) {
            throw new IllegalStateException("Cannot get the parent of ROOT!");
        }

        if (segments.size() == 1) {
            return ROOT;
        }

        // todo: implement this in a more memory-efficient way with Lisp-style list objects
        return new NodePathImpl(segments.subList(0, segments.size()-1), absolute);
    }

    @Override
    public int getSegmentCount() {
        return segments.size();
    }

    @Override
    public NodePathSegment getSegment(final int index) {
        return segments.get(index);
    }

    @Override
    public NodePath subpath(final int beginIndex, final int endIndex) {
        if (beginIndex == 0 && endIndex == 0) {
            return ROOT;
        }

        return new NodePathImpl(segments.subList(beginIndex, endIndex), beginIndex == 0);
    }

    @Override
    public boolean startsWith(final String other) {
        return startsWith(get(other));
    }

    @Override
    public boolean startsWith(final NodePathSegment other) {
        if (other.isRoot()) {
            return true;
        }

        return startsWith(new NodePathImpl(ImmutableList.of(other), false));
    }

    @Override
    public boolean startsWith(final NodePath other) {
        return (other.getSegmentCount() <= segments.size())
                && Iterables.elementsEqual(this, other);
    }

    @Override
    public boolean endsWith(final String other) {
        return endsWith(get(other));
    }

    @Override
    public boolean endsWith(final NodePathSegment other) {
        if (other.isRoot()) {
            return true;
        }

        return endsWith(new NodePathImpl(ImmutableList.of(other), false));
    }

    @Override
    public boolean endsWith(final NodePath other) {
        return (other.getSegmentCount() <= segments.size())
                && Iterables.elementsEqual(
                        segments.subList(segments.size() - other.getSegmentCount(), segments.size()),
                        other);
    }

    @Override
    public NodePath resolve(final String other) {
        return resolve(get(other));
    }

    @Override
    public NodePath resolve(final NodePathSegment other) {
        if (other.isRoot()) {
            return this;
        }

        return resolve(new NodePathImpl(ImmutableList.of(other), false));
    }

    @Override
    public NodePath resolve(final NodePath other) {
        if (other == ROOT) {
            return this;
        }
        if (this == ROOT && other.isAbsolute()) {
            return other;
        }

        return new NodePathImpl(ImmutableList.copyOf(Iterables.concat(segments, other)), absolute);
    }

    @Override
    public NodePath resolveSibling(final String other) {
        // todo: reduce memory churn
        return getParent().resolve(other);
    }

    @Override
    public NodePath resolveSibling(final NodePathSegment other) {
        if (other.isRoot()) {
            return getParent();
        }

        return resolveSibling(new NodePathImpl(ImmutableList.of(other), false));
    }

    @Override
    public NodePath resolveSibling(final NodePath other) {
        // todo: reduce memory churn
        return getParent().resolve(other);
    }

    @Override
    public NodePath relativize(final NodePath other) {
        if (other == ROOT) {
            return new NodePathImpl(segments, false);
        }

        if (other.startsWith(this)) {
            return other.subpath(segments.size(), other.getSegmentCount());
        }

        // generate relative paths up to the root and then work down again
        // todo: remove common ancestors
        return get(Streams.concat(Stream.generate(() -> "..").limit(segments.size()),
                    other.stream().map(NodePathSegment::toString)).collect(joining("/")));
    }

    @Override
    public NodePath toFullyIndexedPath() {
        if (this == ROOT) {
            return this;
        }

        ImmutableList<NodePathSegment> indexedSegments =
                segments.stream().map(NodePathSegment::forceIndex).collect(ImmutableList.toImmutableList());
        return new NodePathImpl(indexedSegments, absolute);
    }

    @Override
    public NodePath toMinimallyIndexedPath() {
        if (this == ROOT) {
            return this;
        }

        ImmutableList<NodePathSegment> indexedSegments =
                segments.stream().map(NodePathSegment::suppressIndex).collect(ImmutableList.toImmutableList());
        return new NodePathImpl(indexedSegments, absolute);
    }

    @Override
    public NodePath toAbsolutePath() {
        if (absolute || this == ROOT) {
            return this;
        }

        return new NodePathImpl(segments, true);
    }

    @Override
    public Iterator<NodePathSegment> iterator() {
        return segments.iterator();
    }

    @Override
    public Stream<NodePathSegment> stream() {
        return segments.stream();
    }

    /**
     * Compare paths by name segment, with unindexed segments considered to be identical to name with index 1.
     */
    @Override
    public int compareTo(final NodePath o) {
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
     * @return true iff all name segments are equal, according to {@link NodePathSegment#equals(Object)}.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        final NodePath other;
        if (obj instanceof String) {
            other = get((String) obj);
        }
        else if (obj instanceof NodePathSegment) {
            other = new NodePathImpl(ImmutableList.of((NodePathSegment) obj), true);
        }
        else if (!(obj instanceof NodePath)) {
            return false;
        }
        else {
            other = (NodePath) obj;
        }
        return Iterables.elementsEqual(segments, other) && (absolute == other.isAbsolute());
    }

    @Override
    public int hashCode() {
        return Objects.hash(segments, absolute);
    }

    @Override
    public String toString() {
        return segments.stream().map(NodePathSegment::toString).collect(joining("/", absolute?"/":"", ""));
    }
}
