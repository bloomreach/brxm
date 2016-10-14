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

package org.onehippo.cms.channelmanager.content.documenttype.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.sort.NodeOrderFieldSorter;
import org.onehippo.cms.channelmanager.content.documenttype.field.sort.FieldSorter;
import org.onehippo.cms.channelmanager.content.documenttype.field.sort.TwoColumnFieldSorter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NamespaceUtils provides utility methods for dealing with repository-based document-type configuration,
 * stored under the /hippo:namespaces root-level node.
 */
public class NamespaceUtils {

    public static final String NODE_TYPE_PATH = HippoNodeType.HIPPOSYSEDIT_NODETYPE + "/" + HippoNodeType.HIPPOSYSEDIT_NODETYPE;
    public static final String EDITOR_CONFIG_PATH = "editor:templates/_default_";

    private static final Logger log = LoggerFactory.getLogger(NamespaceUtils.class);

    private static final String PROPERTY_PLUGIN_CLASS = "plugin.class";
    private static final String LAYOUT_PLUGIN_CLASS_ONE_COLUMN = "org.hippoecm.frontend.service.render.ListViewPlugin";
    private static final String LAYOUT_PLUGIN_CLASS_TWO_COLUMN = "org.hippoecm.frontend.editor.layout.TwoColumn";
    private static final Map<String, FieldSorter> LAYOUT_SORTER;

    static {
        LAYOUT_SORTER = new HashMap<>();

        LAYOUT_SORTER.put(LAYOUT_PLUGIN_CLASS_ONE_COLUMN, new NodeOrderFieldSorter());
        LAYOUT_SORTER.put(LAYOUT_PLUGIN_CLASS_TWO_COLUMN, new TwoColumnFieldSorter());
    }

    /**
     * Retrieve the root node of a document type's definition in the repository.
     *
     * @param typeId  ID of a document type, e.g. "myhippoproject:newsdocument"
     * @param session system JCR session meant for read-only access
     * @return        document type root node or nothing, wrapped in an Optional
     */
    public static Optional<Node> getDocumentTypeRootNode(final String typeId, final Session session) {
        try {
            final String[] part = typeId.split(":");
            if (part.length == 2) {
                final String path = "/hippo:namespaces/" + part[0] + "/" + part[1];
                return Optional.of(session.getNode(path));
            }
        } catch (RepositoryException e) {
            log.debug("Unable to find root node for document type '{}'", typeId, e);
        }
        return Optional.empty();
    }

    /**
     * Retrieve the (CMS) plugin class in use for a specific field.
     *
     * @param editorFieldNode JCR node representing an editor field (or group of fields)
     * @return                the plugin class name or nothing, wrapped in an Optional
     */
    public static Optional<String> getPluginClassForField(final Node editorFieldNode) {
        try {
            if (editorFieldNode.hasProperty(PROPERTY_PLUGIN_CLASS)) {
                return Optional.of(editorFieldNode.getProperty(PROPERTY_PLUGIN_CLASS).getString());
            }
        } catch (RepositoryException e) {
            log.warn("Failed to read property '{}' for field {}", PROPERTY_PLUGIN_CLASS,
                    JcrUtils.getNodePathQuietly(editorFieldNode), e);
        }
        return Optional.empty();
    }

    /**
     * Retrieve a sorter for the fields of a content type.
     *
     * @param contentTypeRootNode JCR node representing the root of a content type definition
     * @return                    Appropriate sorter or nothing, wrapped in an Optional
     */
    public static Optional<FieldSorter> retrieveFieldSorter(final Node contentTypeRootNode) {
        try {
            if (contentTypeRootNode.hasNode(EDITOR_CONFIG_PATH)) {
                final Node editorConfigNode = contentTypeRootNode.getNode(EDITOR_CONFIG_PATH);
                if (editorConfigNode.hasNode("root")) {
                    final Node layoutNode = editorConfigNode.getNode("root");
                    final Optional<String> optionalPluginClass = getPluginClassForField(layoutNode);

                    if (optionalPluginClass.isPresent()) {
                        String pluginClass = optionalPluginClass.get();
                        return Optional.of(LAYOUT_SORTER.containsKey(pluginClass)
                                ? LAYOUT_SORTER.get(pluginClass)
                                : LAYOUT_SORTER.get(LAYOUT_PLUGIN_CLASS_ONE_COLUMN));
                    }
                }
                return Optional.of(LAYOUT_SORTER.get(LAYOUT_PLUGIN_CLASS_ONE_COLUMN));
            }
        } catch (RepositoryException e) {
            log.warn("Failed to determine layout of content type {}",
                    JcrUtils.getNodePathQuietly(contentTypeRootNode), e);
        }
        return Optional.empty();
    }
}
