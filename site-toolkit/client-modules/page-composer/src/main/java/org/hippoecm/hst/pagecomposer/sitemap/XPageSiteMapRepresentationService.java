/*
 * Copyright 2022 Bloomreach
 */
package org.hippoecm.hst.pagecomposer.sitemap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import com.google.common.cache.CacheBuilder;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils;
import org.hippoecm.hst.platform.api.model.InternalHstModel;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_XPAGE_MIXIN;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

public class XPageSiteMapRepresentationService {

    private final static Logger log = LoggerFactory.getLogger(XPageSiteMapRepresentationService.class);
    private final static Random random = new Random();
    private Session hstConfigUser = null;

    private Set<DocVariantEvent> docVariantEvents = new HashSet<>();

    // we only need to listen to newly added, removed or moved xpages : we do not need to listen to MOVED events as they
    // also result in a removed and added event
    private static final int EVENT_TYPES = NODE_ADDED | NODE_REMOVED;

    private final Map<Object, XPageSiteMapTreeItem> cache =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(1, TimeUnit.DAYS)
                    // we assume a maximum of 1000 channels
                    .maximumSize(1000L)
                    .<Object, XPageSiteMapTreeItem>build().asMap();

    private final Map<String, Object> configurationToIdentityObjectCache = new HashMap<>();

    private Repository repository;
    private Credentials credentials;
    private String rootContentPath;

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    public void setRootContentPath(final String rootContentPath) {
        this.rootContentPath = rootContentPath;
    }

    private void init() throws RepositoryException {
        hstConfigUser = repository.login(credentials);

        // although XPages normally always live below a NT_XPAGE_FOLDER, you can store them outside an XPage folder
        // hence we listen to events below NT_FOLDER
        hstConfigUser.getWorkspace().getObservationManager().addEventListener(new XPageDocumentListener(docVariantEvents),
                EVENT_TYPES, rootContentPath, true, null, new String[]{HippoStdNodeType.NT_FOLDER}, false);

    }

    private void destroy() {
        if (hstConfigUser != null && hstConfigUser.isLive()) {
            hstConfigUser.logout();
        }
    }

    public XPageSiteMapTreeItem getSiteMapTree(final Mount previewMount, final InternalHstModel hstModel) {

        return loadRootSiteMapItem(previewMount, hstModel);

    }

    private XPageSiteMapTreeItem loadRootSiteMapItem(final Mount previewMount, final InternalHstModel hstModel) {


        // note the configurationPath can also be for a branch: although all xpage documents (without user session
        // filtering) are the same for all branches, the URLs for them might be different as a branch can have a different
        // hst:sitemap configuration resulting in different SiteMap
        final String configurationPath = previewMount.getHstSite().getConfigurationPath();


        log.debug("get XPageSiteMapTreeItem for '{}'", configurationPath);

        // in case the sitemap configuration identity changes, it means the stored jcr hst sitemap config has changed
        // and a new root XPageSiteMapTreeItem must be loaded
        final Object siteMapConfigurationIdentity = hstModel.getSiteMapConfigurationIdentity(configurationPath);

        final XPageSiteMapTreeItem siteMapTreeItem = cache.get(siteMapConfigurationIdentity);
        if (siteMapTreeItem != null) {

            final String channelContentPathPrefix = previewMount.getContentPath() + "/";
            synchronized (this) {
                final List<DocVariantEvent> channelHandleDocumentEvents = docVariantEvents.stream()
                        .filter(event -> event.absolutePath.startsWith(channelContentPathPrefix))
                        .collect(Collectors.toList());

                docVariantEvents.removeAll(channelHandleDocumentEvents);


                final List<DocVariantEvent> relevantEvents = channelHandleDocumentEvents.stream().filter(event -> {
                    if (event.type == NODE_REMOVED) {
                        // document removed, might have been an XPage document, we can't be sure. Only if it was present
                        // in the EXISTING siteMapTreeItem (as descendant), then we know it must be been an XPage document.

                        if (event.absolutePath.equals(siteMapTreeItem.getAbsoluteJcrPath())) {
                            // root changed
                            return true;
                        }

                        final boolean presentInSiteMapTree = siteMapTreeItem.getRandomOrderXPageDescendants().stream().anyMatch(descendant ->
                                event.absolutePath.equals(descendant.getAbsoluteJcrPath()));

                        if (presentInSiteMapTree) {
                            log.info("Invalidating SiteMap for '{}' as the document '{}' has been removed for it", configurationPath, event.absolutePath);
                            return true;
                        }
                        log.info("The removed document was not present in the XPage SiteMap Tree hence was not an XPage");
                        return false;

                    }
                    if (event.type == NODE_ADDED) {
                        try {
                            final Node node = hstConfigUser.getNode(event.absolutePath);
                            if (XPageUtils.isXPageDocument(node)) {
                                log.info("Invalidating SiteMap for '{}' as the XPage document '{}' has been added for it", configurationPath, event.absolutePath);
                                return true;
                            }
                            log.info("Added document '{}' is not an XPage document, ignoring it", event.absolutePath);
                            return false;
                        } catch (PathNotFoundException e) {
                            log.info("Added node '{}' already removed again. Can be ignored", event.absolutePath);
                            return false;
                        } catch (RepositoryException e) {
                            log.error("Exception while handling added node '{}'. Invalidating cached", event.absolutePath);
                            return true;
                        }
                    }

                    log.error("Unexpected event type '{}' for '{}'", event.type, event.absolutePath);
                    // invalidate just to be sure
                    return true;

                }).collect(Collectors.toList());


                if (relevantEvents.isEmpty()) {
                    return siteMapTreeItem;
                } else {
                    // there have been an XPage document added / removed / moved : reload the entire XPageSiteMapTreeItem
                    // TODO in the future we can implement more finegrained reloading instead of completely discarding
                    //  but this is not trivial (though doable)
                    cache.remove(siteMapConfigurationIdentity);
                }
            }


        }

        synchronized (this) {

            // Full reload of the ROOT XPageSiteMapTreeItem : this happens on first load of a channel (branch) SiteMap
            // or in case the channel hst sitemap (routes) changed
            log.debug("Loading XPageSiteMapTreeItem for '{}' as not present in cache", configurationPath);

            final XPageSiteMapTreeItem newSiteMapTreeItem = cache.get(siteMapConfigurationIdentity);
            if (newSiteMapTreeItem != null) {
                return newSiteMapTreeItem;
            }

            // the old hst sitemap (routes) identity
            final Object identity = configurationToIdentityObjectCache.remove(configurationPath);
            if (identity != null) {
                cache.remove(identity);
            }

            // purge all outdated / stale entries from cache for outdated hst sitemap (routes) identities
            expungeStaleEntries(hstModel);

            try {

                final QueryResult xPageDocuments = getUnpublishedXPageDocVariants(previewMount, hstConfigUser);

                XPageSiteMapTreeItem root = new XPageSiteMapTreeItem();

                final HstLinkCreator hstLinkCreator = hstModel.getHstLinkCreator();

                for (Node unpublishedVariant : new NodeIterable(xPageDocuments.getNodes())) {
                    final Node handle = unpublishedVariant.getParent();
                    if (!unpublishedVariant.isNodeType(NT_DOCUMENT) || !handle.isNodeType(NT_HANDLE)) {
                        log.info("Skipping unexpected node '{}' with mixin '{}' : only document variants are expected",
                                unpublishedVariant.getPath(), MIXINTYPE_HST_XPAGE_MIXIN);
                        continue;
                    }

                    // use the link creator to get hold of URLs for the XPages
                    // Note this is very delicate: we only try to resolve the XPages *WITHIN* the current channel (editingMount),
                    // hence we use create(Node node, Mount editingMount) : if no link can be found, we just skip (for now?)
                    // the XPage!

                    final HstLink hstLink = hstLinkCreator.create(handle, previewMount);
                    if (hstLink.isNotFound()) {
                        log.info("Skipping XPage '{}' since cannot be represented in channel '{}'", handle.getPath(),
                                previewMount.getChannel().getName());
                        continue;
                    }

                    try {
                        XPageSiteMapTreeItem xPageSiteMapTreeItem = createXPageSiteMapTreeItem(hstLink, (HippoNode) handle);

                        if (StringUtils.isEmpty(xPageSiteMapTreeItem.getPathInfo())) {
                            // channel home page, replace the root!
                            xPageSiteMapTreeItem.setChildren(root.getChildren());
                            xPageSiteMapTreeItem.getChildren().values().forEach(child -> child.setParent(xPageSiteMapTreeItem));
                            root = xPageSiteMapTreeItem;
                        } else {
                            place(root, xPageSiteMapTreeItem, xPageSiteMapTreeItem.getPathInfo().split("/"), 0);
                        }

                    } catch (RepositoryException e) {
                        log.error("Could not create XPageSiteMapTreeItem for '%s'", JcrUtils.getNodePathQuietly(handle));
                    }

                }

                configurationToIdentityObjectCache.put(configurationPath, siteMapConfigurationIdentity);
                cache.put(siteMapConfigurationIdentity, root);
                root.optimize();

                return root;

            } catch (RepositoryException e) {
                throw new RuntimeRepositoryException(e);
            }
        }
    }

    private void expungeStaleEntries(final InternalHstModel hstModel) {
        configurationToIdentityObjectCache.entrySet().removeIf(entry -> {
            try {
                final Object updatedIdentity = hstModel.getSiteMapConfigurationIdentity(entry.getKey());
                if (updatedIdentity != entry.getValue()) {
                    // the identity of stored sitemap has changed, purge from cache
                    log.info("Purge the cache for '{}' as the backing HST sitemap has been changed in the meantime");
                    cache.remove(entry.getValue());
                    return true;
                }
            } catch (ModelLoadingException e) {
                log.info("Cannot find hst configuration any more for '{}'. Most likely removed branch or deleted channel", entry.getKey());
                cache.remove(entry.getValue());
                return true;
            }
            return false;
        });

        final Collection<Object> values = configurationToIdentityObjectCache.values();

        // purge all values from the cache for which there is no configurationToIdentityObjectCache value : in general
        // this should not happen but as the cached entries can be large, better safe than sorry
        final Set<Object> keys = cache.keySet();

        for (Object key : keys) {
            if (!values.contains(key)) {
                log.error("Unexpected cache entry present as not present in the 'configurationToIdentityObjectCache' map. " +
                        "Removing the cached entry now. This is a programming issue");
                cache.remove(key);
            }
        }


    }

    private void place(final XPageSiteMapTreeItem parent, final XPageSiteMapTreeItem toAdd, String[] pathElements, final int position) {

        final XPageSiteMapTreeItem child = parent.getChildren().computeIfAbsent(pathElements[position], key -> new XPageSiteMapTreeItem());

        child.setParent(parent);

        if (position == pathElements.length - 1) {
            child.setPathInfo(toAdd.getPathInfo());
            child.setAbsoluteJcrPath(toAdd.getAbsoluteJcrPath());

            if (toAdd.getAbsoluteJcrPath() != null) {
                XPageSiteMapTreeItem ancestor = parent;
                while (ancestor != null) {
                    // add availabel XPage backed descendants in random order: this is because we want random descendant
                    // xpage docs checking for finding out whether some user is allowed to see a structural sitemap folder
                    ancestor.getRandomOrderXPageDescendants()
                            .add(ancestor.getRandomOrderXPageDescendants().size() == 0 ? 0 : random.nextInt(ancestor.getRandomOrderXPageDescendants().size()), child);
                    ancestor = ancestor.getParent();
                }
            }
        } else {
            // do set the pathInfo as we are dealing with a structural sitemap tree item
            if (StringUtils.isEmpty(parent.getPathInfo())) {
                child.setPathInfo(pathElements[position]);
            } else {
                child.setPathInfo(parent.getPathInfo() + "/" + pathElements[position]);
            }
            place(child, toAdd, pathElements, position + 1);
        }
    }

    private XPageSiteMapTreeItem createXPageSiteMapTreeItem(final HstLink hstLink, final HippoNode handle) throws RepositoryException {

        final XPageSiteMapTreeItem xPageSiteMapTreeItem = new XPageSiteMapTreeItem();
        xPageSiteMapTreeItem.setPathInfo(hstLink.getPath());
        xPageSiteMapTreeItem.setAbsoluteJcrPath(handle.getPath());
        return xPageSiteMapTreeItem;
    }

    public static QueryResult getUnpublishedXPageDocVariants(final Mount previewMount, final Session session) throws RepositoryException {
        final Node contentRoot = session.getNode(previewMount.getContentPath());

        // do not use the content root path as scope since this results in *slow* queries *and* if the
        // content root node name starts with a number, you need to escape it, which is unhandy
        final String statement = format("//element(*,%s)[@hippo:paths = '%s' and @hippo:availability = 'preview']",
                MIXINTYPE_HST_XPAGE_MIXIN, contentRoot.getIdentifier());

        final Query xPagesQuery = session.getWorkspace().getQueryManager().createQuery(statement, "xpath");

        final QueryResult xPageDocuments = xPagesQuery.execute();
        return xPageDocuments;
    }


    private static class XPageDocumentListener implements EventListener {

        private Set<DocVariantEvent> docVariantEvents;

        public XPageDocumentListener(final Set<DocVariantEvent> docVariantEvents) {

            this.docVariantEvents = docVariantEvents;
        }

        @Override
        public synchronized void onEvent(final EventIterator events) {
            while (events.hasNext()) {
                try {
                    final Event event = events.nextEvent();
                    // the event path is of the document variant : we need the handle
                    docVariantEvents.add(new DocVariantEvent(event.getPath(), event.getType()));
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class DocVariantEvent {
        private final int type;
        private final String absolutePath;

        DocVariantEvent(final String absolutePath, final int type) {
            this.absolutePath = absolutePath;
            this.type = type;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final DocVariantEvent that = (DocVariantEvent) o;
            return type == that.type &&
                    absolutePath.equals(that.absolutePath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, absolutePath);
        }
    }
}
