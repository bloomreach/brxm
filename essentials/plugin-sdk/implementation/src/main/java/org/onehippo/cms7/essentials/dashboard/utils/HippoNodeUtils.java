/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * @version "$Id: HippoNodeUtils.java 169724 2013-07-05 08:32:08Z dvandiepen $"
 */
public final class HippoNodeUtils {
    public static final String HIPPOSYSEDIT_PATH = HippoNodeType.HIPPO_PATH;
    private static final String HIPPOSYSEDIT_SUPERTYPE = "hipposysedit:supertype";

    private HippoNodeUtils() {
    }

    static void setSupertype(final Node namespaceNode, final String... values) throws RepositoryException {
        Node node = getSupertypeNode(namespaceNode);
        node.setProperty(HIPPOSYSEDIT_SUPERTYPE, values);
    }

    static void setUri(final Node namespaceNode, final String uri) throws RepositoryException {
        Node node = getSupertypeNode(namespaceNode);
        node.setProperty("hipposysedit:uri", uri);
    }

    static void setNodeType(final Node namespaceNode, final String value) throws RepositoryException {
        Node node = getPrototypeNode(namespaceNode);
        node.setPrimaryType(value);
    }

    public static String getStringProperty(final Node node, final String property) throws RepositoryException {
        if (node.hasProperty(property)) {
            return node.getProperty(property).getString();
        } else {
            return null;
        }
    }

    public static String getStringProperty(final Node node, final String property, final String defaultValue) throws RepositoryException {
        if (node.hasProperty(property)) {
            return node.getProperty(property).getString();
        } else {
            return defaultValue;
        }
    }

    public static boolean getBooleanProperty(final Node node, final String property) throws RepositoryException {
        return node.hasProperty(property) && node.getProperty(property).getBoolean();
    }

    public static Long getLongProperty(final Node node, final String property, final Long defaultValue) throws RepositoryException {
        return node.hasProperty(property) ? node.getProperty(property).getLong() : defaultValue;
    }

    public static double getDoubleProperty(final Node node, final String property, final double defaultValue) throws RepositoryException {
        return node.hasProperty(property) ? node.getProperty(property).getDouble() : defaultValue;
    }

    public static Node getNode(final Session session, final String path) throws RepositoryException {
        if (session.nodeExists(path)) {
            return session.getNode(path);
        }
        return null;
    }

    /**
     * Retrieve the namespace prefix from a prefixed node type. This method will return null when the type is not
     * prefixed.
     *
     * @param type the namespace type, e.g. 'hippo:type'
     * @return the namespace prefix or null, e.g. 'hippo'
     */
    public static String getPrefixFromType(final String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        final int i = type.indexOf(':');
        if (i < 0) {
            return null;
        }
        return type.substring(0, i);
    }

    private static Node getPrototypeNode(final Node namespaceNode) throws RepositoryException {
        return namespaceNode.getNode("hipposysedit:prototypes").getNode(EssentialConst.HIPPOSYSEDIT_PROTOTYPE);
    }

    private static Node getSupertypeNode(final Node namespaceNode) throws RepositoryException {
        return namespaceNode.getNode(EssentialConst.HIPPOSYSEDIT_NODETYPE).getNode(EssentialConst.HIPPOSYSEDIT_NODETYPE);
    }
}
