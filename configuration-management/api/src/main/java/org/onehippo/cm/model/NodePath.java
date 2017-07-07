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

import java.util.stream.Stream;

/**
 * Represents a multi-segment JCR node path with possible same-named-sibling indices on each segment.
 */
public interface NodePath extends Comparable<NodePath>, Iterable<NodePathSegment> {
    boolean isAbsolute();

    NodePathSegment getLastSegment();

    NodePath getParent();

    int getSegmentCount();

    NodePathSegment getSegment(int index);

    NodePath subpath(int beginIndex, int endIndex);

    boolean startsWith(String other);

    boolean startsWith(NodePath other);

    boolean endsWith(String other);

    boolean endsWith(NodePath other);

    NodePath resolve(String other);

    NodePath resolve(NodePath other);

    NodePath resolveSibling(String other);

    NodePath resolveSibling(NodePath other);

    NodePath relativize(NodePath other);

    // todo: normalize

    NodePath toAbsolutePath();

    Stream<NodePathSegment> stream();
}
