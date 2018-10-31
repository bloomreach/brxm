/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.model;

import javax.servlet.ServletContext;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.platform.HstModelProvider;
import org.springframework.web.context.ServletContextAware;

public class HstManagerImpl implements HstManager, ServletContextAware {

    private HstModelProvider hstModelProvider;

    /**
     * Request path suffix delimiter
     */
    private String pathSuffixDelimiter = "./";

    private String[] hstFilterPrefixExclusions;

    private String[] hstFilterSuffixExclusions;
    private ServletContext servletContext;

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void setHstModelProvider(HstModelProvider hstModelProvider) {
        this.hstModelProvider = hstModelProvider;
    }


    public HstSiteMapMatcher getSiteMapMatcher() {

        return hstModelProvider.getHstModel().getHstSiteMapMatcher();
    }

    public String getPathSuffixDelimiter() {
        return pathSuffixDelimiter;
    }
    
    public void setPathSuffixDelimiter(String pathSuffixDelimiter) {
        this.pathSuffixDelimiter = pathSuffixDelimiter;
    }

    public void setHstFilterPrefixExclusions(final String[] hstFilterPrefixExclusions) {
        this.hstFilterPrefixExclusions = hstFilterPrefixExclusions;
    }

    public void setHstFilterSuffixExclusions(final String[] hstFilterSuffixExclusions) {
        this.hstFilterSuffixExclusions = hstFilterSuffixExclusions;
    }

    public String[] getHstFilterPrefixExclusions() {
        return hstFilterPrefixExclusions;
    }

    public String[] getHstFilterSuffixExclusions() {
        return hstFilterSuffixExclusions;
    }

    public String getContextPath() {
        return servletContext.getContextPath();
    }

    @Deprecated
    @Override
    public boolean isHstFilterExcludedPath(final String pathInfo) {
        return hstModelProvider.getHstModel().getVirtualHosts().isHstFilterExcludedPath(pathInfo);
    }

    @Override
    public VirtualHosts getVirtualHosts() throws ContainerException {
        return hstModelProvider.getHstModel().getVirtualHosts();
    }

}
