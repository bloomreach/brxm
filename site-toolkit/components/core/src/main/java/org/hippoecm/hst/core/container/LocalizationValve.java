/*
 *  Copyright 2008 Hippo.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.LoggerFactory;

/**
 * LocalizationValve
 * @version $Id$
 */
public class LocalizationValve extends AbstractValve {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LocalizationValve.class);
    
    private static final char HYPHEN = '-';
    private static final char UNDERSCORE = '_';

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        
        HttpServletRequest servletRequest = context.getServletRequest();
        HstMutableRequestContext requestContext = (HstMutableRequestContext) context.getRequestContext();
        
        Locale preferredLocale = findPreferredLocale(servletRequest, requestContext);
        
        // when site mount or site map item doesn't force a preferred locale,
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
        }

        // continue
        context.invokeNext();
    }

    protected Locale findPreferredLocale(HttpServletRequest request, HstRequestContext requestContext) {
        if(requestContext.getResolvedSiteMapItem() != null) {
            HstSiteMapItem siteMapItem =  requestContext.getResolvedSiteMapItem().getHstSiteMapItem();
            if(siteMapItem.getLocale() != null) {
                Locale locale = parseLocale(siteMapItem.getLocale());
                log.debug("Preferred locale for request is set to '{}' by sitemap item '{}'", siteMapItem.getLocale(), siteMapItem.getId());
                return locale;
            }
        }
        // if we did not yet find a locale, test the Mount
        if(requestContext.getResolvedMount() != null) {
            Mount mount = requestContext.getResolvedMount().getMount();
            if(mount.getLocale() != null) {
                Locale locale = parseLocale(mount.getLocale());
                log.debug("Preferred locale for request is set to '{}' by Mount '{}'", mount.getLocale(), mount.getName());
                return locale;
            }
        }
        
        // no locale found
        return null;
    }
    
    /**
     * See parseLocale(String, String) for details.
     */
    public static Locale parseLocale(String locale) {
    return parseLocale(locale, null);
    }

    /**
     * Parses the given locale string into its language and (optionally)
     * country components, and returns the corresponding
     * <tt>java.util.Locale</tt> object.
     *
     * If the given locale string is null or empty, the runtime's default
     * locale is returned.
     *
     * @param locale the locale string to parse
     * @param variant the variant
     *
     * @return <tt>java.util.Locale</tt> object corresponding to the given
     * locale string, or the runtime's default locale if the locale string is
     * null or empty
     *
     * @throws IllegalArgumentException if the given locale does not have a
     * language component or has an empty country component
     */
    public static Locale parseLocale(String locale, String variant) {

    Locale ret = null;
    String language = locale;
    String country = null;
    int index = -1;

    if (((index = locale.indexOf(HYPHEN)) > -1)
            || ((index = locale.indexOf(UNDERSCORE)) > -1)) {
        language = locale.substring(0, index);
        country = locale.substring(index+1);
    }

    if ((language == null) || (language.length() == 0)) {
        throw new IllegalArgumentException("LOCALE_NO_LANGUAGE");
    }

    if (country == null) {
        if (variant != null)
        ret = new Locale(language, "", variant);
        else
        ret = new Locale(language, "");
    } else if (country.length() > 0) {
        if (variant != null)
        ret = new Locale(language, country, variant);
        else
        ret = new Locale(language, country);
    } else {
       throw new IllegalArgumentException("LOCALE_EMPTY_COUNTRY");
    }

    return ret;
    }

}
