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
package org.hippoecm.hst.platform.linking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.linking.CompositeHstLinkCreator;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.linking.LocationResolver;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelImpl;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeHstLinkCreatorImpl implements CompositeHstLinkCreator {

    private static final Logger log = LoggerFactory.getLogger(CompositeHstLinkCreator.class);
    private final HstLinkCreator hstLinkCreator;
    private final HstModelRegistry modelRegistry;

    public CompositeHstLinkCreatorImpl(final HstModelRegistry modelRegistry, final HstLinkCreator hstLinkCreator) {
        this.modelRegistry = modelRegistry;
        this.hstLinkCreator = hstLinkCreator;
    }

    public HstLinkCreator getLocalHstLinkCreator() {
        return hstLinkCreator;
    }

    @Override
    public HstLink create(final String uuid, final Session session, final HstRequestContext requestContext) {
        final HstLink hstLink = hstLinkCreator.create(uuid, session, requestContext);

        if (hstLink == null || hstLink.isNotFound()) {

            final Node node;
            try {
                node = session.getNodeByIdentifier(uuid);
            } catch (ItemNotFoundException e) {
                log.info("Node with uuid '{}' cannot be found. Cannot create a HstLink, return null", uuid);
                return hstLink;
            } catch (RepositoryException e) {
                log.warn("RepositoryException Cannot create a HstLink, return null", uuid);
                return hstLink;
            }

            final HstLink canonicalLink = getCanonicalLinkFromOtherModels(node, requestContext.getVirtualHost().getHostGroupName());
            if (canonicalLink != null) {
                return canonicalLink;
            }

        }
        return hstLink;
    }

    @Override
    public HstLink create(final Node node, final HstRequestContext requestContext) {
        final HstLink hstLink = hstLinkCreator.create(node, requestContext);
        return getFirstCrossWebAppLinkIfNeeded(node, hstLink, requestContext.getVirtualHost().getHostGroupName());
    }

    @Override
    public HstLink create(final Node node, final HstRequestContext requestContext, final HstSiteMapItem preferredItem, final boolean fallback) {
        final HstLink hstLink = hstLinkCreator.create(node, requestContext, preferredItem, fallback);
        if (!fallback) {
            return hstLink;
        }

        final String hostGroupName = requestContext.getVirtualHost().getHostGroupName();
        return getFirstCrossWebAppLinkIfNeeded(node, hstLink, hostGroupName);
    }

    @Override
    public HstLink create(final Node node, final HstRequestContext requestContext, final HstSiteMapItem preferredItem, final boolean fallback, final boolean navigationStateful) {
        final HstLink hstLink = hstLinkCreator.create(node, requestContext, preferredItem, fallback, navigationStateful);
        if (!fallback) {
            return hstLink;
        }

        final String hostGroupName = requestContext.getVirtualHost().getHostGroupName();
        return getFirstCrossWebAppLinkIfNeeded(node, hstLink, hostGroupName);
    }

    @Override
    public HstLink create(final Node node, final Mount mount, final boolean crossMount) {
        final HstLink hstLink = hstLinkCreator.create(node, mount, crossMount);
        if (!crossMount) {
            return hstLink;
        }

        final String hostGroupName = mount.getVirtualHost().getHostGroupName();
        return getFirstCrossWebAppLinkIfNeeded(node, hstLink, hostGroupName);
    }

    @Override
    public HstLink create(final Node node, final Mount mount, final HstSiteMapItem preferredItem, final boolean fallback) {
        final HstLink hstLink = hstLinkCreator.create(node, mount, preferredItem, fallback);
        if (!fallback) {
            return hstLink;
        }

        final String hostGroupName = mount.getVirtualHost().getHostGroupName();
        return getFirstCrossWebAppLinkIfNeeded(node, hstLink, hostGroupName);
    }

    @Override
    public HstLink create(final Node node, final HstRequestContext requestContext, final String mountAlias) {
        //CrossOff
        return hstLinkCreator.create(node, requestContext, mountAlias);
    }

    @Override
    public HstLink create(final Node node, final Mount mount) {
        //CrossOff
        return hstLinkCreator.create(node, mount);
    }

    @Override
    public HstLink create(final Node node, final HstRequestContext requestContext, final String mountAlias, final String type) {
        //CrossOff
        return hstLinkCreator.create(node, requestContext, mountAlias, type);
    }

    @Override
    public HstLink create(final HippoBean bean, final HstRequestContext requestContext) {
        final HstLink hstLink = hstLinkCreator.create(bean, requestContext);

        final String hostGroupName = requestContext.getVirtualHost().getHostGroupName();
        return getFirstCrossWebAppLinkIfNeeded(bean.getNode(), hstLink, hostGroupName);
    }

    @Override
    public HstLink create(final HstSiteMapItem toHstSiteMapItem, final Mount mount) {
        //CrossOff
        return hstLinkCreator.create(toHstSiteMapItem, mount);
    }

    @Override
    public HstLink create(final String path, final Mount mount) {
        //CrossOff
        return hstLinkCreator.create(path, mount);
    }

    @Override
    public HstLink create(final String path, final Mount mount, final boolean containerResource) {
        //CrossOff
        return hstLinkCreator.create(path, mount, containerResource);
    }

    @Override
    public HstLink createByRefId(final String siteMapItemRefId, final Mount mount) {
        //CrossOff
        return hstLinkCreator.createByRefId(siteMapItemRefId, mount);
    }

    @Override
    public HstLink createCanonical(final Node node, final HstRequestContext requestContext) {
        final HstLink hstLink = hstLinkCreator.createCanonical(node, requestContext);
        final String hostGroupName = requestContext.getVirtualHost().getHostGroupName();
        return getFirstCrossWebAppLinkIfNeeded(node, hstLink, hostGroupName);
    }

    @Override
    public HstLink createCanonical(final Node node, final HstRequestContext requestContext, final HstSiteMapItem preferredItem) {
        return hstLinkCreator.createCanonical(node, requestContext, preferredItem);
    }

    @Override
    public List<HstLink> createAllAvailableCanonicals(final Node node, final HstRequestContext requestContext) {
        final Mount mount = requestContext.getResolvedMount().getMount();
        return createAllAvailableCanonicals(node, mount, mount.getType(), mount.getVirtualHost().getHostGroupName());
    }

    @Override
    public List<HstLink> createAllAvailableCanonicals(final Node node, final HstRequestContext requestContext, final String type) {
        final Mount mount = requestContext.getResolvedMount().getMount();
        return createAllAvailableCanonicals(node, mount, type, mount.getVirtualHost().getHostGroupName());
    }

    @Override
    public List<HstLink> createAllAvailableCanonicals(final Node node, final HstRequestContext requestContext, final String type, final String hostGroupName) {
        final Mount mount = requestContext.getResolvedMount().getMount();
        return createAllAvailableCanonicals(node, mount, type, hostGroupName);
    }

    @Override
    public List<HstLink> createAllAvailableCanonicals(final Node node, final Mount mount, final String type, final String hostGroupName) {

        final String mountType = type == null ? mount.getType() : type;
        final List<HstLink> hstLinks = new ArrayList<>(hstLinkCreator.createAllAvailableCanonicals(node, mount, mountType, hostGroupName));

        //Get all models except the model current link creator belongs to
        final List<HstModel> hstModels = getOtherHstModels();
        for (final HstModel hstModel : hstModels) {

            //Get first mount within specified hostGroupName
            final Optional<Mount> firstMount = hstModel.getVirtualHosts()
                    .getMountsByHostGroup(hostGroupName).stream()
                    .findFirst();

            firstMount.ifPresent(m -> {
                //Invoke simple link creator
                final HstLinkCreator hstLinkCreator = ((CompositeHstLinkCreator) hstModel.getHstLinkCreator()).getLocalHstLinkCreator();
                hstLinks.addAll(hstLinkCreator.createAllAvailableCanonicals(node, m, mountType, hostGroupName));
            });
        }

        //Sort aggregated link collection
        hstLinks.sort(new DefaultHstLinkCreator.LowestDepthFirstAndThenLexicalComparator());
        return hstLinks;
    }

    @Override
    public List<HstLink> createAll(final Node node, final HstRequestContext requestContext, final boolean crossMount) {

        final Mount mount = requestContext.getResolvedMount().getMount();
        final String hostGroupName = mount.getVirtualHost().getHostGroupName();
        return createAll(node, mount, hostGroupName, mount.getType(), crossMount);
    }

    @Override
    public List<HstLink> createAll(final Node node, final HstRequestContext requestContext, final String hostGroupName, final String type, final boolean crossMount) {
        final Mount mount = requestContext.getResolvedMount().getMount();
        return createAll(node, mount, hostGroupName, type, crossMount);
    }

    @Override
    public List<HstLink> createAll(final Node node, final Mount mount, final String hostGroupName, final String type, final boolean crossMount) {

        final List<HstLink> hstLinks = new ArrayList<>(hstLinkCreator.createAll(node, mount, hostGroupName, type, crossMount));

        if (!crossMount) {
            hstLinks.sort(new DefaultHstLinkCreator.LowestDepthFirstAndThenLexicalComparator());
            return hstLinks;
        }

        final List<HstModel> hstModels = getOtherHstModels();
        for (final HstModel hstModel : hstModels) {

            //Get first mount within specified hostGroupName
            final Optional<Mount> firstMount = hstModel.getVirtualHosts()
                    .getMountsByHostGroup(hostGroupName).stream()
                    .findFirst();

            firstMount.ifPresent(m -> {
                //Invoke local link creator
                final HstLinkCreator hstLinkCreator = ((CompositeHstLinkCreator) hstModel.getHstLinkCreator()).getLocalHstLinkCreator();
                hstLinks.addAll(hstLinkCreator.createAll(node, m, hostGroupName, type, true));
            });
        }

        //Sort aggregated link collection
        hstLinks.sort(new DefaultHstLinkCreator.LowestDepthFirstAndThenLexicalComparator());
        return hstLinks;
    }

    @Override
    public HstLink createPageNotFoundLink(final Mount mount) {
        return hstLinkCreator.createPageNotFoundLink(mount);
    }

    @Override
    public boolean isBinaryLocation(final String path) {
        return hstLinkCreator.isBinaryLocation(path);
    }

    @Override
    public String getBinariesPrefix() {
        return hstLinkCreator.getBinariesPrefix();
    }

    @Override
    public List<LocationResolver> getLocationResolvers() {
        return hstLinkCreator.getLocationResolvers();
    }

    @Override
    public void clear() {
        hstLinkCreator.clear();
    }

    private HstLink getCanonicalLinkFromOtherModels(final Node node, final String hostGroupName) {
        final List<HstModel> hstModels = getOtherHstModels();
        for (final HstModel hstModel : hstModels) {
            final Optional<Mount> firstMount = hstModel.getVirtualHosts()
                    .getMountsByHostGroup(hostGroupName).stream()
                    .findFirst();

            if (firstMount.isPresent()) {
                final HstLinkCreator localHstLinkCreator = ((CompositeHstLinkCreator) hstModel.getHstLinkCreator()).getLocalHstLinkCreator();
                final HstLink canonicalLink = localHstLinkCreator.create(node, firstMount.get(), true);
                if (canonicalLink != null && !canonicalLink.isNotFound()) {
                    return canonicalLink;
                }
            }
        }
        return null;
    }

    private HstLink getFirstCrossWebAppLinkIfNeeded(final Node node, final HstLink hstLink, final String hostGroupName) {
        if (hstLink == null || hstLink.isNotFound()) {
            final HstLink canonicalLink = getCanonicalLinkFromOtherModels(node, hostGroupName);
            if (canonicalLink != null) {
                return canonicalLink;
            }
        }
        return hstLink;
    }

    /**
     * Get all HST models sorted by configuration root path, except the model current link creator belongs to
     * @return Collection of HST models
     */
    private List<HstModel> getOtherHstModels() {
        return modelRegistry.getHstModels().stream()
                .filter(m -> m.getHstLinkCreator() != this)
                .sorted(Comparator.comparing(m -> ((HstModelImpl)m).getConfigurationRootPath()))
                .collect(Collectors.toList());
    }

}