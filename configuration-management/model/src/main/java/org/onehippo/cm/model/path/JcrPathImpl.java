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

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;

import static java.util.stream.Collectors.joining;

/**
 * Represents a multi-segment JCR node path with possible same-named-sibling indices on each segment.
 * Note that this is essentially an analogy to the {@link java.nio.file.Path} API, and instances can be used in a way
 * familiar to users of that class.
 */
class JcrPathImpl implements JcrPath {

    private final ImmutableList<org.onehippo.cm.model.path.JcrPathSegment> segments;
    private final boolean absolute;

    // private to guarantee ROOT is a constant
    JcrPathImpl(final ImmutableList<org.onehippo.cm.model.path.JcrPathSegment> segments, final boolean absolute) {
        this(segments, absolute, false);
    }

    // private to guarantee ROOT is a constant
    JcrPathImpl(final ImmutableList<org.onehippo.cm.model.path.JcrPathSegment> segments, final boolean absolute, final boolean isRoot) {
        // this check is mainly for internal purposes, to make sure we're not accidentally failing to return ROOT
        if (!isRoot && segments.size() == 0) {
            throw new IllegalArgumentException("Should use ROOT instead of constructing new NodePath with no segments!");
        }

        this.segments = segments;

        // todo: debug this -- for now, assume a leading slash (i.e. an absolute path)
        this.absolute = true;
    }

    @Override
    public org.onehippo.cm.model.path.JcrPath getRoot() {
        return JcrPaths.ROOT;
    }

    @Override
    public boolean isRoot() {
        // default return value is false, but ROOT overrides this to return true
        return false;
    }

    @Override
    public boolean isAbsolute() {
        return absolute;
    }

    @Override
    public org.onehippo.cm.model.path.JcrPathSegment getLastSegment() {
        if (isRoot()) {
            // return a constant NodePathSegment for the root path name
            return JcrPaths.ROOT_NAME;
        }

        return segments.get(segments.size()-1);
    }

    @Override
    public org.onehippo.cm.model.path.JcrPath getParent() {
        if (isRoot()) {
            throw new IllegalStateException("Cannot get the parent of ROOT!");
        }

        if (segments.size() == 1) {
            return getRoot();
        }

        // todo: implement this in a more memory-efficient way with Lisp-style list objects
        return new JcrPathImpl(segments.subList(0, segments.size()-1), absolute);
    }

    @Override
    public int getSegmentCount() {
        return segments.size();
    }

    @Override
    public org.onehippo.cm.model.path.JcrPathSegment getSegment(final int index) {
        return segments.get(index);
    }

    @Override
    public org.onehippo.cm.model.path.JcrPath subpath(final int beginIndex) {
        if (beginIndex == 0) {
            return this;
        }

        if (beginIndex > getSegmentCount()) {
            throw new IllegalArgumentException("beginIndex must be less than getSegmentCount()!");
        }

        return new JcrPathImpl(segments.subList(beginIndex, getSegmentCount()), false);
    }


    @Override
    public org.onehippo.cm.model.path.JcrPath subpath(final int beginIndex, final int endIndex) {
        if (beginIndex == 0 && endIndex == 0) {
            return getRoot();
        }

        return new JcrPathImpl(segments.subList(beginIndex, endIndex), beginIndex == 0 && absolute);
    }

    @Override
    public boolean startsWith(final String other) {
        return startsWith(JcrPaths.getPath(other));
    }

    @Override
    public boolean startsWith(final org.onehippo.cm.model.path.JcrPathSegment other) {
        if (other.isRoot()) {
            return true;
        }

        return getSegment(0).equals(other);
    }

    @Override
    public boolean startsWith(final org.onehippo.cm.model.path.JcrPath other) {
        if (other.isRoot()) {
            return true;
        }

        return (other.getSegmentCount() <= segments.size())
                && Iterables.elementsEqual(segments.subList(0, other.getSegmentCount()), other);
    }

    @Override
    public boolean endsWith(final String other) {
        return endsWith(JcrPaths.getPath(other));
    }

    @Override
    public boolean endsWith(final org.onehippo.cm.model.path.JcrPathSegment other) {
        if (other.isRoot()) {
            return true;
        }

        return getLastSegment().equals(other);
    }

    @Override
    public boolean endsWith(final org.onehippo.cm.model.path.JcrPath other) {
        if (other.isRoot()) {
            return true;
        }

        return (other.getSegmentCount() <= segments.size())
                && Iterables.elementsEqual(
                segments.subList(segments.size() - other.getSegmentCount(), segments.size()),
                other);
    }

    @Override
    public org.onehippo.cm.model.path.JcrPath resolve(final String other) {
        return resolve(JcrPaths.getPath(other));
    }

    @Override
    public org.onehippo.cm.model.path.JcrPath resolve(final org.onehippo.cm.model.path.JcrPathSegment other) {
        if (other.isRoot()) {
            return this;
        }

        return resolve(new JcrPathImpl(ImmutableList.of(other), false));
    }

    @Override
    public org.onehippo.cm.model.path.JcrPath resolve(final org.onehippo.cm.model.path.JcrPath other) {
        if (other.isRoot()) {
            return this;
        }
        if (isRoot() && other.isAbsolute()) {
            return other;
        }

        return new JcrPathImpl(ImmutableList.copyOf(Iterables.concat(segments, other)), absolute);
    }

    @Override
    public org.onehippo.cm.model.path.JcrPath resolveSibling(final String other) {
        // todo: reduce memory churn
        return getParent().resolve(other);
    }

    @Override
    public org.onehippo.cm.model.path.JcrPath resolveSibling(final org.onehippo.cm.model.path.JcrPathSegment other) {
        if (other.isRoot()) {
            return getParent();
        }

        return resolveSibling(new JcrPathImpl(ImmutableList.of(other), false));
    }

    @Override
    public org.onehippo.cm.model.path.JcrPath resolveSibling(final org.onehippo.cm.model.path.JcrPath other) {
        // todo: reduce memory churn
        return getParent().resolve(other);
    }

    @Override
    public org.onehippo.cm.model.path.JcrPath relativize(final org.onehippo.cm.model.path.JcrPath other) {
        if (other.isRoot()) {
            return new JcrPathImpl(segments, false);
        }

        if (other.startsWith(this)) {
            return other.subpath(segments.size(), other.getSegmentCount());
        }

        // generate relative paths up to the root and then work down again
        // todo: remove common ancestors
        return JcrPaths.getPath(Streams.concat(Stream.generate(() -> "..").limit(segments.size()),
                other.stream().map(org.onehippo.cm.model.path.JcrPathSegment::toString)).collect(joining("/")));
    }

    // todo: normalize

    @Override
    public org.onehippo.cm.model.path.JcrPath toAbsolutePath() {
        if (absolute || isRoot()) {
            return this;
        }

        return new JcrPathImpl(segments, true);
    }

    @Override
    public org.onehippo.cm.model.path.JcrPath forceIndices() {
        if (isRoot()) {
            return this;
        }

        ImmutableList<org.onehippo.cm.model.path.JcrPathSegment> indexedSegments =
                segments.stream().map(org.onehippo.cm.model.path.JcrPathSegment::forceIndex).collect(ImmutableList.toImmutableList());
        return new JcrPathImpl(indexedSegments, absolute);
    }

    @Override
    public JcrPathImpl suppressIndices() {
        if (isRoot()) {
            return this;
        }

        ImmutableList<org.onehippo.cm.model.path.JcrPathSegment> indexedSegments =
                segments.stream().map(org.onehippo.cm.model.path.JcrPathSegment::suppressIndex).collect(ImmutableList.toImmutableList());
        return new JcrPathImpl(indexedSegments, absolute);
    }

    @Override
    public Stream<org.onehippo.cm.model.path.JcrPathSegment> stream() {
        return segments.stream();
    }

    @Override
    public Iterator<org.onehippo.cm.model.path.JcrPathSegment> iterator() {
        return segments.iterator();
    }

    @Override
    public org.onehippo.cm.model.path.JcrPathSegment[] toArray() {
        return segments.toArray(new org.onehippo.cm.model.path.JcrPathSegment[segments.size()]);
    }

    @Override
    public int compareTo(final org.onehippo.cm.model.path.JcrPath o) {
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

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        final org.onehippo.cm.model.path.JcrPath other;
        if (obj instanceof String) {
            other = JcrPaths.getPath((String) obj);
        }
        else if (obj instanceof org.onehippo.cm.model.path.JcrPathSegment) {
            other = new JcrPathImpl(ImmutableList.of((org.onehippo.cm.model.path.JcrPathSegment)obj), true);
        }
        else if (!(obj instanceof org.onehippo.cm.model.path.JcrPath)) {
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
