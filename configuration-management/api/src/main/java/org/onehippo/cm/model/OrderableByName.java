/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Set;

/**
 * Implementors of this class provide an explicit partial-ordering of their instances by specifying a name for
 * themselves and a list of names of other items which must proceed them. This can then be used to produce a
 * topological sorting of instances. It is expected that only "compatible" instances may be sorted together in this
 * way, as defined by type compatibility with a specific implementation of this interface.
 */
public interface OrderableByName {

    /**
     * @return the name of this instance, which can be used by other instances in their {@link #getAfter()}
     */
    String getName();

    /**
     * @return The <strong>ordered</strong> immutable set of {@link String}s after which
     * this {@link OrderableByName} instance should be sorted.
     */
    Set<String> getAfter();

}
