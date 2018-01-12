/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

import com.google.common.base.Strings;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.essentials.plugin.sdk.service.JcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: CndUtils.java 169509 2013-07-03 12:23:40Z dvandiepen $"
 */
public final class CndUtils {


    private static Logger log = LoggerFactory.getLogger(CndUtils.class);

    private CndUtils() {
    }

    /**
     * Register a new namespace in the repository.
     *
     * @param prefix  the prefix of the new namespace
     * @param uri     the URI of the new namespace
     * @throws RepositoryException when unable to register namespace
     */
    public static void registerNamespace(final JcrService jcrService, final String prefix, final String uri) throws RepositoryException {
        final Session session = jcrService.createSession();
        try {
            final NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
            namespaceRegistry.registerNamespace(prefix, uri);
        } finally {
            jcrService.destroySession(session);
        }
    }

    /**
     * Check whether a namespace URI exists in the namespace registry.
     *
     * @param uri     the URI of the namespace
     * @return true when namespace with given URI exists, false otherwise
     */
    public static boolean namespaceUriExists(final JcrService jcrService, final String uri) {
        final Session session = jcrService.createSession();
        try {
            final NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
            // Check whether a prefix is mapped for the prefix
            final String p = namespaceRegistry.getPrefix(uri);
            return !Strings.isNullOrEmpty(p);
        } catch (NamespaceException e) {
            // expected if not there
            return false;
        } catch (RepositoryException e) {
            log.error("Error while determining namespace check.", e);
        } finally {
            jcrService.destroySession(session);
        }
        return false;
    }

    /**
     * Check whether a namespace prefix exists in the namespace registry.
     *
     * @param prefix  the prefix of the namespace
     * @return true when namespace with given prefix exists, false otherwise
     */
    public static boolean namespacePrefixExists(final JcrService jcrService, final String prefix) {
        final Session session = jcrService.createSession();
        try {
            final NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
            // Check whether a URI is mapped for the prefix
            final String p = namespaceRegistry.getURI(prefix);
            return !Strings.isNullOrEmpty(p);
        } catch (NamespaceException e) {
            // expected:
            return false;
        } catch (RepositoryException e) {
            log.error("Error while determining namespace check.", e);
        } finally {
            jcrService.destroySession(session);
        }
        return false;
    }

    public static void registerDocumentType(
            final JcrService jcrService,
            final String prefix,
            final String name,
            final boolean orderable,
            final boolean mixin,
            final String... superTypes) throws RepositoryException {
        final Session session = jcrService.createSession();
        try {
            final Workspace workspace = session.getWorkspace();
            final NodeTypeManager manager = workspace.getNodeTypeManager();
            final NodeTypeTemplate template = manager.createNodeTypeTemplate();

            template.setName(prefix + ':' + name);
            template.setOrderableChildNodes(orderable);
            template.setMixin(mixin);
            if (superTypes.length > 0) {
                template.setDeclaredSuperTypeNames(superTypes);
            }


            manager.registerNodeType(template, false);
        } finally {
            jcrService.destroySession(session);
        }
    }

    /**
     * Create the hippo namespace node (underneath {@code HippoNodeType.NAMESPACES_PATH}) for the
     * provided namespace prefix. When there is already a namespace node available for the prefix,
     * no new namespace node will be created.
     *
     * @param prefix  the namespace prefix
     * @return the node representing the namespace
     * @throws RepositoryException when hippo namespace can't be created
     */
    public static void createHippoNamespace(final JcrService jcrService, final String prefix) throws RepositoryException {
        if (StringUtils.isBlank(prefix)) {
            throw new RepositoryException("Unable to create namespace for empty prefix");
        }

        final Session session = jcrService.createSession();
        try {
            final Node namespaces = session.getRootNode().getNode(HippoNodeType.NAMESPACES_PATH);
            if (namespaces.hasNode(prefix)) {
                log.info("Namespace '{}' already registered", prefix);
                session.save();
                //return namespaces.getNode(prefix);
            }
            //return namespaces.addNode(prefix, HippoNodeType.NT_NAMESPACE);
        } finally {
            jcrService.destroySession(session);
        }
    }

    public static Node createHippoNamespace(final Session session, final String prefix) throws RepositoryException {
        if (StringUtils.isBlank(prefix)) {
            throw new RepositoryException("Unable to create namespace for empty prefix");
        }
        final Node namespaces = session.getRootNode().getNode(HippoNodeType.NAMESPACES_PATH);
        if (namespaces.hasNode(prefix)) {
            log.info("Namespace '{}' already registered", prefix);
            return namespaces.getNode(prefix);
        }
        return namespaces.addNode(prefix, HippoNodeType.NT_NAMESPACE);
    }

    public static Node getHippoNamespaceNode(final Session session, final String prefix) throws RepositoryException {
        if (StringUtils.isBlank(prefix)) {
            throw new RepositoryException("Unable to fetch namespace for empty prefix");
        }

        final Node namespaces = session.getRootNode().getNode(HippoNodeType.NAMESPACES_PATH);
        if (namespaces.hasNode(prefix)) {
            return namespaces.getNode(prefix);
        }
        log.info("Namespace node '{}' not available", prefix);
        return null;
    }

    /**
     * Check whether a node type exists according to the node type manaager.
     *
     * @param nodeType the node type to check
     * @return true when the node type exists, false otherwise
     * @throws RepositoryException
     */
    public static boolean nodeTypeExists(final JcrService jcrService, final String nodeType) throws RepositoryException {
        if (StringUtils.isEmpty(nodeType)) {
            log.debug("Empty node type does not exist");
            return false;
        }
        final Session session = jcrService.createSession();
        try {
            final Workspace workspace = session.getWorkspace();
            final NodeTypeManager manager = workspace.getNodeTypeManager();
            return manager.hasNodeType(nodeType);
        } finally {
            jcrService.destroySession(session);
        }
    }

    /**
     * Check whether a node type is a certain super type.
     *
     * @param nodeType  the node type to check
     * @param superType the node type to verify against
     * @return true when nodeType is of superType
     * @throws RepositoryException
     */
    public static boolean isNodeOfSuperType(final JcrService jcrService, final String nodeType, final String superType) throws RepositoryException {
        if (StringUtils.isEmpty(nodeType)) {
            log.debug("Empty node type does not exist");
            return false;
        }
        final Session session = jcrService.createSession();
        try {
            final Workspace workspace = session.getWorkspace();
            final NodeTypeManager manager = workspace.getNodeTypeManager();
            if (manager.hasNodeType(nodeType)) {
                final NodeType type = manager.getNodeType(nodeType);
                if (type != null) {
                    return type.isNodeType(superType);
                }
            }
        } finally {
            jcrService.destroySession(session);
        }
        return false;
    }

    /**
     * Retrieve a list of node types that are registered in the repository and
     * are sub types of the super type.
     * <p/>
     * The super type itself will be included by default. Use {@link #getNodeTypesOfType(JcrService, String, boolean)}
     * to determine whether the super type should be included as well.
     *
     * @param superType the super type
     * @return a list of node types
     * @throws RepositoryException when exception in repository occurs
     */
    public static List<String> getNodeTypesOfType(final JcrService jcrService, final String superType) throws RepositoryException {
        return getNodeTypesOfType(jcrService, superType, true);
    }

    /**
     * Retrieve a list of node types that are registered in the repository and
     * are sub types of the super type. When {@code includeSuperType} is true
     * the super type will be included in the list. Otherwise the super type
     * will be excluded from the list.
     *
     * @param superType        the super type
     * @param includeSuperType determine whether super type should be returned in list
     * @return a list of node types
     * @throws RepositoryException when exception in repository occurs
     */
    public static List<String> getNodeTypesOfType(final JcrService jcrService, final String superType, boolean includeSuperType) throws RepositoryException {
        final List<String> nodeTypes = new ArrayList<>();
        if (StringUtils.isEmpty(superType)) {
            log.debug("Return empty list for empty super type");
            return nodeTypes;
        }
        final Session session = jcrService.createSession();
        try {
            final Workspace workspace = session.getWorkspace();
            final NodeTypeManager manager = workspace.getNodeTypeManager();
            final NodeTypeIterator primaryNodeTypes = manager.getPrimaryNodeTypes();
            while (primaryNodeTypes.hasNext()) {
                final NodeType nodeType = primaryNodeTypes.nextNodeType();
                final String name = nodeType.getName();
                if (includeSuperType && isNodeType(nodeType, superType)) {
                    log.debug("Adding {} to list of types of {}", name, superType);
                    nodeTypes.add(name);
                } else if (isSubType(nodeType, superType)) {
                    log.debug("Adding {} to list of sub types of {}", name, superType);
                    nodeTypes.add(name);
                }
            }
        } finally {
            jcrService.destroySession(session);
        }
        return nodeTypes;
    }

    private static boolean isNodeType(final NodeType nodeType, final String superType) {
        if (nodeType == null) {
            log.debug("Unable to check node type for empty node type");
            return false;
        }
        if (StringUtils.isEmpty(superType)) {
            log.debug("Unable to check node type for empty super type");
            return false;
        }
        return nodeType.isNodeType(superType);
    }

    private static boolean isSubType(final NodeType nodeType, final String superType) {
        if (isNodeType(nodeType, superType)) {
            return !superType.equals(nodeType.getName());
        } else {
            return false;
        }
    }

}
