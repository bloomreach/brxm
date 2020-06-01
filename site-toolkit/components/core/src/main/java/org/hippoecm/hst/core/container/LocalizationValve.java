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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.resourcebundle.CompositeResourceBundle;
import org.hippoecm.hst.resourcebundle.ResourceBundleRegistry;
import org.slf4j.LoggerFactory;

/**
 * LocalizationValve
 */
public class LocalizationValve extends AbstractBaseOrderableValve {

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
        if (requestContext.getResolvedSiteMapItem() != null) {
            HstSiteMapItem siteMapItem =  requestContext.getResolvedSiteMapItem().getHstSiteMapItem();
            String localeString = siteMapItem.getLocale();

            if (localeString != null) {
                try {
                    Locale locale = LocaleUtils.toLocale(localeString);
                    log.debug("Preferred locale for request is set to '{}' by sitemap item '{}'", localeString, siteMapItem.getId());
                    return locale;
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid locale, '{}', on the sitemap item, '{}'.", localeString, siteMapItem.getId());
                }
            }
        }

        // if we did not yet find a locale, test the Mount
        if (requestContext.getResolvedMount() != null) {
            Mount mount = requestContext.getResolvedMount().getMount();
            String localeString = mount.getLocale();

            if (localeString != null) {
                try {
                    Locale locale = LocaleUtils.toLocale(localeString);
                    log.debug("Preferred locale for request is set to '{}' by Mount '{}'", localeString, mount.getName());
                    return locale;
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid locale, '{}', on the mount, '{}'.", localeString, mount.getName());
                }
            }
        }

        // no locale found
        return null;
    }

    /**
     * Find the default resource bundle object if available. Otherwise, return null.
     * @param requestContext request context
     * @return the default resource bundle object if available. Otherwise, return null
     */
    protected ResourceBundle findDefaultResourceBundle(HstRequestContext requestContext) {
        String [] bundleIds = findResourceBundleIds(requestContext);

        if (ArrayUtils.isEmpty(bundleIds)) {
            return null;
        }

        ResourceBundleRegistry bundleRegistry = getResourceBundleRegistry();
        Locale locale = requestContext.getPreferredLocale();
        List<ResourceBundle> bundles = new ArrayList<ResourceBundle>();
        ResourceBundle bundle = null;

        for (String bundleId : bundleIds) {
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

                if (bundle != null) {
                    bundles.add(bundle);
                }
            } catch (MissingResourceException e) {
                log.warn("Resource bundle not found by the basename, '{}'. {}", bundleId, e);
            }
        }

        if (bundles.isEmpty()) {
            return null;
        } else if (bundles.size() == 1) {
            return bundles.get(0);
        } else {
            return new CompositeResourceBundle(bundles.toArray(new ResourceBundle[bundles.size()]));
        }
    }

    /**
     * Find the default resource bundle ID array if available. Otherwise, return null.
     * @param requestContext
     * @return the default resource bundle ID array if available. Otherwise, return null
     */
    protected String [] findResourceBundleIds(HstRequestContext requestContext) {
        String [] bundleIds = null;

        final ResolvedMount resolvedMount = requestContext.getResolvedMount();
        final ChannelInfo channelInfo = (resolvedMount != null) ? resolvedMount.getMount().getChannelInfo() : null;

        if (channelInfo != null && channelInfo.getProperties().containsKey(HstNodeTypes.GENERAL_PROPERTY_DEFAULT_RESOURCE_BUNDLE_ID)) {
            bundleIds = StringUtils.split((String) channelInfo.getProperties().get(HstNodeTypes.GENERAL_PROPERTY_DEFAULT_RESOURCE_BUNDLE_ID), "\t\r\n, ");
        } else {
            if (requestContext.getResolvedSiteMapItem() != null) {
                bundleIds = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getResourceBundleIds();
            } else if (resolvedMount != null) {
                bundleIds = resolvedMount.getMount().getDefaultResourceBundleIds();
            }
        }

        return bundleIds;
    }
}
