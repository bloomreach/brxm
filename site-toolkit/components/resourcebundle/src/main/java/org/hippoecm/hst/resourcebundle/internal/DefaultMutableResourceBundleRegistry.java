/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.resourcebundle.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.LocaleUtils;
import org.hippoecm.hst.resourcebundle.PlaceHolderEmptyResourceBundleFamily;
import org.hippoecm.hst.resourcebundle.ResourceBundleFamily;

/**
 * DefaultMutableResourceBundleRegistry
 */
public class DefaultMutableResourceBundleRegistry implements MutableResourceBundleRegistry {

    private Map<String, ResourceBundleFamily> bundleFamiliesMap = new ConcurrentHashMap(new HashMap<String, ResourceBundleFamily>());

    private ResourceBundleFamilyFactory resourceBundleFamilyFactory;

    /**
     * Flag whether or not to fallback to the default Java standard resource bundles
     * when the registry cannot find a matched resource bundle family.
     */
    private boolean fallbackToJavaResourceBundle = true;

    public DefaultMutableResourceBundleRegistry() {
    }

    public ResourceBundleFamilyFactory getResourceBundleFamilyFactory() {
        return resourceBundleFamilyFactory;
    }

    public void setResourceBundleFamilyFactory(ResourceBundleFamilyFactory resourceBundleFamilyFactory) {
        this.resourceBundleFamilyFactory = resourceBundleFamilyFactory;
    }

    public boolean isFallbackToJavaResourceBundle() {
        return fallbackToJavaResourceBundle;
    }

    public void setFallbackToJavaResourceBundle(boolean fallbackToJavaResourceBundle) {
        this.fallbackToJavaResourceBundle = fallbackToJavaResourceBundle;
    }

    @Override
    public ResourceBundle getBundle(String basename) {
        return getBundle(basename, null);
    }

    @Override
    public ResourceBundle getBundleForPreview(String basename) {
        return getBundleForPreview(basename, null);
    }

    @Override
    public ResourceBundle getBundle(String basename, Locale locale) {
        return getBundle(basename, locale, false);
    }

    @Override
    public ResourceBundle getBundleForPreview(String basename, Locale locale) {
        return getBundle(basename, locale, true);
    }

    @Override
    public void registerBundleFamily(String basename, ResourceBundleFamily bundleFamily) {
        bundleFamiliesMap.put(basename, bundleFamily);
    }

    @Override
    public void unregisterBundleFamily(String basename) {
        bundleFamiliesMap.remove(basename);
    }

    @Override
    public void unregisterAllBundleFamilies() {
        bundleFamiliesMap.clear();
    }

    protected ResourceBundle getBundle(String basename, Locale locale, boolean preview) {
        ResourceBundle bundle = null;
        ResourceBundleFamily bundleFamily = bundleFamiliesMap.get(basename);

        if (bundleFamily == null && resourceBundleFamilyFactory != null) {
            bundleFamily = bundleFamiliesMap.get(basename);

            if (bundleFamily == null) {
                bundleFamily = resourceBundleFamilyFactory.createBundleFamily(basename);

                if (bundleFamily != null) {
                    bundleFamiliesMap.put(basename, bundleFamily);
                }
            }
        }

        if (bundleFamily != null && !(bundleFamily instanceof PlaceHolderEmptyResourceBundleFamily)) {
            bundle = (preview ? bundleFamily.getDefaultBundleForPreview() : bundleFamily.getDefaultBundle());

            if (locale != null) {
                //
                // Let's try to find the best mapped resource bundle.
                // For example, if the locale is 'en_US', then it tries to find bundle by 'en_US'.
                // Next, it tries to find bundle by 'en' if not found.
                //
                @SuppressWarnings("unchecked")
                List<Locale> lookupLocales = (List<Locale>) LocaleUtils.localeLookupList(locale);

                for (Locale loc : lookupLocales) {
                    ResourceBundle localizedBundle = (preview ? bundleFamily.getLocalizedBundleForPreview(loc) : bundleFamily.getLocalizedBundle(loc));

                    if (localizedBundle != null) {
                        bundle = localizedBundle;
                        break;
                    }
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
