/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.version;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.commons.proxy.Interceptor;
import org.apache.commons.proxy.ProxyFactory;
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.util.JcrConstants;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import static org.apache.jackrabbit.JcrConstants.JCR_FROZENMIXINTYPES;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

/**
 * Utility to handle versioned, frozen node (nt:frozenNode).
 * <p>
 * For example, a version is stored, embedding a frozen node which contains the versioned document variant content,
 * in locations like '/jcr:system/jcr:versionStorage/91/90/1d/91901d8f-d6ab-480a-a693-a6e459a678c3/1.0/jcr:frozenNode',
 * where '1.0' node is of 'nt:version' jcr primary type and 'jcr:frozenNode' is the node containing the versioned
 * document variant node content.
 * </P>
 * <p>
 * Note that HST Content Beans module doesn't allow to map between beans and frozen nodes by default.
 * Therefore, this utility class provides a way to proxy a frozen node instance to pretend as a non-frozen node instance.
 * </P>
 */
public class HippoBeanFrozenNodeUtils {

    private HippoBeanFrozenNodeUtils() {
    }

    /**
     * Returns a proxy which pretends to be non-frozen node from the {@code frozenNode}.
     *
     * @param frozenNode       frozen node
     * @param absWorkspacePath the workspace path of the frozen node. Note that at this workspace path there might not be
     *                         a node present
     * @return a proxy which pretends to be non-frozen node from the {@code frozenNode}
     * @throws RepositoryException if unexpected repository exception occurs
     */
    public static HippoNode getWorkspaceFrozenNode(final Node frozenNode, final String absWorkspacePath, final String name) throws RepositoryException {
        if (absWorkspacePath.startsWith("/jcr:system")) {
            throw new IllegalArgumentException(String.format("absWorkspacePath should never be a jcr:system path but it was %s", absWorkspacePath));
        }

        if (!frozenNode.isNodeType(NT_FROZEN_NODE)) {
            throw new IllegalArgumentException(String.format("frozenNode must be type of %s!", NT_FROZEN_NODE));
        }

        ProxyFactory proxyFactory = new ProxyFactory();

        final Interceptor primaryNodeTypeInterceptor = invocation -> {
            final Method method = invocation.getMethod();
            final String methodName = method.getName();

            if ("getName".equals(methodName)) {
                return frozenNode.getProperty(JcrConstants.JCR_FROZEN_PRIMARY_TYPE).getString();
            }

            return invocation.proceed();
        };

        final NodeType primaryNodeTypeProxy =
                (NodeType) proxyFactory.createInterceptorProxy(frozenNode.getPrimaryNodeType(),
                        primaryNodeTypeInterceptor,
                        new Class[]{NodeType.class});

        final HippoNodeWrapper wrapper = new HippoNodeWrapper();

        final Interceptor nodeInterceptor = invocation -> {
            final Method method = invocation.getMethod();
            final String methodName = method.getName();
            final Object[] arguments = invocation.getArguments();

            switch (methodName) {
                case "getPrimaryNodeType":
                    return primaryNodeTypeProxy;
                case "getPath":
                    return absWorkspacePath;
                case "getName":
                    return name;
                case "isNodeType":
                    return proxyIsNodeType(frozenNode, (String) arguments[0]);
                case "getParent":
                    final String parentWorkspacePath = substringBeforeLast(absWorkspacePath, "/");
                    final Node parent = frozenNode.getParent();
                    if (parent.isNodeType(NT_FROZEN_NODE)) {
                        // the frozen node does not contain the original node name. The workspace node name can
                        // have been removed already. Hence, all we can return is part behind the last '/' in
                        // absWorkspacePath and remove potential index holder in the path (eg [2])
                        // substringBefore '[' is good enough since AFAIK not allowed in a name
                        return getWorkspaceFrozenNode(parent, parentWorkspacePath, getNodeNameFromPath(parentWorkspacePath));
                    }
                    // parent is not a frozen node hence return the workspace parent because this parent is meant to
                    // be fetched
                    return frozenNode.getSession().getNode(parentWorkspacePath);
                case "getNode":
                    return proxyGetNode(frozenNode, absWorkspacePath, (String) arguments[0]);
                case "getNodes":
                    if (arguments == null || arguments.length == 0) {
                        return proxyGetNodes(frozenNode, absWorkspacePath);
                    } else if (arguments[0] != null && arguments[0].getClass().isArray()) {
                        return proxyGetNodes(frozenNode, absWorkspacePath, (String[]) arguments[0]);
                    } else {
                        return proxyGetNodes(frozenNode, absWorkspacePath, (String) arguments[0]);
                    }
                case "getCanonicalNode":
                    // just return the proxy object itself since that is the canonical we are looking for
                    return wrapper.getHippoNode();
                case "getDisplayName":
                    try {
                        // the workspace might not have a node for the absolute workspace path
                        return ((HippoNode) frozenNode.getSession().getNode(absWorkspacePath)).getDisplayName();
                    } catch (PathNotFoundException e) {
                        return name;
                    }
                case "pendingChanges":
                    return new EmptyNodeIterator();
                case "isVirtual":
                    return FALSE;
                case "recomputeDerivedData":
                    return FALSE;
                case "getFrozenNode":
                    return frozenNode;
                default:
                    return invocation.proceed();
            }
        };

        HippoNode pretenderNode = (HippoBeanFrozenNode) proxyFactory
                .createInterceptorProxy(frozenNode, nodeInterceptor, new Class[]{HippoBeanFrozenNode.class});

        wrapper.setHippoNode(pretenderNode);

        return pretenderNode;
    }

    private static String getNodeNameFromPath(final String parentWorkspacePath) {
        return substringBefore(substringAfterLast(parentWorkspacePath, "/"), "[");
    }

    private static class HippoNodeWrapper {

        private HippoNode hippoNode;

        public HippoNode getHippoNode() {
            return hippoNode;
        }

        public void setHippoNode(final HippoNode hippoNode) {
            this.hippoNode = hippoNode;
        }
    }

    private static Boolean proxyIsNodeType(final Node frozenNode, final String sourceNodeType) throws RepositoryException {

        if (frozenNode.isNodeType(sourceNodeType)) {
            // we also return true if the check is whether we are dealing with a frozen node
            return TRUE;
        }

        final NodeTypeManager nodeTypeManager = frozenNode.getSession().getWorkspace().getNodeTypeManager();

        final String originalTypeString = frozenNode.getProperty(JcrConstants.JCR_FROZEN_PRIMARY_TYPE).getString();

        final NodeType originalType = nodeTypeManager.getNodeType(originalTypeString);

        if (originalType.isNodeType(sourceNodeType)) {
            return TRUE;
        }

        final String[] frozenMixins = JcrUtils.getMultipleStringProperty(frozenNode, JCR_FROZENMIXINTYPES, new String[0]);

        for (String frozenMixin : frozenMixins) {
            final NodeType nodeType = nodeTypeManager.getNodeType(frozenMixin);
            if (nodeType.isNodeType(sourceNodeType)) {
                return TRUE;
            }
        }

        return FALSE;
    }

    private static Node proxyGetNode(final Node frozenNode, final String absWorkspacePath, final String relPath) throws RepositoryException {
        Node childFrozenNode = frozenNode.getNode(relPath);
        Node pretendingChild = getWorkspaceFrozenNode(childFrozenNode, absWorkspacePath + "/" + relPath, childFrozenNode.getName());
        return pretendingChild;
    }

    private static NodeIterator proxyGetNodes(final Node frozenNode, final String absWorkspacePath) throws RepositoryException {
        List<Node> childNodes = new LinkedList<>();
        Node childNode;

        for (NodeIterator nodeIt = frozenNode.getNodes(); nodeIt.hasNext(); ) {
            childNode = nodeIt.nextNode();

            if (childNode != null) {
                childNode = getWorkspaceFrozenNode(childNode, getPath(absWorkspacePath, childNode), childNode.getName());
                childNodes.add(childNode);
            }
        }

        return new NodeIteratorAdapter(childNodes);
    }

    private static String getPath(final String absWorkspacePath, final Node childNode) throws RepositoryException {
        String path = absWorkspacePath + "/" + childNode.getName();
        if (childNode.getIndex() > 1) {
            path += "[" + childNode.getIndex() + "]";
        }
        return path;
    }

    private static NodeIterator proxyGetNodes(final Node frozenNode, final String absWorkspacePath, final String namePattern) throws RepositoryException {
        List<Node> childNodes = new LinkedList<>();
        Node childNode;

        for (NodeIterator nodeIt = frozenNode.getNodes(namePattern); nodeIt.hasNext(); ) {
            childNode = nodeIt.nextNode();

            if (childNode != null) {
                childNode = getWorkspaceFrozenNode(childNode, getPath(absWorkspacePath, childNode), childNode.getName());
                childNodes.add(childNode);
            }
        }

        return new NodeIteratorAdapter(childNodes);
    }

    private static NodeIterator proxyGetNodes(final Node frozenNode, final String absWorkspacePath, final String[] nameGlobs) throws RepositoryException {
        List<Node> childNodes = new LinkedList<>();
        Node childNode;

        for (NodeIterator nodeIt = frozenNode.getNodes(nameGlobs); nodeIt.hasNext(); ) {
            childNode = nodeIt.nextNode();

            if (childNode != null) {
                childNode = getWorkspaceFrozenNode(childNode, getPath(absWorkspacePath, childNode), childNode.getName());
                childNodes.add(childNode);
            }
        }

        return new NodeIteratorAdapter(childNodes);
    }

    private static class EmptyNodeIterator implements NodeIterator {

        @Override
        public void skip(long skipNum) {
        }

        @Override
        public long getSize() {
            return 0;
        }

        @Override
        public long getPosition() {
            return 0;
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            return null;
        }

        @Override
        public void remove() {
        }

        @Override
        public Node nextNode() {
            return null;
        }
    }
}
