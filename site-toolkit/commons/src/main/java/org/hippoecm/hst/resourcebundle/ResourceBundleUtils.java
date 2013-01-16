/**
 * Copyright 2013-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.resourcebundle;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstRequestUtils;

/**
 * ResourceBundleUtils
 * <P>
 * Utility to get resource bundle from either HST ResourceBundleRegistry or Java Standard Resource Bundles.
 * </P>
 */
public class ResourceBundleUtils {

    private ResourceBundleUtils() {
    }

    /**
     * Returns resource bundle based on the specified basename and locale.
     * If the locale is null, then the default locale (Locale.getDefault()) is used.
     * If the specific resource bundle is not found from the HST ResourceBundleRegistry, then it looks up Java standard resource bundles.
     * It may throw java.util.MissingResourceException if a resource is not found when Java standard resource bundle resources are looked up.
     * @param servletRequest
     * @param basename
     * @param locale
     * @return
     * @throws java.lang.NullPointerException - if baseName is null 
     * @throws java.util.MissingResourceException - if no Java standard resource bundle for the specified base name can be found
     */
    public static ResourceBundle getBundle(HttpServletRequest servletRequest, String basename, Locale locale) {
        return getBundle(servletRequest, basename, locale, true);
    }

    /**
     * Returns resource bundle based on the specified basename and locale.
     * If the locale is null, then the default locale (Locale.getDefault()) is used.
     * If the specific resource bundle is not found from the HST ResourceBundleRegistry, then it looks up Java standard resource bundles
     * when the fallbackToJavaResourceBundle is true.
     * It may throw java.util.MissingResourceException if a resource is not found when Java standard resource bundle resources are looked up.
     * @param servletRequest
     * @param basename
     * @param locale
     * @param fallbackToJavaResourceBundle
     * @throws java.lang.NullPointerException - if baseName is null 
     * @throws java.util.MissingResourceException - if no Java standard resource bundle for the specified base name can be found
     */
    public static ResourceBundle getBundle(HttpServletRequest servletRequest, String basename, Locale locale, boolean fallbackToJavaResourceBundle) {
        ResourceBundle bundle = null;
        HstRequest hstRequest = HstRequestUtils.getHstRequest(servletRequest);

        if (hstRequest != null) {
            ResourceBundleRegistry resourceBundleRegistry = 
                    HstServices.getComponentManager().getComponent(ResourceBundleRegistry.class.getName());

            if (resourceBundleRegistry != null) {
                boolean preview = hstRequest.getRequestContext().isPreview();

                if (locale == null) {
                    bundle = (preview ? resourceBundleRegistry.getBundleForPreview(basename) : resourceBundleRegistry.getBundle(basename));
                } else {
                    bundle = (preview ? resourceBundleRegistry.getBundleForPreview(basename, locale) : resourceBundleRegistry.getBundle(basename, locale));
                }
            }
        }

        if (bundle == null && fallbackToJavaResourceBundle) {
            if (locale == null) {
                bundle = ResourceBundle.getBundle(basename, Locale.getDefault(), Thread.currentThread().getContextClassLoader());
            } else {
                bundle = ResourceBundle.getBundle(basename, locale, Thread.currentThread().getContextClassLoader());
            }
        }

        return bundle;
    }

}
