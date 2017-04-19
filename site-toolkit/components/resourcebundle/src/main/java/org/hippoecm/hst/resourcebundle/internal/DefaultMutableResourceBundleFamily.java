/**
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.LocaleUtils;
import org.hippoecm.hst.resourcebundle.SimpleListResourceBundle;

/**
 * DefaultMutableResourceBundleFamily is an in-memory representation of a variant (live or preview)
 * of a resource bundle document. It is cached by the resource bundle registry.
 */
public class DefaultMutableResourceBundleFamily implements MutableResourceBundleFamily {

    private final String basename;
    private String variantUUID;
    private ResourceBundle defaultBundle;
    private final Map<Locale, ResourceBundle> localizedBundlesMap = new ConcurrentHashMap(new HashMap<Locale, ResourceBundle>());

    public DefaultMutableResourceBundleFamily(final String basename) {
        this.basename = basename;
    }

    @Override
    public String getBasename() {
        return basename;
    }

    @Override
    public ResourceBundle getDefaultBundle() {
        return defaultBundle;
    }

    @Override
    public ResourceBundle getDefaultBundleForPreview() {
        throw new UnsupportedOperationException("ResourceBundleFamily no longer contains separate livae and preview data sets");
    }

    @Override
    public void setDefaultBundle(ResourceBundle defaultBundle) {
        this.defaultBundle = defaultBundle;
    }

    @Override
    public void setDefaultBundleForPreview(ResourceBundle defaultBundleForPreview) {
        throw new UnsupportedOperationException("ResourceBundleFamily no longer contains separate livae and preview data sets");
    }

    @Override
    public ResourceBundle getLocalizedBundle(Locale locale) {
        return localizedBundlesMap.get(locale);
    }

    @Override
    public ResourceBundle getLocalizedBundleForPreview(Locale locale) {
        throw new UnsupportedOperationException("ResourceBundleFamily no longer contains separate livae and preview data sets");
    }

    @Override
    public void setLocalizedBundle(Locale locale, ResourceBundle bundle) {
        localizedBundlesMap.put(locale, bundle);
    }

    @Override
    public void setLocalizedBundleForPreview(Locale locale, ResourceBundle bundle) {
        throw new UnsupportedOperationException("ResourceBundleFamily no longer contains separate livae and preview data sets");
    }

    @Override
    public void removeLocalizedBundle(Locale locale) {
        localizedBundlesMap.remove(locale);
    }

    @Override
    public void removeLocalizedBundleForPreview(Locale locale) {
        throw new UnsupportedOperationException("ResourceBundleFamily no longer contains separate livae and preview data sets");
    }

    public String getVariantUUID() {
        return variantUUID;
    }

    @Override
    public void setVariantUUID(final String variantUUID) {
        this.variantUUID = variantUUID;
    }

    public void setParentBundles() {
        for (Map.Entry<Locale, ResourceBundle> entry : localizedBundlesMap.entrySet()) {
            final ResourceBundle bundle = entry.getValue();
            if (!(bundle instanceof SimpleListResourceBundle)) {
                continue;
            }
            Locale locale = entry.getKey();
            boolean parentIsSet = false;
            List<Locale> parentLocales = new ArrayList<>((List<Locale>) LocaleUtils.localeLookupList(locale));
            // locale at index 0 is current locale
            parentLocales.remove(0);
            for (Locale parentLocale : parentLocales) {
                ResourceBundle parentBundle = localizedBundlesMap.get(parentLocale);
                if (parentBundle != null) {
                    ((SimpleListResourceBundle) bundle).setParent(parentBundle);
                    parentIsSet = true;
                    break;
                }
            }
            if (!parentIsSet) {
                ((SimpleListResourceBundle) bundle).setParent(defaultBundle);
            }
        }
    }
}
