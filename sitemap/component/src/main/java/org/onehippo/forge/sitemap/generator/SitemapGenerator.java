/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.sitemap.generator;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenuItem;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.onehippo.forge.sitemap.components.UrlInformationProvider;
import org.onehippo.forge.sitemap.components.beans.SiteMap;
import org.onehippo.forge.sitemap.components.beans.SiteMapItem;
import org.onehippo.forge.sitemap.components.model.ChangeFrequency;
import org.onehippo.forge.sitemap.components.model.SiteMapCharacterEscapeHandler;
import org.onehippo.forge.sitemap.components.model.Url;
import org.onehippo.forge.sitemap.components.model.Urlset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.synchronizedList;
import static java.util.Collections.synchronizedMap;

public class SitemapGenerator {

    private final HstRequestContext requestContext;
    private final ObjectConverter objectConverter;
    private final UrlInformationProvider urlInformationProvider;
    private int queriesFired;
    private int queryCacheHits;

    private final Map<String, List<String>> queriesWithResultingPaths;
    private final List<String> siteMapRefIdsToExcludeFromSiteMap;
    private final List<String> componentConfigurationIdsToExcludeFromSiteMap;
    private final List<String> siteMapPathsToExcludeFromSiteMap;

    private final List<WorkItem> workItemQueue;
    private final List<WorkItem> workItemsInProgress;

    private final Mount mount;

    private final Object waitObj = new Object();

    private static final String UNUSED = "unused";
    private static final int DEFAULT_AMOUNT_OF_WORKERS = 4;
    private static final long MS_TO_WAIT_FOR_UPDATES = 1000;

    private static final Logger LOG = LoggerFactory.getLogger(SitemapGenerator.class);
    private boolean errorOccured = false;
    private Exception lastWorkerException = null;
    private List<SitemapGeneratorWorker> workers;
    private int amountOfWorkers = DEFAULT_AMOUNT_OF_WORKERS;

    public SitemapGenerator(HstRequestContext requestContext, ObjectConverter objectConverter) {
        this(requestContext, objectConverter, new DefaultUrlInformationProvider());
    }

    public SitemapGenerator(HstRequestContext requestContext, ObjectConverter objectConverter,
                            UrlInformationProvider urlInformationProvider) {
        this(requestContext, objectConverter, urlInformationProvider, requestContext.getResolvedMount().getMount());
    }

    public SitemapGenerator(final HstRequestContext requestContext, final ObjectConverter objectConverter,
                            final UrlInformationProvider urlInformationProvider, Mount mount) {
        this.objectConverter = objectConverter;
        this.urlInformationProvider = urlInformationProvider;
        this.requestContext = requestContext;
        this.mount = mount;
        componentConfigurationIdsToExcludeFromSiteMap = new ArrayList<String>();
        siteMapRefIdsToExcludeFromSiteMap = new ArrayList<String>();
        siteMapPathsToExcludeFromSiteMap = new ArrayList<String>();
        queriesWithResultingPaths = synchronizedMap(new HashMap<String, List<String>>());
        workItemQueue = synchronizedList(new ArrayList<WorkItem>());
        workItemsInProgress = new ArrayList<WorkItem>();
        queriesFired = 0;
        queryCacheHits = 0;
    }

    @SuppressWarnings({UNUSED})
    public String createSitemap(final HstSiteMenu sitemenu, final int maxDepth) {
        Urlset urls = new Urlset();
        addSitemapItems(sitemenu, urls, maxDepth);
        return toString(urls);
    }

    public String createSitemap(final HstSiteMenus sitemenus, final int maxDepth) {
        Urlset urls = new Urlset();
        for (HstSiteMenu menu : sitemenus.getSiteMenus().values()) {
            addSitemapItems(menu, urls, maxDepth);
        }
        return toString(urls);
    }

    @SuppressWarnings({UNUSED})
    protected void addSitemapItems(final HstSiteMenus sitemenus, final Urlset urls, final int maxDepth) {
        for (HstSiteMenu menu : sitemenus.getSiteMenus().values()) {
            addSitemapItems(menu, urls, maxDepth);
        }
    }

    protected void addSitemapItems(final HstSiteMenu sitemenu, final Urlset urls, final int maxDepth) {
        Calendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        for (HstSiteMenuItem item : sitemenu.getSiteMenuItems()) {
            addMenuItem(item, urls, 1, maxDepth, now);
        }
    }

    protected void addMenuItem(final HstSiteMenuItem item, final Urlset urlSet, final int depth, final int maxDepth,
                               final Calendar defaultLastModifedDate) {
        Url url = new Url();
        if (item.getHstLink() != null) {
            url.setChangeFrequency(ChangeFrequency.DAILY);
            url.setLastmod(defaultLastModifedDate);
            url.setLoc(item.getHstLink().toUrlForm(requestContext, true));
            url.setPriority(new BigDecimal("1.0"));
            urlSet.getUrls().add(url);
        }

        if (depth < maxDepth) {
            for (HstSiteMenuItem childItem : item.getChildMenuItems()) {
                addMenuItem(childItem, urlSet, depth + 1, maxDepth, defaultLastModifedDate);
            }
        }
    }

    public static String toString(final Urlset urls) {
        String output;
        try {
            JAXBContext jc = JAXBContext.newInstance(Urlset.class, Url.class);
            Marshaller m = jc.createMarshaller();

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            TransformerHandler handler = ((SAXTransformerFactory)SAXTransformerFactory.newInstance()).newTransformerHandler();
            Transformer transformer = handler.getTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
            handler.setResult(new StreamResult(out));

            m.marshal(urls, new SiteMapCharacterEscapeHandler(handler));
            output = out.toString();
        } catch (JAXBException | TransformerConfigurationException e) {
            throw new IllegalStateException("Cannot marshal the Urlset to an XML string", e);
        }
        return output;
    }

    public static SiteMap getSitemapView(final HstSiteMenus sitemenus, final int maxDepth) {
        SiteMap map = new SiteMap();
        for (HstSiteMenu menu : sitemenus.getSiteMenus().values()) {
            addSitemapItems(menu, map, maxDepth);
        }
        return map;
    }

    public static void addSitemapItems(final HstSiteMenu sitemenu, final SiteMap map, final int maxDepth) {
        for (HstSiteMenuItem item : sitemenu.getSiteMenuItems()) {
            addMenuItem(item, map, 1, maxDepth, null);
        }
    }

    public static void addMenuItem(final HstSiteMenuItem item, final SiteMap map, final int depth, final int maxDepth,
                                   final String parent) {

        SiteMapItem mapItem = new SiteMapItem(item.getName(), item.getHstLink().getPath());
        String newParent;
        if (parent != null && map.getItems().containsKey(parent)) {
            map.getItems().get(parent).add(mapItem);
            newParent = item.getName();
        } else {
            List<SiteMapItem> sitemMapItems = new ArrayList<SiteMapItem>();
            sitemMapItems.add(mapItem);
            map.getItems().put(item.getName(), sitemMapItems);
            newParent = item.getName();
        }
        if (depth < maxDepth) {
            for (HstSiteMenuItem childItem : item.getChildMenuItems()) {
                addMenuItem(childItem, map, depth + 1, maxDepth, newParent);
            }
        }
    }

    /**
     * Creates a new {@link Urlset} based upon the current {@link HstSiteMap} resolved from the current
     * {@link HstRequestContext}
     *
     * @return The Urlset containing the urls of the sitemap items
     */
    public Urlset createUrlSetBasedOnHstSiteMap() {
        Urlset urlset = new Urlset();

        createAndStartWorkers(urlset);

        fillInitialWorkQueue();

        boolean allWorkDone = waitUntilAllWorkItemsAreFinished();

        if (!allWorkDone) {
            // We might have been interrupted
            return null;
        }

        stopWorkers();

        if (errorOccured) {
            throw new IllegalStateException("Error occurred while trying to generate the site map", lastWorkerException);
        }

        return urlset;
    }

    private void stopWorkers() {
        for (SitemapGeneratorWorker worker : workers) {
            worker.interrupt();
        }
    }

    private boolean waitUntilAllWorkItemsAreFinished() {
        // Wait until all work items have been processed or an error occurs
        while ((!workItemQueue.isEmpty() || !workItemsInProgress.isEmpty()) && !errorOccured) {
            try {
                synchronized (waitObj) {
                    if ((!workItemQueue.isEmpty() || workItemsInProgress.isEmpty()) && !errorOccured) {
                        waitObj.wait(MS_TO_WAIT_FOR_UPDATES);
                    }
                }
            } catch (InterruptedException e) {
                // we were interrupted, cancel
                return false;
            }
        }
        return true;
    }

    private void fillInitialWorkQueue() {

        HstSiteMap siteMap = mount.getHstSite().getSiteMap();
        for (HstSiteMapItem siteMapItem : siteMap.getSiteMapItems()) {
            addSiteMapBranchToUrlSet(siteMapItem);
        }
    }

    private List<SitemapGeneratorWorker> createAndStartWorkers(Urlset urlset) {
        // Initialize the workers
        workers = new ArrayList<SitemapGeneratorWorker>();
        for (int i = 0 ; i < amountOfWorkers; i++) {
            SitemapGeneratorWorker worker = new SitemapGeneratorWorker(
                    this, mount, urlset, requestContext,
                    objectConverter, urlInformationProvider
            );
            worker.start();
            workers.add(worker);
        }
        return workers;
    }

    /**
     * Initial method passing through an empty List, because there are no matched nodes yet
     *
     * @param siteMapItem the {@link HstSiteMapItem} to "parse"
     */
    private void addSiteMapBranchToUrlSet(final HstSiteMapItem siteMapItem) {
        WorkItem workItem = new WorkItem(siteMapItem, new ArrayList<String>());
        addWorkItem(workItem);
    }

    public boolean shouldIgnoreSiteMapItem(final HstSiteMapItem siteMapItem) {
        String refId = siteMapItem.getRefId();
        String siteMapPath = HstSiteMapUtils.getPath(siteMapItem);

        boolean shouldIgnoreBasedOnRefId = siteMapRefIdsToExcludeFromSiteMap.contains(refId);
        boolean shouldIgnoreBasedOnPath = siteMapPathsToExcludeFromSiteMap.contains(siteMapPath);

        if (LOG.isDebugEnabled()) {
            if (shouldIgnoreBasedOnRefId) {
                LOG.debug("Ignoring sitemap item \"{}\" because it has a refId \"{}\" which we exclude.",
                        siteMapItem.getId(), refId);
            }
            if (shouldIgnoreBasedOnPath) {
                LOG.debug("Ignoring sitemap item \"{}\" because it has a path \"{}\" which we exclude.",
                        siteMapItem.getId(), siteMapPath);
            }
        }

        return shouldIgnoreBasedOnPath || shouldIgnoreBasedOnRefId;
    }

    public void addSitemapRefIdExclusions(final String[] refIdsToExclude) {
        addSitemapRefIdExclusions(Arrays.asList(refIdsToExclude));
    }

    public void addSitemapRefIdExclusions(final Collection<String> refIdsToExclude) {
        siteMapRefIdsToExcludeFromSiteMap.addAll(refIdsToExclude);
    }

    @SuppressWarnings({UNUSED})
    public void addSitemapRefIdExclusion(final String sitemapRefId) {
        siteMapRefIdsToExcludeFromSiteMap.add(sitemapRefId);
    }

    public void addSitemapPathExclusions(final String[] pathsToExclude) {
        addSitemapPathExclusions(Arrays.asList(pathsToExclude));
    }

    public void addSitemapPathExclusions(final Collection<String> pathsToExclude) {
        siteMapPathsToExcludeFromSiteMap.addAll(pathsToExclude);
    }

    @SuppressWarnings({UNUSED})
    public void addSitemapPathExclusion(final String sitemapPath) {
        siteMapPathsToExcludeFromSiteMap.add(sitemapPath);
    }

    public void addComponentConfigurationIdExclusions(final Collection<String> componentConfigurationIdsToExclude) {
        componentConfigurationIdsToExcludeFromSiteMap.addAll(componentConfigurationIdsToExclude);
    }

    public void addComponentConfigurationIdExclusions(final String[] componentConfigurationIdsToExclude) {
        addComponentConfigurationIdExclusions(Arrays.asList(componentConfigurationIdsToExclude));
    }

    @SuppressWarnings({UNUSED})
    public void addComponentConfigurationIdExclusion(final String componentConfigurationId) {
        componentConfigurationIdsToExcludeFromSiteMap.add(componentConfigurationId);
    }

    public int getQueriesFired() {
        return queriesFired;
    }

    public int getQueryCacheHits() {
        return queryCacheHits;
    }

    public boolean componentConfigurationIdShouldBeExcluded(final String componentConfigurationId) {
        return componentConfigurationIdsToExcludeFromSiteMap.contains(componentConfigurationId);
    }

    public void addWorkItem(final WorkItem workItem) {
        workItemQueue.add(workItem);
    }

    public boolean queryIsCached(final String query) {
        return queriesWithResultingPaths.containsKey(query);
    }

    public List<String> getNodePathsForQueryFromCache(final String query) {
        List<String> nodePaths = queriesWithResultingPaths.get(query);
        if (nodePaths == null) {
            LOG.error("Query \"{}\" is not cached, cannot return node paths, so throwing exception.", query);
            throw new IllegalArgumentException("Query not cached, so cannot return node paths");
        }
        return nodePaths;
    }

    public void addNodePathsForQueryToCache(final String query, final List<String> nodePaths) {
        queriesWithResultingPaths.put(query, nodePaths);
    }

    /**
     * Returns the next {@link WorkItem} in the queue
     * @return {@link WorkItem} to be processed
     */
    public WorkItem getNextWorkItem() {
        synchronized (this) {
            if (!workItemQueue.isEmpty()) {
                synchronized (workItemsInProgress) {
                    WorkItem workItem = workItemQueue.remove(0);
                    workItemsInProgress.add(workItem);
                    return workItem;
                }
            } else {
                return null;
            }
        }
    }

    /**
     * Called by a worker to indicate that a work item has been processed
     * @param workItem work item that is finished
     */
    public void finishWorkItem(WorkItem workItem) {
        synchronized (workItemsInProgress) {
            workItemsInProgress.remove(workItem);
        }
    }

    public void reportErrorOccurred(Exception e) {
        errorOccured = true;
        lastWorkerException = e;
    }

    public void setAmountOfWorkers(int amountOfWorkers) {
        this.amountOfWorkers = amountOfWorkers;
    }

    protected HstRequestContext getRequestContext() {
        return requestContext;
    }
}