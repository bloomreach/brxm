/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.usagestatistics.events;


import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.usagestatistics.UsageEvent;

/**
 * Strategy for determining the identifier used in a {@link UsageEvent}
 */
public interface IdentifierStrategy {

    /**
     * Determines the identifier to be used in a {@link UsageEvent}. This can
     * be the identifier of the node itself or anything that uniquely identifies
     * the node or any of its ancestors.
     *
     * @param node {@link Node}
     * @return The identifier of the node ( not necessarily the {@link Node#getIdentifier()} )
     * @throws RepositoryException if an error occurs
     */
    String getIdentifier(Node node) throws RepositoryException;
}
