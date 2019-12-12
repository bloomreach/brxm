/*
 *  Copyright 2015-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.usagestatistics.events;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.usagestatistics.UsageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeUsageEvent extends UsageEvent {

    public static final Logger log = LoggerFactory.getLogger(NodeUsageEvent.class);

    private static final String EVENT_PARAM_NODE_ID = "node";
    private final IdentifierStrategy strategy;

    public NodeUsageEvent(final String name, final IModel<Node> nodeModel, IdentifierStrategy strategy) {
        super(name);
        this.strategy = strategy;
        final Node node = nodeModel.getObject();
        if (node != null) {
            try {
                getNodeIdentifier(node).ifPresent(id -> setParameter(EVENT_PARAM_NODE_ID, id));
            } catch (RepositoryException e) {
                log.warn("Error retrieving node identifier", e);
            }
        }
    }

    public NodeUsageEvent(final String name, final IModel<Node> nodeModel) {
        this(name, nodeModel, node -> Optional.of(node.getIdentifier()));
    }

    protected Optional<String> getNodeIdentifier(final Node node) throws RepositoryException {
        return strategy.getIdentifier(node);
    }
}
