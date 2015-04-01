/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The possible states of a document type.
 */
enum TypeState {

    UNKNOWN, NEW, LIVE, CHANGED;

    private static final Logger log = LoggerFactory.getLogger(TypeState.class);

    static TypeState getState(Node node) {
        try {
            if (node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                return determineState(node);
            }
        } catch (RepositoryException e) {
            log.info("Unable to determine document type state of node '{}'", JcrUtils.getNodePathQuietly(node), e);
        }
        return UNKNOWN;
    }

    private static TypeState determineState(final Node template) throws RepositoryException {
        final String prefix = template.getParent().getName();
        final NamespaceRegistry nsReg = template.getSession().getWorkspace().getNamespaceRegistry();
        final String currentUri = nsReg.getURI(prefix);

        final Node ntHandle = template.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
        final NodeIterator variants = ntHandle.getNodes(HippoNodeType.HIPPOSYSEDIT_NODETYPE);

        Node current = null;
        Node draft = null;
        while (variants.hasNext()) {
            final Node variant = variants.nextNode();
            if (variant.isNodeType(HippoNodeType.NT_REMODEL)) {
                final String uri = variant.getProperty(HippoNodeType.HIPPO_URI).getString();
                if (currentUri.equals(uri)) {
                    current = variant;
                }
            } else {
                draft = variant;
            }
        }

        if (current == null) {
            return draft != null ? NEW : UNKNOWN;
        } else {
            return draft == null ? LIVE : CHANGED;
        }
    }

}
