/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.configuration.cache;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO CHECK COMMONS CATALOG BELOW /hst:hst/hst:configurations/hst:catalog

/**
 * <p>
 *  This is a {@link org.hippoecm.hst.configuration.components.HstComponentsConfigurationService} instance cache : When all the backing HstNode's for
 *  hst:pages, hst:components, hst:catalog and hst:templates, hst:workspace, then, the HstComponentsConfiguration object can be shared between different Mounts.
 *  The key is the Set of all HstNode path's directly below the components, pages, catalog and templates : The path uniquely defines the HstNode
 *  and there is only inheritance on the nodes directly below components, pages, catalog and templates: Since no fine-grained inheritance, these
 *  HstNode's identify uniqueness
 *  Also this cache is reused when a configuration change did not impact HstComponentsConfiguration's at all
 * </p>
 * <p>
 *   Note that this class is <strong>not</strong> thread-safe : It should not be accessed by concurrent threads
 * </p>
 */
public class HstConfigurationLoadingCache implements HstEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(HstConfigurationLoadingCache.class);

    /**
     * The enhanced version of configurationRootNodes : During enhancing, the inheritance (hst:inheritsfrom) is resolved. Note
     * that the original HstNode's in configurationRootNodes are not changed. Thus, all HstNode's in configurationRootNodes are
     * first copied to new instances. The backing provider is allowed to be the same instance still.
     */
    Map<String, HstSiteConfigurationRootNodeImpl> inheritanceResolvedMap = new HashMap<>();
    Map<String, List<String>> inheritanceResolvedEventRegistry = new HashMap<>();


    private Map<Set<UUID>, WeakReference<HstComponentsConfigurationService>> componentsConfigurationCache = new HashMap<>();
    private Map<String, List<WeakReference<HstComponentsConfiguration>>> componentsConfigurationEventRegistry = new HashMap<>();


    private HstNodeLoadingCache hstNodeLoadingCache;

    private String rootConfigurationsPrefix;

    public void setHstNodeLoadingCache(final HstNodeLoadingCache hstNodeLoadingCache) {
        this.hstNodeLoadingCache = hstNodeLoadingCache;
    }

    public void setRootConfigurationsPrefix(final String rootConfigurationsPrefix) {
        this.rootConfigurationsPrefix = rootConfigurationsPrefix;
    }

    @Override
    public void handleEvents(final Set<HstEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        Set<String> rootConfigurationEventPaths = new HashSet<>();
        for (HstEvent event : events) {
            log.debug("Processing event {}", event);
            if (isHstConfigurationsEvent(event)) {
                // get event for root config
                try {
                    rootConfigurationEventPaths.add(getRootConfigPath(event));
                } catch (IllegalArgumentException e) {
                    log.warn("Skipping unexpected event : {}", e.toString());
                }
            }
        }
        for (String rootPath : rootConfigurationEventPaths) {
             processEvent(rootPath);
        }

        // check caches for null weak ref values to avoid memory leaks: Remove those now
        List<Set<UUID>> toRemove = new ArrayList<>();
        for (Map.Entry<Set<UUID>, WeakReference<HstComponentsConfigurationService>> entry : componentsConfigurationCache.entrySet()) {
            if (entry.getValue().get() == null) {
                toRemove.add(entry.getKey());
            }
        }
        for (Set<UUID> key : toRemove) {
            componentsConfigurationCache.remove(key);
        }

        List<String> toRemoveKeys = new ArrayList<>();
        for (Map.Entry<String, List<WeakReference<HstComponentsConfiguration>>> entry : componentsConfigurationEventRegistry.entrySet()) {
            List<WeakReference<HstComponentsConfiguration>> nullWeakRefs = new ArrayList<>();
            for (WeakReference<HstComponentsConfiguration> weakReference : entry.getValue()) {
                if (weakReference.get() == null) {
                    nullWeakRefs.add(weakReference);
                }
            }
            for (WeakReference<HstComponentsConfiguration> nullWeakRef : nullWeakRefs) {
                entry.getValue().remove(nullWeakRef);
            }
            if (entry.getValue().isEmpty()) {
                toRemoveKeys.add(entry.getKey());
            }
        }

        for (String toRemoveKey : toRemoveKeys) {
            componentsConfigurationEventRegistry.remove(toRemoveKey);
        }
    }

    /**
     *
     * @param event
     * @return the root config path for the <code>event</code> and {@Link IllegalArgumentException} is not a
     * valid root configuration event
     */
    String getRootConfigPath(final HstEvent event) throws IllegalArgumentException {
        try {
            String eventPath = event.getNodePath();
            String configName = StringUtils.substringBefore(eventPath.substring(rootConfigurationsPrefix.length()), "/");
            if (StringUtils.isEmpty(configName)) {
                throw new IllegalArgumentException("Event '"+event+"' is not a valid hst configuration event.");
            }
            return rootConfigurationsPrefix + configName;
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Event '"+event+"' is not a valid hst configuration event.");
        }
    }

    private boolean isHstConfigurationsEvent(final HstEvent event) {
        return event.getNodePath().startsWith(rootConfigurationsPrefix);
    }

    /**
     * @param configurationPath
     * @return an inheritance resolved hst configuration root node
     */
    public HstSiteConfigurationRootNodeImpl getInheritanceResolvedNode(String configurationPath) {
        HstSiteConfigurationRootNodeImpl node = inheritanceResolvedMap.get(configurationPath);
        if (node != null) {
            return node;
        }
        node =  new HstSiteConfigurationRootNodeImpl((HstNodeImpl) hstNodeLoadingCache.getNode(configurationPath));
        registerInheritanceEventPaths(node, configurationPath);
        inheritanceResolvedMap.put(configurationPath, node);
        return node;
    }

    /**
     * check wether we already a an instance that would result in the very same HstComponentsConfiguration instance. If so, set that value
     * the cachekey is the set of all HstNode identifiers that make a HstComponentsConfigurationService unique: thus, pages, components, catalog and templates.
     * @param inheritanceResolvedNode the inheritance resolved root configuration node
     * @return a {@link HstComponentsConfigurationService} instance or <code>null</code> when no found for <code>configurationPath</code>
     */
    public HstComponentsConfigurationService get(final HstSiteConfigurationRootNodeImpl inheritanceResolvedNode) throws ServiceException {

        if (!valid(inheritanceResolvedNode)) {
            return null;
        }

        Set<UUID> cachekey = computeCacheKey(inheritanceResolvedNode);

        final WeakReference<HstComponentsConfigurationService> weakRef = componentsConfigurationCache.get(cachekey);
        HstComponentsConfigurationService hstComponentsConfiguration;
        if (weakRef != null && (hstComponentsConfiguration = weakRef.get()) != null ) {
            log.debug("Return cached HstComponentsConfiguration because exact same configuration. We do not build HstComponentsConfiguration for '{}' but use existing version.", inheritanceResolvedNode.getValueProvider().getPath());
            return hstComponentsConfiguration;
        }
        HstComponentsConfigurationService componentsConfigurationService = new HstComponentsConfigurationService(inheritanceResolvedNode, hstNodeLoadingCache);
        componentsConfigurationCache.put(cachekey, new WeakReference<>(componentsConfigurationService));

        for (String configurationDependencyPath : inheritanceResolvedNode.getConfigurationDependenyPaths()) {
            registerComponentsConfigurationEventPaths(configurationDependencyPath, componentsConfigurationService);
        }
        return componentsConfigurationService;
    }

    /**
     * @param componentsConfiguration if present, removes this item from the cache. It could be present multiple times.
     */
    public void remove(final HstComponentsConfiguration componentsConfiguration) {
        Set<Set<UUID>> keys2Remove = new HashSet<>();
        for (Map.Entry<Set<UUID>, WeakReference<HstComponentsConfigurationService>> entry : componentsConfigurationCache.entrySet()) {
            final HstComponentsConfigurationService entryValue = entry.getValue().get();
            if (componentsConfiguration == entryValue) {
                keys2Remove.add(entry.getKey());
            }
        }

        if (!keys2Remove.isEmpty()) {
            for (Set<UUID> key : keys2Remove) {
                final WeakReference<HstComponentsConfigurationService> removed = componentsConfigurationCache.remove(key);
                if (removed != null) {
                    HstComponentsConfiguration removedConfig = removed.get();
                    if (removedConfig != null) {
                        log.info("HstComponentsConfiguration '{}' removed from cache.", removedConfig.toString());
                    }
                }
            }
        }
    }

    public void processEvent(String hstRootConfigurationPathEvent) {
        final List<String> dependingConfigs = inheritanceResolvedEventRegistry.get(hstRootConfigurationPathEvent);
        if (dependingConfigs != null) {
            for (String dependingConfig : dependingConfigs) {
                inheritanceResolvedMap.remove(dependingConfig);
            }
            inheritanceResolvedEventRegistry.remove(hstRootConfigurationPathEvent);
        }

        Set<HstComponentsConfiguration> allConfigsToInvalidate = new HashSet<HstComponentsConfiguration>();
        List<WeakReference<HstComponentsConfiguration>> configsToInvalidate = componentsConfigurationEventRegistry.get(hstRootConfigurationPathEvent);
        if (configsToInvalidate != null) {
            for (WeakReference<HstComponentsConfiguration> weakRef : configsToInvalidate) {
                HstComponentsConfiguration config = weakRef.get();
                if (config != null) {
                    allConfigsToInvalidate.add(config);
                }
            }
            componentsConfigurationEventRegistry.remove(hstRootConfigurationPathEvent);
        }
        for (HstComponentsConfiguration invalidated : allConfigsToInvalidate) {
            remove(invalidated);
        }
    }


    private Set<UUID> computeCacheKey(HstNode configurationNode) {
        Set<UUID> key = new HashSet<UUID>();
        augmentKey(key,configurationNode.getNode(HstNodeTypes.NODENAME_HST_COMPONENTS));
        augmentKey(key,configurationNode.getNode(HstNodeTypes.NODENAME_HST_PAGES));
        augmentKey(key,configurationNode.getNode(HstNodeTypes.NODENAME_HST_CATALOG));
        augmentKey(key,configurationNode.getNode(HstNodeTypes.NODENAME_HST_TEMPLATES));
        augmentKey(key,configurationNode.getNode(HstNodeTypes.NODENAME_HST_WORKSPACE));
        return key;
    }

    // use UUID instead of String uuid as UUID are much smaller and faster to compare
    private void augmentKey(Set<UUID> key, HstNode node) {
        if(node != null) {
            for(HstNode n :node.getNodes()) {
                try {
                    key.add(UUID.fromString(n.getValueProvider().getIdentifier()));
                } catch (IllegalArgumentException e) {
                    log.warn("Cannot add identifier '{}' to cache key as not valid UUID format.", n.getValueProvider().getIdentifier());
                }
            }
        }
    }

    private boolean valid(final HstNode configurationRootNode) {
        if (!HstNodeTypes.NODETYPE_HST_CONFIGURATION.equals(configurationRootNode.getNodeTypeName())) {
            log.warn("Unexpected configurationRootNode '{}' as not of type '{}' but of type '{}'",
                    new String[]{configurationRootNode.getValueProvider().getPath(),HstNodeTypes.NODETYPE_HST_CONFIGURATION,
                            configurationRootNode.getNodeTypeName()});
            return false;
        }
        return true;
    }

    private void registerComponentsConfigurationEventPaths(final String eventPath, final HstComponentsConfiguration hstComponentsConfiguration) {
        List<WeakReference<HstComponentsConfiguration>> configsForEventPath = componentsConfigurationEventRegistry.get(eventPath);
        if (configsForEventPath == null) {
            configsForEventPath = new ArrayList<>();
            configsForEventPath.add(new WeakReference<>(hstComponentsConfiguration));
            componentsConfigurationEventRegistry.put(eventPath, configsForEventPath);
        } else {
            configsForEventPath.add(new WeakReference<>(hstComponentsConfiguration));
        }
    }

    private void registerInheritanceEventPaths(final HstSiteConfigurationRootNodeImpl node, final String configurationPath) {
        for (String eventPath : node.getConfigurationDependenyPaths()){
            List<String> configurationPaths = inheritanceResolvedEventRegistry.get(eventPath);
            if (configurationPaths == null) {
                configurationPaths = new ArrayList<>();
                configurationPaths.add(configurationPath);
                inheritanceResolvedEventRegistry.put(eventPath, configurationPaths);
            } else {
                if (!configurationPaths.contains(configurationPath)) {
                    configurationPaths.add(configurationPath);
                }
            }
        }
    }

}
