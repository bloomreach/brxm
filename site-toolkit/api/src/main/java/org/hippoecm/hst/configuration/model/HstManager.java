/*
 * Copyright 2010-2022 Bloomreach
 */
package org.hippoecm.hst.configuration.model;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;

public interface HstManager {

    /**
     * @return the <code>VirtualHosts</code>, aka the HST model.
     */
    VirtualHosts getVirtualHosts() throws ContainerException;


    /**
     * a HstSitesManager must contain a reference to the {@link HstSiteMapMatcher} that is being used. You can inject your own
     *  {@link HstSiteMapMatcher} implementation if needed
     * @return the global HstSiteMapMatcher implementation used for all the hosts {@literal &} sites
     */
    HstSiteMapMatcher getSiteMapMatcher();

    /**
     *
     * Some paths should not be handled by the hst framework request processing, eg /ping/
     *
     * When a path must be excluded, this method return true.
     *
     * @param pathInfo
     * @return true when the path must be excluded for matching to a host.
     * @deprecated since 13.0.0 : use {@link VirtualHosts#isHstFilterExcludedPath(String)}
     */
    @Deprecated
    boolean isHstFilterExcludedPath(String pathInfo);

    /**
     * Returns the request path suffix delimiter.
c     */
    String getPathSuffixDelimiter();

    String[] getHstFilterPrefixExclusions();

    String[] getHstFilterSuffixExclusions();


    /**
     * @return the contextPath of the current webapp
     */
    String getContextPath();


}