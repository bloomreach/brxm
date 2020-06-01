/*
 *  Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.request;

import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.util.PropertyParser;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResolvedSiteMapItemImpl
 *
 * @version $Id$
 */
public class ResolvedSiteMapItemImpl implements ResolvedSiteMapItem {

    private final static Logger log = LoggerFactory.getLogger(ResolvedSiteMapItemImpl.class);
    private HstSiteMapItem hstSiteMapItem;
    private Properties resolvedParameters;
    private Properties localResolvedParameters;
    private ResolvedMount resolvedMount;
    private String relativeContentPath;
    private HstComponentConfiguration hstComponentConfiguration;
    private String pathInfo;
    private String pageTitle;
    private PropertyParser pp;
    private boolean isComponentResolved;

    public ResolvedSiteMapItemImpl(HstSiteMapItem hstSiteMapItem , Properties params, String pathInfo, ResolvedMount resolvedMount) {
        this.pathInfo = PathUtils.normalizePath(pathInfo);
        this.hstSiteMapItem = hstSiteMapItem;
        this.resolvedMount = resolvedMount;

       /*
        * We take the properties form the hstSiteMapItem getParameters and replace params (like ${1}) with the params[] array
        */

        resolvedParameters = new Properties();
        localResolvedParameters = new Properties();

        resolvedParameters.putAll(params);
        localResolvedParameters.putAll(params);

        pp = new PropertyParser(params);

        for(Entry<String, String> entry : hstSiteMapItem.getParameters().entrySet()) {
            Object o = pp.resolveProperty(entry.getKey(), entry.getValue());
            if (o != null) {
                resolvedParameters.put(entry.getKey(), o);
            }
        }
        for (Entry<String, String> mountParams : resolvedMount.getMount().getParameters().entrySet()) {
            if (resolvedParameters.contains(mountParams.getKey())) {
                continue;
            }
            Object o = pp.resolveProperty(mountParams.getKey(), mountParams.getValue());
            if (o != null) {
                resolvedParameters.put(mountParams.getKey(), o);
            }
        }

        for(Entry<String, String> entry : hstSiteMapItem.getLocalParameters().entrySet()) {
            Object o = pp.resolveProperty(entry.getKey(), entry.getValue());
            if (o != null) {
                localResolvedParameters.put(entry.getKey(), o);
            }
        }

        relativeContentPath = (String)pp.resolveProperty("relativeContentPath", hstSiteMapItem.getRelativeContentPath());
        pageTitle = (String)pp.resolveProperty("pageTitle", hstSiteMapItem.getPageTitle());
    }

    public int getStatusCode(){
        return hstSiteMapItem.getStatusCode();
    }

    public int getErrorCode(){
        return hstSiteMapItem.getErrorCode();
    }

    public HstSiteMapItem getHstSiteMapItem() {
        return hstSiteMapItem;
    }


    public HstComponentConfiguration getHstComponentConfiguration() {
        if (isComponentResolved) {
            return hstComponentConfiguration;
        }
        resolveComponentConfiguration();

        return hstComponentConfiguration;
    }

    public String getParameter(String name) {
        return (String)resolvedParameters.get(name);
    }

    public Properties getParameters(){
        return resolvedParameters;
    }

    public String getLocalParameter(String name) {
        return (String)localResolvedParameters.get(name);
    }

    public Properties getLocalParameters() {
        return localResolvedParameters;
    }

    public String getRelativeContentPath() {
        return relativeContentPath;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getPageTitle() {
        return pageTitle;
    }

    public ResolvedMount getResolvedMount() {
        return resolvedMount;
    }

    public String getNamedPipeline() {
        return hstSiteMapItem.getNamedPipeline();
    }

    public boolean isAuthenticated() {
        return hstSiteMapItem.isAuthenticated();
    }

    public Set<String> getRoles() {
        return hstSiteMapItem.getRoles();
    }

    public Set<String> getUsers() {
        return hstSiteMapItem.getUsers();
    }

    private void resolveComponentConfiguration() {

        // check whether there is a more specific mapping
        String componentConfigurationId = resolveMappedConponentConfigurationId();

        HstSite hstSite = hstSiteMapItem.getHstSiteMap().getSite();
        if (componentConfigurationId == null && hstSiteMapItem.getComponentConfigurationId() == null) {
            log.debug("The ResolvedSiteMapItemImpl does not have a component configuration id because the sitemap item '{}' does not have one", hstSiteMapItem.getId());
        } else {
            if (componentConfigurationId == null) {
                log.debug("No mapped component configuration id, getting the default componentconfiguration id");
                componentConfigurationId = hstSiteMapItem.getComponentConfigurationId();
            }

            if (componentConfigurationId != null) {
                final String resolvedComponentConfigurationId = (String)pp.resolveProperty("componentConfigurationId", componentConfigurationId);
                hstComponentConfiguration = hstSite.getComponentsConfiguration().getComponentConfiguration(resolvedComponentConfigurationId);

                if (hstComponentConfiguration == null) {
                    log.warn("ResolvedSiteMapItemImpl cannot be created correctly, because the component configuration id {} cannot be found.",
                            componentConfigurationId);
                }
            }
        }

        isComponentResolved = true;
    }

    /**
     * @return the more specific component configuration id and <code>null<</code> if there is no more specific one
     */
    private String resolveMappedConponentConfigurationId() {
        if (hstSiteMapItem.getComponentConfigurationIdMappings() == null
                || hstSiteMapItem.getComponentConfigurationIdMappings().isEmpty()) {
            return null;
        }

        try {
            final HippoBean contentBean = RequestContextProvider.get().getContentBean();
            if (contentBean != null && contentBean.getNode() != null) {
                String primaryType = contentBean.getNode().getPrimaryNodeType().getName();
                return hstSiteMapItem.getComponentConfigurationIdMappings().get(primaryType);
            } else {
                log.debug("No content bean found for request, return null");
                return null;
            }
        } catch (RepositoryException e) {
            log.error("Repository exception while looking up component mapping", e);
            return null;
        }
    }
}
