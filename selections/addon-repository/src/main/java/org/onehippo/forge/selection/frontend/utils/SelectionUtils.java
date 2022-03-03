/*
 * Copyright 2011-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.selection.frontend.utils;

import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.ItemModelWrapper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.model.properties.JcrMultiPropertyValueModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils
 */
public final class SelectionUtils {

    private static final Logger log = LoggerFactory.getLogger(SelectionUtils.class);

    private SelectionUtils() {
    }

    /**
     * Get a node from a JCR model
     * @param model model representing JCR node
     * @return Node from model
     */
    public static Node getNode(Object model) {
        Object node = null;
        if (model instanceof JcrMultiPropertyValueModel) {
            node = ((JcrMultiPropertyValueModel) model).getItemModel().getParentModel().getObject();
        }
        else if (model instanceof JcrPropertyValueModel) {
            node = ((JcrPropertyValueModel) model).getJcrPropertymodel().getItemModel().getParentModel().getObject();
        }
        else if (model instanceof JcrNodeModel) {
            node = ((JcrNodeModel) model).getNode();
        }
        else if (model instanceof NodeModelWrapper) {
            node = ((NodeModelWrapper) model).getNodeModel().getObject();
        }
        else if (model instanceof ItemModelWrapper) {
            node = ((ItemModelWrapper) model).getItemModel().getParentModel().getObject();
        }
        return node instanceof Node ? (Node) node : null;
    }

    /**
     * Get the locale of a node representing a translated document, or a compound therein
     * @param node Node
     * @return Locale (nullable)
     */
    public static Locale getLocale(Node node) {
        if (node == null) {
            return null;
        }

        Locale locale = null;
        try {

            // in case of compounds (that should not be translated), move up the tree
            Node docNode = node;
            while (!docNode.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {

                docNode = docNode.getParent();

                // stop at handle level or at the repository root in case you're looking at a prototype
                if (docNode.isNodeType(HippoNodeType.NT_HANDLE) || docNode.isNodeType("rep:root")) {
                    break;
                }
            }

            if (!docNode.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                log.debug("No translated nodes found for node '{}' and below", docNode.getPath());
                return null;
            }

            final String property = docNode.getProperty(HippoTranslationNodeType.LOCALE).getString();
            if (property.isEmpty()) {
                log.debug("Property '{}' is empty for node '{}'", HippoTranslationNodeType.LOCALE, docNode.getPath());
                return null;
            }

            // create locale
            final String[] parts = property.split("_");
            if (parts.length == 1) {
                locale = new Locale(parts[0]);
            }
            else if (parts.length == 2) {
                locale = new Locale(parts[0], parts[1]);
            }
            else if (parts.length == 3) {
                locale = new Locale(parts[0], parts[1], parts[2]);
            }
        } catch (RepositoryException e) {
            log.warn(e.getMessage(), e);
        }
        return locale;
    }

    /**
     * Concatenates a new path segment to a path and makes sure a slash is inserted when necessary.
     * @param basePath the base path
     * @param newPathSegment the segment to be added to a path
     * @return concatenated path
     */
    public static String ensureSlashes(final String basePath, final String newPathSegment) {
        StringBuilder sb = new StringBuilder(basePath);
        if(basePath != null && !basePath.endsWith("/")) {
            sb.append("/");
        }
        sb.append(newPathSegment);
        return sb.toString();
    }
}
