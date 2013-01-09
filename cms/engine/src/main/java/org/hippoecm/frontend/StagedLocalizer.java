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
package org.hippoecm.frontend;

import org.apache.wicket.Component;
import org.apache.wicket.Localizer;
import org.apache.wicket.model.IModel;

/**
 * A Localizer implementation that interprets keys as criteria of descending
 * importance.  Different criteria are separated by a "," (comma).  Criteria
 * are dropped until a match is found.
 */
public class StagedLocalizer extends Localizer {

    public StagedLocalizer() {
    }

    /**
     * Returns a cache key for a resource. The cache key depends on the key, the component path, and a unique key
     * returned by the resource provider. The resource provider key is ignored when it is null.
     *
     * @param key the resource key
     * @param component the component
     * @return a cache key
     *
     * @see {@link org.hippoecm.frontend.IStringResourceProvider#getResourceProviderKey()}
     */
    @Override
    protected String getCacheKey(final String key, final Component component) {
        IStringResourceProvider provider = null;
        if (component instanceof IStringResourceProvider) {
            provider = (IStringResourceProvider) component;
        } else if (component != null) {
            provider = component.findParent(IStringResourceProvider.class);
        }
        String resourceProviderKey = null;
        if (provider != null) {
            resourceProviderKey = provider.getResourceProviderKey();
        }
        if (resourceProviderKey != null) {
            return resourceProviderKey + super.getCacheKey(key, component);
        } else {
            return super.getCacheKey(key, component);
        }
    }

    @Override
    public String getStringIgnoreSettings(String key, final Component component, final IModel<?> model,
            final String defaultValue) {
        while (key.contains(",")) {
            String value = super.getStringIgnoreSettings(key, component, model, null);
            if (value != null) {
                return value;
            }
            key = key.substring(0, key.lastIndexOf(','));
        }
        return super.getStringIgnoreSettings(key, component, model, defaultValue);
    }
}
