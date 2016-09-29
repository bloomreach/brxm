/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.util;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms.channelmanager.content.exception.DocumentTypeNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NamespaceUtils provides utility methods for dealing with repository-based document-type configuration,
 * stored under the /hippo:namespaces root-level node.
 */
public class NamespaceUtils {
    private static final Logger log = LoggerFactory.getLogger(NamespaceUtils.class);

    /**
     * Retrieve the root node of a document type's definition in the repository.
     *
     * @param typeId  ID of a document type, e.g. "myhippoproject:newsdocument"
     * @param session system JCR session meant for read-only access
     * @return        document type root node
     * @throws DocumentTypeNotFoundException
     *                Not finding the root node means we abort the document type reading.
     */
    public static Node getDocumentTypeRootNode(final String typeId, final Session session)
            throws DocumentTypeNotFoundException {
        try {
            String[] part = typeId.split(":");
            String path = "/hippo:namespaces/" + part[0] + "/" + part[1];

            return session.getNode(path);
        } catch (RepositoryException e) {
            log.debug("Unable to find root node for document type '{}'", typeId, e);
            throw new DocumentTypeNotFoundException();
        }
    }


    /**
     * Given a node or property name (e.g. "myhippoproject:title"), retrieve the corresponding "config node"
     * under "editor:templates".
     *
     * @param documentTypeRootNode the root node of a document type
     * @param fieldId              node or property name
     * @return                     the config node, or null
     */
    public static Node getConfigForField(final Node documentTypeRootNode, final String fieldId) {
        try {
            final String nodeTypePath = HippoNodeType.HIPPOSYSEDIT_NODETYPE + "/" + HippoNodeType.HIPPOSYSEDIT_NODETYPE;
            final Node nodeTypeNode = documentTypeRootNode.getNode(nodeTypePath);
            final NodeIterator children = nodeTypeNode.getNodes();
            while (children.hasNext()) {
                final Node child = children.nextNode();
                if (child.hasProperty(HippoNodeType.HIPPO_PATH)
                        && child.getProperty(HippoNodeType.HIPPO_PATH).getString().equals(fieldId)) {
                    final String configPath = "editor:templates/_default_/" + child.getName();
                    return documentTypeRootNode.getNode(configPath);
                }
            }
        } catch (RepositoryException e) {
            // unable to retrieve the config node
        }
        return null;
    }

    /**
     * Retrieve the (CMS) plugin class in use for a specific field.
     *
     * @param documentTypeRootNode the root node of a document type
     * @param fieldId              node or property name
     * @return                     the plugin class name or null
     */
    public static String getPluginClassForField(final Node documentTypeRootNode, final String fieldId) {
        final Node fieldConfig = getConfigForField(documentTypeRootNode, fieldId);
        if (fieldConfig != null) {
            try {
                return fieldConfig.getProperty("plugin.class").getString();
            } catch (RepositoryException e) {
                // failed to read plugin config
            }
        }
        return null;
    }
}
