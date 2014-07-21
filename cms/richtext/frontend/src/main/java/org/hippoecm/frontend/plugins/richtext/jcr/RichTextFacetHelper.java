/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.jcr;

import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RichTextFacetHelper {

    static final Logger log = LoggerFactory.getLogger(RichTextFacetHelper.class);

    private RichTextFacetHelper() {
    }

    static void createFacets(final Node node, final Set<String> uuids) throws RepositoryException {
        final Session session = node.getSession();
        for (String uuid : uuids) {
            try {
                final Node target = session.getNodeByIdentifier(uuid);
                createFacet(node, target.getName(), uuid);
            } catch (ItemNotFoundException e) {
                log.warn("Cannot create facet node below '{}', target UUID does not exist: '{}'", JcrUtils.getNodePathQuietly(node), uuid);
            }
        }
    }

    static String createFacet(final Node node, final String link, final Node target) throws RepositoryException {
        return createFacet(node, link, target.getIdentifier());
    }

    static String createFacet(final Node node, final String link, final String targetUuid) throws RepositoryException {

        String linkName = newLinkName(node, link);

        Node facetselect = node.addNode(linkName, HippoNodeType.NT_FACETSELECT);
        facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, targetUuid);
        facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {});
        facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[] {});
        facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[] {});

        return linkName;
    }

    public static String getChildDocBaseOrNull(final Node node, final String childNodeName) {
        try {
            if (isRelativePath(childNodeName) && node.hasNode(childNodeName)) {
                final Node child = node.getNode(childNodeName);
                return getDocBaseOrNull(child);
            }
        } catch (RepositoryException e) {
            final String parentNodePath = JcrUtils.getNodePathQuietly(node);
            final String childNodePath = parentNodePath != null ? parentNodePath + "/" + childNodeName : childNodeName;
            log.warn("Cannot get child node '{}'", childNodePath, e);
        }
        return null;
    }

    private static boolean isRelativePath(final String childNodeName) {
        return StringUtils.isNotEmpty(childNodeName) && !StringUtils.startsWith(childNodeName, "/");
    }

    private static String getDocBaseOrNull(final Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_FACETSELECT)) {
            return JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_DOCBASE, null);
        }
        return null;
    }

    static String getChildFacetNameOrNull(final Node node, final String uuid) {
        try {
            final NodeIterator children = node.getNodes();
            while (children.hasNext()) {
                final Node child = children.nextNode();
                final String docBaseOrNull = getDocBaseOrNull(child);
                if (uuid.equals(docBaseOrNull)) {
                    return child.getName();
                }
            }
        } catch (RepositoryException e) {
            log.warn("Node '{}' does not have a child facet node for UUID '{}'",
                    JcrUtils.getNodePathQuietly(node), uuid);
        }
        return null;
    }

    private static String newLinkName(Node node, String link) throws RepositoryException {
        if (!node.hasNode(link)) {
            return link;
        }
        int postfix = 1;
        while (true) {
            final String testLink = NodeNameCodec.encode(link + "_" + postfix, true);
            if (!node.hasNode(testLink)) {
                return testLink;
            }
            postfix++;
        }
    }

}
