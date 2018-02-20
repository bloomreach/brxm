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
package org.hippoecm.hst.core.channelmanager;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Content Management Link related utilities.
 */
public class ContentManagementLinkUtils {

    private static Logger log = LoggerFactory.getLogger(ContentManagementLinkUtils.class);

    private static final char DOUBLE_QUOTE = '"';

    private ContentManagementLinkUtils() {
    }

    /**
     * Get the CMS base URL location for the current request context.
     * @return the CMS base URL location for the current request context
     */
    public static String getCmsBaseURL() {
        HstRequestContext requestContext = RequestContextProvider.get();
        Mount mount = requestContext.getResolvedMount().getMount();

        if (mount.getCmsLocations().isEmpty()) {
            log.warn("Skipping cms edit url no cms locations configured in hst hostgroup configuration");
            return null;
        }

        String cmsBaseUrl;

        if (mount.getCmsLocations().size() == 1) {
            cmsBaseUrl = mount.getCmsLocations().get(0);
        } else {
            cmsBaseUrl = getBestCmsLocation(mount.getCmsLocations(),
                    HstRequestUtils.getFarthestRequestHost(requestContext.getServletRequest(), false));
        }

        return StringUtils.removeEnd(cmsBaseUrl, "/");
    }

    /**
     * Find the editable node from the given {@link HippoNode node} object by resolving the canonical handle node.
     * @param node base hippo node
     * @return the editable node from the given {@link HippoNode node} object by resolving the canonical handle node
     * @throws RepositoryException if repository exception occurs
     */
    public static Node getCmsEditableNode(final HippoNode node) throws RepositoryException {
        Node editableNode = null;

        try {
            editableNode = node.getCanonicalNode();

            if (editableNode == null) {
                log.debug("Cannot create a 'surf and edit' link for a pure virtual jcr node: '{}'", node.getPath());
                return null;
            } else {
                Node rootNode = editableNode.getSession().getRootNode();

                if (editableNode.isSame(rootNode)) {
                    log.warn("Cannot create a 'surf and edit' link for a jcr root node.");
                }

                if (editableNode.isNodeType(HstNodeTypes.NODETYPE_HST_SITES)) {
                    log.warn("Cannot create a 'surf and edit' link for a jcr node of type '{}'.",
                            HstNodeTypes.NODETYPE_HST_SITES);
                }

                if (editableNode.isNodeType(HstNodeTypes.NODETYPE_HST_SITE)) {
                    log.warn("Cannot create a 'surf and edit' link for a jcr node of type '{}'.",
                            HstNodeTypes.NODETYPE_HST_SITE);
                }

                Node handleNode = getHandleNodeIfIsAncestor(editableNode, rootNode);

                if (handleNode != null) {
                    // take the handle node as this is the one expected by the cms edit url:
                    editableNode = handleNode;
                    log.debug("The nodepath for the edit link in cms is '{}'", editableNode.getPath());
                } else {
                    // do nothing, most likely, editNode is a folder node.
                }
            }
        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve the node path for the edit location", e);
            return null;
        }
 
        return editableNode;
    }

    /**
     * Convert the given {@code map} to a JSON string.
     * @param map map to convert to a JSON string
     * @return converted JSON string from the given {@code map}
     */
    public static String toJSONMap(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        final StringBuilder builder = new StringBuilder("{");
        for (Map.Entry<?, ?> each : map.entrySet()) {
            doubleQuote(builder, each.getKey()).append(':');
            doubleQuote(builder, each.getValue()).append(',');
        }
        final int length = builder.length();
        return builder.replace(length - 1, length, "}").toString();
    }

    private static String getBestCmsLocation(final List<String> cmsLocations, final String cmsRequestHostName) {
        for (String cmsLocation : cmsLocations) {
            String hostName = cmsLocation;

            if (cmsLocation.startsWith("http://")) {
                hostName = hostName.substring("http://".length());
            } else if (cmsLocation.startsWith("https://")) {
                hostName = hostName.substring("https://".length());
            }

            hostName = StringUtils.substringBefore(hostName, "/");

            if (cmsRequestHostName.equals(hostName)) {
                log.debug("For cms request with host {} found from {} best cms host to be {}", cmsRequestHostName,
                        cmsLocations, cmsLocation);
                return cmsLocation;
            }
        }

        log.debug("For cms request with host {} no matching host was found in {}. Return {} as cms host.",
                cmsRequestHostName, cmsLocations, cmsLocations.get(0));

        return cmsLocations.get(0);
    }

    private static Node getHandleNodeIfIsAncestor(Node currentNode, Node rootNode) throws RepositoryException {
        if (currentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
            return currentNode;
        }

        if (currentNode.isSame(rootNode)) {
            return null;
        }

        return getHandleNodeIfIsAncestor(currentNode.getParent(), rootNode);
    }

    private static StringBuilder doubleQuote(StringBuilder builder, Object value) {
        return builder.append(DOUBLE_QUOTE).append(value).append(DOUBLE_QUOTE);
    }

}
