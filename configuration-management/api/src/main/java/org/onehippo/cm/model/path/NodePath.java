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

import java.util.stream.Stream;
import java.nio.file.Path;

/**
 * Represents a multi-segment JCR node path with possible same-named-sibling indices on each segment.
 * Note that this is essentially an analogy to the {@link java.nio.file.Path} API, and instances can be used in a way
 * familiar to users of that class.
 */
public interface NodePath extends Comparable<NodePath>, Iterable<NodePathSegment> {

    /**
     * @return a constant value representing the path of the JCR root node
     */
    NodePath getRoot();

    /**
     * @return true iff this instance represents the path of the JCR root node
     */
    boolean isRoot();

    /**
     * @return true if this path is an absolute path, represented in String form with a leading "/"
     */
    boolean isAbsolute();

    /**
     * @return the final segment of this {@link NodePath}, which may represent a JCR Node or Property
     */
    NodePathSegment getLastSegment();

    /**
     * @return a path representing the parent node of this path
     * @throws IllegalStateException iff {@link #isRoot()}
     */
    NodePath getParent();

    /**
     * @return the number of separate path segments are composed in this path
     */
    int getSegmentCount();

    /**
     * @param index the zero-based index of the desired path segment
     * @return a {@link NodePathSegment} representing the segment of this path at the index position
     */
    NodePathSegment getSegment(int index);

    /**
     * @param beginIndex the zero-based index (inclusive) of the starting segment of the desired subpath
     * @param endIndex the zero-based index (exclusive) of the ending segment of the desired subpath
     * @return a path representing a specific portion of this path
     */
    NodePath subpath(int beginIndex, int endIndex);

    /**
     * @see Path#startsWith(String)
     */
    boolean startsWith(String other);

    /**
     * @see Path#startsWith(String)
     */
    boolean startsWith(NodePathSegment other);

    /**
     * @see Path#startsWith(Path)
     */
    boolean startsWith(NodePath other);

    /**
     * @see Path#endsWith(String)
     */
    boolean endsWith(String other);

    /**
     * @see Path#endsWith(String)
     */
    boolean endsWith(NodePathSegment other);

    /**
     * @see Path#endsWith(Path)
     */
    boolean endsWith(NodePath other);

    /**
     * @see Path#resolve(String)
     */
    NodePath resolve(String other);

    /**
     * @see Path#resolve(String)
     */
    NodePath resolve(NodePathSegment other);

    /**
     * @see Path#resolve(Path)
     */
    NodePath resolve(NodePath other);

    /**
     * @see Path#resolveSibling(String)
     */
    NodePath resolveSibling(String other);

    /**
     * @see Path#resolveSibling(String)
     */
    NodePath resolveSibling(NodePathSegment other);

    /**
     * @see Path#resolveSibling(Path)
     */
    NodePath resolveSibling(NodePath other);

    /**
     * @see Path#relativize(Path)
     */
    NodePath relativize(NodePath other);

    // todo: normalize

    /**
     * @return if {@link #isRoot()} or {@link #isAbsolute()}, this; otherwise, a new variant of this path for which
     * {@link #isAbsolute()} == true
     */
    NodePath toAbsolutePath();

    /**
     * @return a NodePath equivalent to this one, except that each segment has had forceIndex() applied to it
     */
    NodePath toFullyIndexedPath();

    /**
     * @return a NodePath equivalent to this one, except that each segment has had suppressIndex() applied to it
     */
    NodePath toMinimallyIndexedPath();

    /**
     * @return an ordered, serial stream of NodePathSegments in the same order used by {@link #getSegment(int)}
     */
    Stream<NodePathSegment> stream();
}
