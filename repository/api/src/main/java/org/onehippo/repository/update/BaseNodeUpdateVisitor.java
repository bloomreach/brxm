/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;

/**
 * Base {@link NodeUpdateVisitor} class adding support for logging.
 */
public abstract class BaseNodeUpdateVisitor implements NodeUpdateVisitor {

    protected Logger log;
    protected Map<String, Object> parametersMap;
    protected NodeUpdateVisitorContext visitorContext;

    public void setLogger(Logger log) {
        this.log = log;
    }

    public void setParametersMap(Map<String, Object> parametersMap) {
        this.parametersMap = parametersMap;
    }

    public void setVisitorContext(NodeUpdateVisitorContext visitorContext) {
        this.visitorContext = visitorContext;
    }

    /**
     * Overridable boolean function to indicate if skipped node paths should be logged (default true)
     * @return true if skipped node paths should be logged
     */
    public boolean logSkippedNodePaths() {
        return true;
    }

    /**
     * Overridable boolean function to indicate if node checkout can be skipped (default false)
     * @return true if node checkout can be skipped (e.g. for readonly visitors and/or updates unrelated to versioned content)
     */
    public boolean skipCheckoutNodes() {
        return false;
    }

    @Override
    public void initialize(Session session) throws RepositoryException {
    }

    /**
     * Initiates the retrieval of the nodes when using custom, instead of path or xpath (query) based, node
     * selection/navigation, returning the first node to visit. Intended to be overridden, default implementation returns null.
     * @param session
     * @return first node to visit, or null if none found
     * @throws RepositoryException
     */
    public Node firstNode(final Session session) throws RepositoryException {
        return null;
    }

    /**
     * Return a following node, when using custom, instead of path or xpath (query) based, node selection/navigation.
     * Intended to be overridden, default implementation returns null.
     * @return next node to visit, or null if none left
     * @throws RepositoryException
     */
    public Node nextNode() throws RepositoryException {
        return null;
    }

    @Override
    public void destroy() {
    }

}
