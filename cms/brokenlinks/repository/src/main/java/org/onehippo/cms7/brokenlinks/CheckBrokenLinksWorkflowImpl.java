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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.CronExpression;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckBrokenLinksWorkflowImpl extends WorkflowImpl implements CheckBrokenLinksWorkflow, InternalWorkflow {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(CheckBrokenLinksWorkflowImpl.class);

    private final Session rootSession;

    @SuppressWarnings("unused") // workflow engine expects this constructor signature
    public CheckBrokenLinksWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject) throws RemoteException, RepositoryException {
        this.rootSession = rootSession;
    }

    @Override
    public Map<String, Serializable> hints() {
        Map<String, Serializable> hints = new TreeMap<String,Serializable>();
        hints.put("checkLinks", Boolean.TRUE);
        return hints;
    }

    @Override
    public void checkLinks() throws WorkflowException, RepositoryException, RemoteException {
        final CheckExternalBrokenLinksConfig config = new CheckExternalBrokenLinksConfig(getWorkflowContext().getWorkflowConfiguration());
        final LinkChecker linkChecker = new LinkChecker(config);
        final WorkflowContext workflowContext = context.getWorkflowContext(null);
        log.info("Checking broken external links, configuration: ", config);
        // For the xpath query below, do not include a path constraint to begin with, like
        // /jcr:root/content/documents as this results in much less efficient queries
        String xpath = "//element(*,hippostd:html)";
        QueryResult result = rootSession.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        NodeIterator hippostdHtmlNodes =  result.getNodes();


        // the key of the links map is the uuid of the handle node
        Map<String,Set<Link>> linksByHandleUUID = new HashMap<String, Set<Link>>();
        // the unique links by URL
        Map<String,Link> linksByURL = new HashMap<String, Link>();
        
        
        long start = System.currentTimeMillis();
        int count = 0;
        while (hippostdHtmlNodes.hasNext()) {
            try {
                Node hippostdHtml = hippostdHtmlNodes.nextNode();
                // since this might be a long running loop and searches are not transactional, nodes might
                // be deleted in the meantime, hence, check for now
                if (hippostdHtml == null) {
                    continue;
                }
    
                if (!hippostdHtml.getPath().startsWith(config.getStartPath())) {
                    // skip paths that do not start with the path we want to scan below
                    continue;
                }
    
                // we need to group the links per handle because all hippostd:content
                // fields below a handle store their broken links directly below the handle
                Node handleNode = getHandleNode(hippostdHtml);
                
                if (handleNode == null) {
                    // could not find handle for hippostd:html node. Skip it
                    continue;
                }
                String handleUUID = handleNode.getIdentifier();
                // hippostd:content is a mandatory property so no need to check for existence
                String content = hippostdHtml.getProperty("hippostd:content").getString();
                try {
                    Set<Link> linksForHandle = linksByHandleUUID.get(handleUUID);
                    count++;
                    for (String url : PlainTextLinksExtractor.getLinks(content)) {
                        if (linksForHandle == null) {
                            linksForHandle = new HashSet<Link>();
                            linksByHandleUUID.put(handleUUID, linksForHandle);
                        }
                        
                        Link alreadyPresent = linksByURL.get(url);
                        if (alreadyPresent == null) {
                            Link link = new Link(url);
                            linksByURL.put(url, link);
                            log.debug("Adding to test for handle with '{}' the url '{}'", handleUUID, url);
                            linksForHandle.add(link);
                        } else {
                            log.debug("Adding to test for handle with '{}' the url '{}'", handleUUID, url);
                            linksForHandle.add(alreadyPresent);
                        }
                        
                    }
                } catch (IllegalStateException e) {
                    log.warn("Unable to get link from hippostd:html for node '{}'", hippostdHtml.getPath());
                }
            } catch (RepositoryException e) {
                log.warn("RepositoryException for hippostd:html node from search result. Skip and continue with next");
            }
        }
        long scanningTook = (System.currentTimeMillis() - start);
        log.info("Finished scanning all hippostd:html nodes for external links in {} seconds.", String.valueOf((scanningTook / 1000.0)));
        log.info("In total {}  hippostd:html nodes where scanned.", String.valueOf(count));
        log.info("In total {} handles have links", linksByHandleUUID.size());
        log.info("In total there are {} unique links", linksByURL.size());
        log.info("Starting scanning for external links that are broken");
        
        start = System.currentTimeMillis();
        // this set keeps track of scanned links to avoid needless double scanning

        // Now first check all external links whether they are available : The linkChecker runs multi-threaded thus
        // to utilize the multi-threading best, it is best to scan all Links combined, not just the ones for a single handle
        linkChecker.run(linksByURL.values());
        linkChecker.shutdown();

        log.info("Finished testing availability of all URLs. Tested '{}' URLs in {} seconds.", String.valueOf(linksByURL.size()), String.valueOf((scanningTook / 1000.0)));

        for (Map.Entry<String, Set<Link>> entry : linksByHandleUUID.entrySet()) {

            // all links belong to one document, so we can safely collect and process them at once:
            Collection<Link> brokenLinks = new ArrayList<Link>();
            for (Link link : entry.getValue()) {
                if (link.isBroken()) {
                    brokenLinks.add(link);
                }
            }
            // the key in the Map contains the handleUUID
            try {
                Node handleNode = rootSession.getNodeByIdentifier(entry.getKey());
                if (!brokenLinks.isEmpty() || handleNode.isNodeType(NodeType.BROKENLINKS_MIXIN)) {
                    // need to get the document below the handle to be able to get the workflow
                    Node doc;
                    try {
                        doc = handleNode.getNode(handleNode.getName());
                    } catch (PathNotFoundException e) {
                        log.warn("could not find document below handle '{}'. SKip", handleNode.getPath());
                        continue;
                    }
                    try {
                        Workflow reportWorkflow = workflowContext.getWorkflow("brokenlinks", new Document(doc.getIdentifier()));
                        if (reportWorkflow instanceof ReportBrokenLinksWorkflow) {
                            ((ReportBrokenLinksWorkflow) reportWorkflow).reportBrokenLinks(brokenLinks);
                        }
                    } catch (WorkflowException e) {
                        if (log.isDebugEnabled()) {
                            log.warn("WorkflowException exception while trying to write link report to handle with uuid '" + entry.getKey() + "'", e);
                        } else {
                            log.warn("WorkflowException exception while trying to write link report to handle with uuid '{}' : {}",entry.getKey(), e.toString());
                        }
                    } catch (RepositoryException e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Repository exception while trying to write link report to handle with uuid '" + entry.getKey() + "'", e);
                        } else {
                            log.warn("Repository exception while trying to write link report to handle with uuid '{}' : {}",entry.getKey(), e.toString());
                        }
                    }

                }
            } catch (ItemNotFoundException e) {
                if (log.isDebugEnabled()) {
                    log.warn("ItemNotFoundException exception while trying to create link report to handle with uuid '" + entry.getKey() + "'", e);
                } else {
                    log.warn("ItemNotFoundException exception while trying to create link report to handle with uuid '{}' : {}",entry.getKey(), e.toString());
                }
            } catch (RepositoryException e) {
                if (log.isDebugEnabled()) {
                    log.warn("RepositoryException exception while trying to create link report to handle with uuid '" + entry.getKey() + "'", e);
                } else {
                    log.warn("RepositoryException exception while trying to create link report to handle with uuid '{}' : {}",entry.getKey(), e.toString());
                }
            }

        }

        long testingLinksTook = (System.currentTimeMillis() - start);

        log.info("Testing all external links and writing brokenlinks to handle nodes where needed took {} seconds", String.valueOf(testingLinksTook/1000.0));
    }

    /**
     * @return the first ancestor node of <code>node</code> of type hippo:handle and <code>null</code> if no such ancestor exists
     */
    private Node getHandleNode(final Node node) throws RepositoryException {
        Node parent = node.getParent();
        if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
            return parent;
        }
        if (parent.isSame(parent.getSession().getRootNode())) {
            return null;
        }
        return getHandleNode(parent);
    }

    @Override
    public void checkLinks(CronExpression cronExpression) throws WorkflowException, RepositoryException, RemoteException {
        final WorkflowContext cronContext = getWorkflowContext().getWorkflowContext(cronExpression);
        final CheckBrokenLinksWorkflow workflow = (CheckBrokenLinksWorkflow)cronContext.getWorkflow("brokenlinks");
        workflow.checkLinks();
    }
}
