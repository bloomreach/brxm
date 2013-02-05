/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.resourcebundle.ResourceBundleRegistry;
import org.slf4j.LoggerFactory;

/**
 * LocalizationValve
 * @version $Id$
 */
public class LocalizationValve extends AbstractValve {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LocalizationValve.class);
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        
        HttpServletRequest servletRequest = context.getServletRequest();
        HstMutableRequestContext requestContext = (HstMutableRequestContext) context.getRequestContext();
        
        Locale preferredLocale = findPreferredLocale(servletRequest, requestContext);
        
        // when Mount or site map item doesn't force a preferred locale,
        // allow to override preferred locale from the request or session attribute.
        // In some environment, they can just implement a filter to set request attirbute or
        // they can simply store a session attribute for a transient session sepecific locale setting.
        if (preferredLocale == null) {
            preferredLocale = (Locale) servletRequest.getAttribute(ContainerConstants.PREFERRED_LOCALE_ATTR_NAME);
            
            if (preferredLocale == null) {
                HttpSession session = servletRequest.getSession(false);
                
                if (session != null) {
                    preferredLocale = (Locale) session.getAttribute(ContainerConstants.PREFERRED_LOCALE_ATTR_NAME);
                }
            }
        }
        
        if (preferredLocale != null) {
            List<Locale> locales = new ArrayList<Locale>();
            locales.add(preferredLocale);

            for (Enumeration<?> e = servletRequest.getLocales(); e.hasMoreElements();) {
                Locale locale = (Locale) e.nextElement();

                if (!locale.equals(preferredLocale)) {
                    locales.add(locale);
                }
            }

            requestContext.setPreferredLocale(preferredLocale);
            requestContext.setLocales(locales);
            
            requestContext.setAttribute("javax.servlet.jsp.jstl.fmt.locale.application", preferredLocale);
            servletRequest.setAttribute("javax.servlet.jsp.jstl.fmt.locale.request", preferredLocale);
        }

        // if default resource bundle is found, then set it to request attribute.
        ResourceBundle defaultResourceBundle = findDefaultResourceBundle(requestContext);
        if (defaultResourceBundle != null) {
            Config.set(servletRequest, Config.FMT_LOCALIZATION_CONTEXT, new LocalizationContext(defaultResourceBundle));
        }

        // continue
        context.invokeNext();
    }

    protected Locale findPreferredLocale(HttpServletRequest request, HstRequestContext requestContext) {
        if(requestContext.getResolvedSiteMapItem() != null) {
            HstSiteMapItem siteMapItem =  requestContext.getResolvedSiteMapItem().getHstSiteMapItem();
            if(siteMapItem.getLocale() != null) {
                Locale locale = LocaleUtils.toLocale(siteMapItem.getLocale());
                log.debug("Preferred locale for request is set to '{}' by sitemap item '{}'", siteMapItem.getLocale(), siteMapItem.getId());
                return locale;
            }
        }
        // if we did not yet find a locale, test the Mount
        if(requestContext.getResolvedMount() != null) {
            Mount mount = requestContext.getResolvedMount().getMount();
            if(mount.getLocale() != null) {
                Locale locale = LocaleUtils.toLocale(mount.getLocale());
                log.debug("Preferred locale for request is set to '{}' by Mount '{}'", mount.getLocale(), mount.getName());
                return locale;
            }
        }
        
        // no locale found
        return null;
    }

    protected ResourceBundle findDefaultResourceBundle(HstRequestContext requestContext) {
        ResourceBundle bundle = null;

        if (requestContext.getResolvedMount() != null) {
            String bundleId;
            if (requestContext.getResolvedSiteMapItem() != null) {
                bundleId = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getResourceBundleId();
            } else {
                bundleId = requestContext.getResolvedMount().getMount().getDefaultResourceBundleId();
            }
            if (StringUtils.isNotEmpty(bundleId)) {
                ResourceBundleRegistry bundleRegistry = getResourceBundleRegistry();
                Locale locale = requestContext.getPreferredLocale();

                try {
                    if (bundleRegistry != null) {
                        if (locale == null) {
                            bundle = (requestContext.isPreview() ? bundleRegistry.getBundleForPreview(bundleId) : bundleRegistry.getBundle(bundleId));
                        } else {
                            bundle = (requestContext.isPreview() ? bundleRegistry.getBundleForPreview(bundleId, locale) : bundleRegistry.getBundle(bundleId, locale));
                        }
                    } else {
                        if (locale == null) {
                            bundle = ResourceBundle.getBundle(bundleId, Locale.getDefault(), Thread.currentThread().getContextClassLoader());
                        } else {
                            bundle = ResourceBundle.getBundle(bundleId, locale, Thread.currentThread().getContextClassLoader());
                        }
                    }
                } catch (MissingResourceException e) {
                    log.warn("Resource bundle not found by the basename, '{}'. {}", bundleId, e);
                }
            }
        }

        return bundle;
    }
}
