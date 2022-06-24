/*
 * Copyright 2008-2022 Bloomreach
 */
package org.hippoecm.hst.configuration.sitemapitemhandlers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface HstSiteMapItemHandlersConfiguration {

    static final HstSiteMapItemHandlersConfiguration NOOP = new HstSiteMapItemHandlersConfiguration() {
        @Override
        public Map<String, HstSiteMapItemHandlerConfiguration> getSiteMapItemHandlerConfigurations() {
            return Collections.EMPTY_MAP;
        }

        @Override
        public HstSiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration(final String id) {
            return null;
        }
    };

    /**
     * Return the map of all <code>HstSiteMapItemHandlerConfiguration</code>'s where the keys are the the <code>HstSiteMapItemHandlerConfiguration</code>'s 
     * ({@link HstSiteMapItemHandlerConfiguration#getId()}).
     * Implementations should return an unmodifiable map, for example the one returned by {@link java.util.Collections#unmodifiableList(List)}, to avoid
     * client code changing configuration
     * @return the map of all <code>HstSiteMapItemHandlerConfiguration</code>'s and an empty map if there are no <code>HstSiteMapItemHandlerConfiguration</code>'s
     */
    Map<String, HstSiteMapItemHandlerConfiguration> getSiteMapItemHandlerConfigurations();

    /**
     *  @return the HstSiteMapItemHandlerConfiguration for <code>id</code> and  <code>null</code> if there is no HstSiteMapItemHandlerConfiguration with {@link HstSiteMapItemHandlerConfiguration#getId()} = id 
     */
    HstSiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration(String id);
    
}
