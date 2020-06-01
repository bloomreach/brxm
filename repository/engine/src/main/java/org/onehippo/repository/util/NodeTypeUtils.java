/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.util;

import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.hippoecm.repository.jackrabbit.HippoCompactNodeTypeDefReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class NodeTypeUtils {

    private NodeTypeUtils() {}

    public static Logger log = LoggerFactory.getLogger(NodeTypeUtils.class);

    public static void initializeNodeTypes(Session session, InputStream cndStream, String cndName) throws RepositoryException {
        try {
            log.debug("Initializing nodetypes from {} ", cndName);
            final NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
            final CompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping> cndReader =
                    new HippoCompactNodeTypeDefReader(new InputStreamReader(cndStream), cndName, namespaceRegistry);
            final List<QNodeTypeDefinition> ntdList = cndReader.getNodeTypeDefinitions();
            final NodeTypeRegistry nodeTypeRegistry = ((NodeTypeManagerImpl) session.getWorkspace().getNodeTypeManager()).getNodeTypeRegistry();

            for (QNodeTypeDefinition ntd : ntdList) {
                try {
                    if (!nodeTypeRegistry.isRegistered(ntd.getName())) {
                        log.debug("Registering node type {}", ntd.getName());
                        nodeTypeRegistry.registerNodeType(ntd);
                    } else {
                        log.debug("Replacing node type {}", ntd.getName());
                        nodeTypeRegistry.reregisterNodeType(ntd);
                    }
                } catch (InvalidNodeTypeDefException e) {
                    throw new RepositoryException("Invalid node type definition for node type " + ntd.getName(), e);
                }
            }
        } catch (ParseException e) {
            throw new RepositoryException("Failed to parse cnd " + cndName, e);
        }
    }
}
