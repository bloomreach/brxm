/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Interface for iterating through a set of nodes to match. Used currently in org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils#prototypes
 *
 * @version "$Id$"
 */
public interface JcrMatcher {

    /**
     * Does the node match according to the rules which it implements
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    boolean matches(final Node node) throws RepositoryException;
}
