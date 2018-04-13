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
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.proxy.Interceptor;
import org.apache.commons.proxy.Invocation;
import org.apache.commons.proxy.ProxyFactory;
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

/**
 * Utility to handle versioned, frozen node (nt:frozenNode).
 * <P>
 * For example, a version is stored, embedding a frozen node which contains the versioned document variant content,
 * in locations like '/jcr:system/jcr:versionStorage/91/90/1d/91901d8f-d6ab-480a-a693-a6e459a678c3/1.0/jcr:frozenNode',
 * where '1.0' node is of 'nt:version' jcr primary type and 'jcr:frozenNode' is the node containing the versioned
 * document variant node content.
 * </P>
 * <P>
 * Note that HST Content Beans module doesn't allow to map between beans and frozen nodes by default.
 * Therefore, this utility class provides a way to proxy a frozen node instance to pretend as a non-frozen node instance.
 * </P>
 */
public class HippoBeanFrozenNodeUtils {

    private static final Logger log = LoggerFactory.getLogger(HippoBeanFrozenNodeUtils.class);

    private HippoBeanFrozenNodeUtils() {
    }

    /**
     * Returns a proxy which pretends to be non-frozen node from the {@code frozenNode}.
     * @param frozenNode frozen node
     * @param absWorkspacePath the workspace path of the frozen node. Note that at this workspace path there might not be
     *                         a node present
     * @return a proxy which pretends to be non-frozen node from the {@code frozenNode}
     * @throws RepositoryException if unexpected repository exception occurs
     */
    public static HippoNode getWorkspaceFrozenNode(final Node frozenNode, final String absWorkspacePath) throws RepositoryException {
        if (absWorkspacePath.startsWith("/jcr:system")) {
            log.error("absWorkspacePath should always be a workspace path but was '{}'", absWorkspacePath,
                    new Throwable("absWorkspacePath should never be a jcr:system path"));
        }

        if (!frozenNode.isNodeType(NT_FROZEN_NODE)) {
            throw new IllegalArgumentException("frozenNode must be type of nt:frozenNode!");
        }

        ProxyFactory proxyFactory = new ProxyFactory();

        final Interceptor primaryNodeTypeInterceptor = new Interceptor() {
            @Override
            public Object intercept(Invocation invocation) throws Throwable {
                final Method method = invocation.getMethod();
                final String methodName = method.getName();

                if ("getName".equals(methodName)) {
                    return frozenNode.getProperty("jcr:frozenPrimaryType").getString();
                }

                return invocation.proceed();
            }
        };

        final NodeType primaryNodeTypeProxy =
                (NodeType) proxyFactory.createInterceptorProxy(frozenNode.getPrimaryNodeType(),
                        primaryNodeTypeInterceptor,
                        new Class[]{NodeType.class});


        final Interceptor nodeInterceptor = invocation -> {
            final Method method = invocation.getMethod();
            //final Class<?> declaringClass = method.getDeclaringClass();
            final String methodName = method.getName();
            final Object [] arguments = invocation.getArguments();

            if ("getPrimaryNodeType".equals(methodName)) {
                return primaryNodeTypeProxy;
            } if ("getPath".equals(methodName)) {
                return absWorkspacePath;
            } else if ("isNodeType".equals(methodName)) {
                return proxyIsNodeType(frozenNode, (String) arguments[0]);
            } else if ("getParent".equals(methodName)) {
                final String parentWorkspacePath = substringBeforeLast(absWorkspacePath, "/");
                final Node parent = frozenNode.getParent();
                if (parent.isNodeType(NT_FROZEN_NODE)) {
                    return getWorkspaceFrozenNode(parent, parentWorkspacePath);
                }
                // parent is not a frozen node hence return the workspace parent because this parent is meant to
                // be fetched
                return frozenNode.getSession().getNode(parentWorkspacePath);
            } else if ("getNode".equals(methodName)) {
                return proxyGetNode(frozenNode, absWorkspacePath, (String) arguments[0]);
            } else if ("getNodes".equals(methodName)) {
                if (arguments == null || arguments.length == 0) {
                    return proxyGetNodes(frozenNode, absWorkspacePath);
                } else if (arguments[0] != null && arguments[0].getClass().isArray()) {
                    return proxyGetNodes(frozenNode, absWorkspacePath, (String []) arguments[0]);
                } else {
                    return proxyGetNodes(frozenNode, absWorkspacePath, (String) arguments[0]);
                }
            } else if ("getCanonicalNode".equals(methodName)) {
                return frozenNode;
            } if ("getDisplayName".equals(methodName)) {
                // TODO how to get the display name? Property lookup?
                return frozenNode.getName();
            } else if ("pendingChanges".equals(methodName)) {
                return new EmptyNodeIterator();
            } else if ("isVirtual".equals(methodName)) {
                return FALSE;
            } else if ("recomputeDerivedData".equals(methodName)) {
                return FALSE;
            } else if ("getFrozenNode".equals(methodName)) {
                return frozenNode;
            }

            return invocation.proceed();
        };

        HippoNode pretenderNode = (HippoBeanFrozenNode) proxyFactory
                .createInterceptorProxy(frozenNode, nodeInterceptor, new Class[]{HippoBeanFrozenNode.class});

        return pretenderNode;
    }

    private static Boolean proxyIsNodeType(final Node frozenNode, final String nodeType) throws RepositoryException {
        // TODO this does not check super types yet! Needs improvement. Most likely via the node type registry

        if (frozenNode.isNodeType(nodeType)) {
            return TRUE;
        }

        String frozenType = frozenNode.getProperty("jcr:frozenPrimaryType").getString();

        if (nodeType.equals(frozenType)) {
            return TRUE;
        }

        for (NodeType mixinType : frozenNode.getMixinNodeTypes()) {
            frozenType = mixinType.getName();

            if (nodeType.equals(frozenType)) {
                return TRUE;
            }
        }

        return FALSE;
    }

    private static Node proxyGetNode(final Node frozenNode, final String absWorkspacePath, final String relPath) throws RepositoryException {
        Node childFrozenNode = frozenNode.getNode(relPath);
        Node pretendingChild = getWorkspaceFrozenNode(childFrozenNode, absWorkspacePath + "/" + relPath);
        return pretendingChild;
    }

    private static NodeIterator proxyGetNodes(final Node frozenNode, final String absWorkspacePath) throws RepositoryException {
        List<Node> childNodes = new LinkedList<>();
        Node childNode;

        for (NodeIterator nodeIt = frozenNode.getNodes(); nodeIt.hasNext(); ) {
            childNode = nodeIt.nextNode();

            if (childNode != null) {
                // TODO SNS should get an index in the path?
                childNode = getWorkspaceFrozenNode(childNode, absWorkspacePath + "/" + childNode.getName());
                childNodes.add(childNode);
            }
        }

        return new NodeIteratorAdapter(childNodes);
    }

    private static NodeIterator proxyGetNodes(final Node frozenNode, final String absWorkspacePath, final String namePattern) throws RepositoryException {
        List<Node> childNodes = new LinkedList<>();
        Node childNode;

        for (NodeIterator nodeIt = frozenNode.getNodes(namePattern); nodeIt.hasNext(); ) {
            childNode = nodeIt.nextNode();

            if (childNode != null) {
                // TODO SNS should get an index in the path?
                childNode = getWorkspaceFrozenNode(childNode, absWorkspacePath + "/" + childNode.getName());
                childNodes.add(childNode);
            }
        }

        return new NodeIteratorAdapter(childNodes);
    }

    private static NodeIterator proxyGetNodes(final Node frozenNode, final String absWorkspacePath, final String [] nameGlobs) throws RepositoryException {
        // TODO make sure this is hit in integration test as well!! (get children of a node of type HippoBeanFrozenNode
        List<Node> childNodes = new LinkedList<>();
        Node childNode;

        for (NodeIterator nodeIt = frozenNode.getNodes(nameGlobs); nodeIt.hasNext(); ) {
            childNode = nodeIt.nextNode();

            if (childNode != null) {
                // TODO SNS should get an index in the path?
                childNode = getWorkspaceFrozenNode(childNode, absWorkspacePath + "/" + childNode.getName());
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
