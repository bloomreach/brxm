/**
 * Copyright 2013-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.brokenlinks;

import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BrokenLinksCheckingJob
 * <P>
 * This job implementation is instantiated by Hippo Repository Scheduler Service
 * based on the job information provided by the broken links checker daemon module.
 * </P>
 */
public class BrokenLinksCheckingJob implements RepositoryJob {

    private static Logger log = LoggerFactory.getLogger(BrokenLinksCheckingJob.class);

    @Override
    public void execute(RepositoryJobExecutionContext context) throws RepositoryException {
        log.info("BrokenLinksCheckingJob begins ...");
        long start = System.currentTimeMillis();

        Session session = null;

        try {
            session = context.getSystemSession();
            session.refresh(false);
            Map<String, String> params = new HashMap<String, String>();

            for (String attrName : context.getAttributeNames()) {
                params.put(attrName, context.getAttribute(attrName));
            }
            checkBrokenLinks(session, new CheckExternalBrokenLinksConfig(params));
        } finally {
            if (session != null) {
                session.logout();
            }

            log.info("BrokenLinksCheckingJob ends, spending {} seconds.", (System.currentTimeMillis() - start) / 1000.0);
        }
    }

    private void checkBrokenLinks(final Session session, final CheckExternalBrokenLinksConfig config) throws RepositoryException {
        final WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();

        final LinkChecker linkChecker = new LinkChecker(config, session);
        log.info("Checking broken external links, configuration: ", config);
        // For the xpath query below, do not include a path constraint to begin with, like
        // /jcr:root/content/documents as this results in much less efficient queries
        String xpath = "//element(*,hippostd:html)";
        QueryResult result = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH).execute();
        NodeIterator hippostdHtmlNodes =  result.getNodes();


        // the key of the links map is the uuid of the handle node
        Map<String,Set<Link>> linksByHandleUUID = new HashMap<String, Set<Link>>();
        // the unique links by URL
        Map<String,Link> linksByURL = new HashMap<String, Link>();

        long start = System.currentTimeMillis();
        int count = 0;
        int totalLinksCount = 0;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        while (hippostdHtmlNodes.hasNext()) {
            try {
                Node hippostdHtml = hippostdHtmlNodes.nextNode();

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
                md.update(content.getBytes());

                try {
                    Set<Link> linksForHandle = linksByHandleUUID.get(handleUUID);
                    count++;

                    final List<String> links = PlainTextLinksExtractor.getLinks(content);
                    totalLinksCount += links.size();
                    for (String url : links) {

                        if (isExcludedURL(config, url)) {
                            log.info("The URL is excluded while broken links checking in '{}': {}", hippostdHtml.getPath(), url);
                            continue;
                        }

                        if (linksForHandle == null) {
                            linksForHandle = new HashSet<Link>();
                            linksByHandleUUID.put(handleUUID, linksForHandle);
                        }

                        Link alreadyPresent = linksByURL.get(url);

                        if (alreadyPresent == null) {
                            String sourceNodeIdentifier = hippostdHtml.getIdentifier();
                            Link link = new Link(url, sourceNodeIdentifier);

                            if (StringUtils.startsWithIgnoreCase(url, "http:") || StringUtils.startsWithIgnoreCase(url, "https:")) {
                                linksByURL.put(url, link);
                            } else {
                                linksByURL.put(sourceNodeIdentifier + "/" + url, link);
                            }

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
        log.info("In total {}  hippostd:html nodes were scanned.", String.valueOf(count));
        log.info("In total {} handles have links", linksByHandleUUID.size());
        log.info("In total there are {} unique links", linksByURL.size());
        final StringBuilder sb = new StringBuilder();
        for (final byte b : md.digest()) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        log.info("Digest of text processed: {}", sb.toString());
        log.info("Total amount of links counted: {}", totalLinksCount);
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
                Node handleNode = session.getNodeByIdentifier(entry.getKey());
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
                        Workflow reportWorkflow = workflowManager.getWorkflow("brokenlinks", new Document(doc));
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
                    } catch (RemoteException e) {
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

    private boolean isExcludedURL(final CheckExternalBrokenLinksConfig config, final String url) {
        for (Pattern excludePattern : config.getUrlExcludePatterns()) {
            Matcher m = excludePattern.matcher(url);

            if (m.matches()) {
                return true;
            }
        }

        return false;
    }

}
