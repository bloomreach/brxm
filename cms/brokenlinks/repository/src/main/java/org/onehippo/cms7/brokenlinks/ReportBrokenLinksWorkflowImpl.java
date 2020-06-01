/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.brokenlinks;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.ext.WorkflowImpl;

public class ReportBrokenLinksWorkflowImpl extends WorkflowImpl implements ReportBrokenLinksWorkflow, InternalWorkflow {

    private static final long serialVersionUID = 1L;

    private final Session session;
    private final Node subject;

    @SuppressWarnings("unused") // workflow engine expects this constructor signature
    public ReportBrokenLinksWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject) throws RemoteException, RepositoryException {
        this.session = rootSession;
        this.subject = rootSession.getNodeByIdentifier(subject.getIdentifier());
    }

    @Override
    public void reportBrokenLinks(Collection<Link> brokenLinks) throws WorkflowException, RepositoryException, RemoteException {
        if (brokenLinks.isEmpty()) {
            removeBrokenLinksMixin();
        } else {
            generateReport(brokenLinks);
        }
        session.save();
    }

    private void generateReport(final Collection<Link> brokenLinks) throws RepositoryException {
        final Node node = getReportNode();
        if (!node.isCheckedOut()) {
            node.getSession().getWorkspace().getVersionManager().checkout(node.getPath());
        }
        if (node.isNodeType(NodeType.BROKENLINKS_MIXIN)) {
            cleanupExistingReport(node, brokenLinks);
        } else {
            node.addMixin(NodeType.BROKENLINKS_MIXIN);
        }
        for (Link link : brokenLinks) {
            Node brokenLinkNode = node.addNode(NodeType.BROKENLINKS_NODE, NodeType.BROKENLINKS_NODE);
            brokenLinkNode.setProperty(NodeType.PROPERTY_URL, link.getUrl());
            brokenLinkNode.setProperty(NodeType.PROPERTY_ERROR_CODE, link.getResultCode());
            brokenLinkNode.setProperty(NodeType.PROPERTY_ERROR_MESSAGE, link.getResultMessage());
            brokenLinkNode.setProperty(NodeType.PROPERTY_BROKEN_SINCE, link.getBrokenSince());
            brokenLinkNode.setProperty(NodeType.PROPERTY_LAST_TIME_CHECKED, link.getLastTimeChecked());
            StringBuilder excerpt = new StringBuilder();
            excerpt.append("<a target=\"_blank\" href =\"");
            excerpt.append(link.getUrl());
            excerpt.append("\">");
            excerpt.append(link.getUrl());
            excerpt.append("</a>");
            brokenLinkNode.setProperty(NodeType.PROPERTY_EXCERPT, excerpt.toString());
        }
    }

    /**
     * Removes all reported broken links for the given node. If a reported broken link is also present in the given
     * collection of broken links, the 'broken since' date of the broken link in the collection is set to the
     * 'broken since' date of the reported broken link. That way, links that remain broken over a number of checks
     * will keep their initial 'broken since' date.
     *
     * @param reportNode the report node
     * @param brokenLinks the list of new broken links
     *
     * @throws RepositoryException when the broken links under the report node cannot be read or removed.
     */
    private void cleanupExistingReport(final Node reportNode, final Collection<Link> brokenLinks) throws RepositoryException {
        for (NodeIterator iter = reportNode.getNodes(NodeType.BROKENLINKS_NODE); iter.hasNext();) {
            Node brokenLinkNode = iter.nextNode();
            String url = brokenLinkNode.getProperty(NodeType.PROPERTY_URL).getString();
            for (Link link : brokenLinks) {
                if (link.getUrl().equals(url)) {
                    if (brokenLinkNode.hasProperty(NodeType.PROPERTY_BROKEN_SINCE)) {
                         link.setBrokenSince(brokenLinkNode.getProperty(NodeType.PROPERTY_BROKEN_SINCE).getDate());
                    }
                }
            }
            brokenLinkNode.remove();
        }
    }

    private Node getReportNode() throws RepositoryException {
        Node node = subject;
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            return node;
        }
        if (node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
            node = node.getParent();
        }
        return node;
    }

    private void removeBrokenLinksMixin() throws RepositoryException {
        Node node = getReportNode();
        if (node.isNodeType(NodeType.BROKENLINKS_MIXIN)) {
            if (!node.isCheckedOut()) {
                node.getSession().getWorkspace().getVersionManager().checkout(node.getPath());
            }
            node.removeMixin(NodeType.BROKENLINKS_MIXIN);
        }
    }

    @Override
    public Map<String, Serializable> hints() throws WorkflowException {
        Map<String, Serializable> m = super.hints();
        m.put("reportBrokenLinks", Boolean.TRUE);
        return m;
    }
}
