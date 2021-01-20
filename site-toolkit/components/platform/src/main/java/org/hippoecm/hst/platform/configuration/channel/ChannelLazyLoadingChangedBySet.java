/*
 *  Copyright 2011-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.channel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.internal.ConfigurationLockInfo;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.hst.Channel;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelLazyLoadingChangedBySet implements Set<String> {

    private static final Logger log = LoggerFactory.getLogger(ChannelLazyLoadingChangedBySet.class);

    private Set<String> delegatee;
    private transient final HstSite previewHstSite;
    private transient final Channel channel;
    private transient final HstNodeLoadingCache hstNodeLoadingCache;

    public ChannelLazyLoadingChangedBySet(final HstSite previewHstSite, final Channel channel,
                                          final HstNodeLoadingCache hstNodeLoadingCache) {
        this.previewHstSite = previewHstSite;
        this.channel = channel;
        this.hstNodeLoadingCache = hstNodeLoadingCache;
    }

    private synchronized void load() {
        if (delegatee != null) {
            return;
        }
        delegatee = new HashSet<>();
        delegatee.addAll(getAllUsersWithComponentLock(previewHstSite));
        delegatee.addAll(getAllUsersWithSiteMapItemLock(previewHstSite));
        delegatee.addAll(getAllUsersWithSiteMenuLock(previewHstSite));

        // check preview channel node itself
        if (channel.getChannelNodeLockedBy() != null) {
            delegatee.add(channel.getChannelNodeLockedBy());
        }

        if (hstNodeLoadingCache != null && delegatee.size() > 0) {
            // filter all system users out because they are not manageable through the changed by set of a channel
            try (HstNodeLoadingCache.LazyCloseableSession lazyCloseableSession = hstNodeLoadingCache.createLazyCloseableSession()) {
                final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);

                final Iterator<String> iterator = delegatee.iterator();
                while (iterator.hasNext()) {
                    String userId = iterator.next();
                    try {
                        final User user = securityService.getUser(userId);
                        if (user.isSystemUser()) {
                            log.debug("Removing system user with Id '{}' from Changed By Set", userId);
                            iterator.remove();
                        }
                    } catch (ItemNotFoundException e) {
                        log.info("User with userId '{}' does not exist (any more). Do not filter that user Id out because " +
                                "for sure it is not a system user.", userId);
                    }
                }
            } catch (RepositoryException e) {
                throw new ModelLoadingException("Repository exception while getting jcr session", e);
            }
        }

    }

    private static Set<String> getAllUsersWithSiteMapItemLock(final HstSite previewHstSite) {
        Set<String> usersWithLock = new HashSet<>();
        for (HstSiteMapItem siteMapItem : previewHstSite.getSiteMap().getSiteMapItems()) {
            addUsersWithSiteMapItemLock(siteMapItem, usersWithLock, previewHstSite.getConfigurationPath());
        }
        return usersWithLock;
    }

    private static void addUsersWithSiteMapItemLock(final HstSiteMapItem item,
                                                    final Set<String> usersWithLock,
                                                    final String previewConfigurationPath) {

        final boolean inherited = !((CanonicalInfo) item).getCanonicalPath().startsWith(previewConfigurationPath + "/");
        if (inherited) {
            // skip inherited sitemap item changes as that is not supported currently
            return;
        }
        if (!(item instanceof ConfigurationLockInfo)) {
            return;
        }
        String lockedBy = ((ConfigurationLockInfo) item).getLockedBy();
        if (StringUtils.isNotBlank(lockedBy)) {
            usersWithLock.add(lockedBy);
        }
        for (HstSiteMapItem child : item.getChildren()) {
            addUsersWithSiteMapItemLock(child, usersWithLock, previewConfigurationPath);
        }
    }

    private static Set<String> getAllUsersWithComponentLock(final HstSite previewHstSite) {
        Set<String> usersWithLock = new HashSet<>();
        final HstComponentsConfiguration componentsConfiguration = previewHstSite.getComponentsConfiguration();
        for (HstComponentConfiguration hstComponentConfiguration : componentsConfiguration.getComponentConfigurations().values()) {
            addUsersWithComponentLock(hstComponentConfiguration, usersWithLock);
        }
        return usersWithLock;
    }

    private static void addUsersWithComponentLock(final HstComponentConfiguration config, final Set<String> usersWithLock) {
        if (config.isInherited()) {
            // skip inherited configuration changes as that is not supported currently
            return;
        }
        if (!(config instanceof ConfigurationLockInfo)) {
            return;
        }
        String lockedBy = ((ConfigurationLockInfo) config).getLockedBy();
        if (StringUtils.isNotBlank(lockedBy)) {
            usersWithLock.add(lockedBy);
        }
        for (HstComponentConfiguration hstComponentConfiguration : config.getChildren().values()) {
            addUsersWithComponentLock(hstComponentConfiguration, usersWithLock);
        }
    }


    private static Set<String> getAllUsersWithSiteMenuLock(final HstSite previewHstSite) {
        Set<String> usersWithLock = new HashSet<>();
        for (HstSiteMenuConfiguration config : previewHstSite.getSiteMenusConfiguration().getSiteMenuConfigurations().values()) {
            if (!(config instanceof ConfigurationLockInfo)) {
                continue;
            }
            final boolean inherited = !((CanonicalInfo) config).getCanonicalPath().startsWith(previewHstSite.getConfigurationPath() + "/");
            if (inherited) {
                // skip inherited sitemenu item changes as that is not supported currently
                continue;
            }
            String lockedBy = ((ConfigurationLockInfo) config).getLockedBy();
            addUserWithSiteMenuLock(lockedBy, usersWithLock);
        }
        return usersWithLock;
    }

    private static void addUserWithSiteMenuLock(String lockedBy, Set<String> usersWithLock) {
        if (StringUtils.isNotBlank(lockedBy)) {
            usersWithLock.add(lockedBy);
        }
    }

    @Override
    public int size() {
        load();
        return delegatee.size();
    }

    @Override
    public boolean isEmpty() {
        load();
        return delegatee.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        load();
        return delegatee.contains(o);
    }

    @Override
    public Iterator<String> iterator() {
        load();
        return delegatee.iterator();
    }

    @Override
    public Object[] toArray() {
        load();
        return delegatee.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        load();
        return delegatee.toArray(a);
    }

    @Override
    public boolean removeIf(final Predicate<? super String> filter) {
        throw new UnsupportedOperationException("#removeIf not supported in ChannelLazyLoadingChangedBySet");
    }

    @Override
    public boolean add(final String string) {
        throw new UnsupportedOperationException("#add not supported in ChannelLazyLoadingChangedBySet");
    }

    @Override
    public boolean remove(final Object o) {
        throw new UnsupportedOperationException("#remove not supported in ChannelLazyLoadingChangedBySet");
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(final Collection<? extends String> c) {
        throw new UnsupportedOperationException("#addAll not supported in ChannelLazyLoadingChangedBySet");
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException("#retainAll not supported in ChannelLazyLoadingChangedBySet");
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException("#removeAll not supported in ChannelLazyLoadingChangedBySet");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("#clear not supported in ChannelLazyLoadingChangedBySet");
    }

    @Override
    public int hashCode() {
        load();
        return delegatee.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        load();
        return delegatee.equals(obj);
    }

    @Override
    public String toString() {
        load();
        return "[ChannelLazyLoadingChangedBySet: " + delegatee.toString() + "]";
    }

    @Override
    public Object clone() {
        throw new UnsupportedOperationException("#clone not supported in ChannelLazyLoadingChangedBySet");
    }

}
