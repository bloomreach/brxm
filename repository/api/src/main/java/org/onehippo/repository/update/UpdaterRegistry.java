/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.update;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.services.SingletonService;

@SingletonService
public interface UpdaterRegistry {

    /**
     * Get the list of updaters that are registered for this node.
     *
     * @param node  the node to get the updaters for
     * @return  the list classes of the updaters, empty list if no updaters for this node.
     * @throws RepositoryException
     */
    List<Class<? extends NodeUpdateVisitor>> getUpdaters(final Node node) throws RepositoryException;

}
