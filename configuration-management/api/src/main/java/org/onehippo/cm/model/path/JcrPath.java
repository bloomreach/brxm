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
package org.onehippo.cm.model.path;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Represents a multi-segment JCR node path with possible same-named-sibling indices on each segment.
 * Note that this is essentially an analogy to the {@link java.nio.file.Path} API, and instances can be used in a way
 * familiar to users of that class. Note that this class deliberately does not handle the full JCR spec for node
 * references, and in particular does not support UUID-based node references. Note also that the current implementation
 * does not fully respect the differences between relative and absolute paths, treating all paths as absolute.
 */
public interface JcrPath extends Comparable<JcrPath>, Iterable<JcrPathSegment> {

    /**
     * @return a constant value representing the path of the JCR root node
     */
    JcrPath getRoot();

    /**
     * @return true iff this instance represents the path of the JCR root node
     */
    boolean isRoot();

    /**
     * @return true if this path is an absolute path, represented in String form with a leading "/"
     */
    boolean isAbsolute();

    /**
     * @return the final segment of this {@link JcrPath}, which may represent a JCR Node or Property
     */
    JcrPathSegment getLastSegment();

    /**
     * @return a path representing the parent node of this path
     * @throws IllegalStateException iff {@link #isRoot()}
     */
    JcrPath getParent();

    /**
     * @return the number of separate path segments are composed in this path
     */
    int getSegmentCount();

    /**
     * @param index the zero-based index of the desired path segment
     * @return a {@link JcrPathSegment} representing the segment of this path at the index position
     */
    JcrPathSegment getSegment(final int index);

    /**
     * @param beginIndex the zero-based index (inclusive) of the starting segment of the desired subpath
     * @param endIndex the zero-based index (exclusive) of the ending segment of the desired subpath
     * @return a path representing a specific portion of this path
     */
    JcrPath subpath(final int beginIndex, final int endIndex);

    /**
     * @see Path#startsWith(String)
     */
    boolean startsWith(final String other);

    /**
     * @see Path#startsWith(String)
     */
    boolean startsWith(final JcrPathSegment other);

    /**
     * @see Path#startsWith(Path)
     */
    boolean startsWith(final JcrPath other);

    /**
     * @see Path#endsWith(String)
     */
    boolean endsWith(final String other);

    /**
     * @see Path#endsWith(String)
     */
    boolean endsWith(final JcrPathSegment other);
    /**
     * @see Path#endsWith(Path)
     */
    boolean endsWith(final JcrPath other);

    /**
     * @see Path#resolve(String)
     */
    JcrPath resolve(final String other);

    /**
     * @see Path#resolve(String)
     */
    JcrPath resolve(final JcrPathSegment other);

    /**
     * @see Path#resolve(Path)
     */
    JcrPath resolve(final JcrPath other);

    /**
     * @see Path#resolveSibling(String)
     */
    JcrPath resolveSibling(final String other);

    /**
     * @see Path#resolveSibling(String)
     */
    JcrPath resolveSibling(final JcrPathSegment other);

    /**
     * @see Path#resolveSibling(Path)
     */
    JcrPath resolveSibling(final JcrPath other);

    /**
     * @see Path#relativize(Path)
     */
    JcrPath relativize(final JcrPath other);

    // todo: normalize

    /**
     * @return if {@link #isRoot()} or {@link #isAbsolute()}, this; otherwise, a new variant of this path for which
     * {@link #isAbsolute()} == true
     */
    JcrPath toAbsolutePath();

    /**
     * @return a {@link JcrPath} equivalent to this one, except that each segment has had forceIndex() applied to it
     */
    // TODO: remove this
    JcrPath toFullyIndexedPath();

    /**
     * @return a {@link JcrPath} equivalent to this one, except that each segment has had suppressIndex() applied to it
     */
    // TODO: rename this?
    JcrPath toMinimallyIndexedPath();

    /**
     * @return an ordered, serial stream of {@link JcrPathSegment}s in the same order used by {@link #getSegment(int)}
     */
    Stream<JcrPathSegment> stream();

    /**
     * @return an ordered, iterator of {@link JcrPathSegment}s in the same order used by {@link #getSegment(int)}
     */
    @Override
    Iterator<JcrPathSegment> iterator();

    /**
     * @return an array of JcrPathSegment in the same order used by {@link #getSegment(int)}
     */
    JcrPathSegment[] toArray();

    /**
     * Compare paths by name segment, with unindexed segments considered to be identical to name with index 1.
     */
    @Override
    int compareTo(final JcrPath o);

    /**
     * @return true iff all name segments are equal, according to {@link JcrPathSegment#equals(Object)}.
     */
    @Override
    boolean equals(final Object obj);
}
