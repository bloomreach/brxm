/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.l10n;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.LocaleUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCEBUNDLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCEBUNDLES;
import static org.onehippo.repository.l10n.LocalizationService.DEFAULT_LOCALE;

class ResourceBundleLoader {

    private static final Logger log = LoggerFactory.getLogger(ResourceBundleLoader.class);

    private final Map<ResourceBundleKey, ResourceBundleImpl> bundles = new HashMap<>();
    private final Stack<String> path = new Stack<>();

    private ResourceBundleLoader() {
    }

    static Map<ResourceBundleKey, ResourceBundle> load(final Node translations) throws RepositoryException {
        final ResourceBundleLoader loader = new ResourceBundleLoader();
        for (Node child : new NodeIterable(translations.getNodes())) {
            loader.traverse(child);
        }
        loader.wireParentBundles();
        return Collections.unmodifiableMap(loader.bundles);
    }

    private void wireParentBundles() {
        for (ResourceBundleImpl bundle : bundles.values()) {
            bundle.setParent(resolveParent(bundle));
        }
    }

    private ResourceBundle resolveParent(ResourceBundleImpl bundle) {
        ResourceBundle result = null;
        for (ResourceBundleImpl parent : bundles.values()) {
            if (parent == bundle) {
                continue;
            }
            if (parent.getName().equals(bundle.getName())) {
                if (isParent(parent.getLocale(), bundle.getLocale())) {
                    if (result == null || isParent(result.getLocale(), parent.getLocale())) {
                        result = parent;
                    }
                }
                if (result == null && isDefaultLocale(parent.getLocale())) {
                    result = parent;
                }
            }
        }
        return result;
    }

    private boolean isDefaultLocale(Locale locale) {
        return locale.equals(DEFAULT_LOCALE);
    }

    private boolean isParent(Locale locale1, Locale locale2) {
        if (locale1.getLanguage().equals(locale2.getLanguage())) {
            if (locale1.getCountry().equals(locale2.getCountry())) {
                return locale1.getVariant().isEmpty() && !locale2.getVariant().isEmpty();
            } else {
                return locale1.getCountry().isEmpty() && !locale2.getCountry().isEmpty();
            }
        }
        return false;
    }

    private void traverse(final Node node) throws RepositoryException {
        if (node.isNodeType(NT_RESOURCEBUNDLES)) {
            path.push(node.getName());
            for (Node child : new NodeIterable(node.getNodes())) {
                traverse(child);
            }
            path.pop();
        }
        if (node.isNodeType(NT_RESOURCEBUNDLE)) {
            try {
                final ResourceBundleImpl bundle = createResourceBundle(node);
                bundles.put(bundle.getKey(), bundle);
            } catch (IllegalArgumentException e) {
                log.error("Failed to load bundle '{}' for locale '{}': {}", getName(), node.getName(), e.getMessage());
            }
        }
    }

    private ResourceBundleImpl createResourceBundle(final Node node) throws RepositoryException {
        final Locale locale = LocaleUtils.toLocale(node.getName());
        final ResourceBundleImpl bundle = new ResourceBundleImpl(getName(), locale);
        log.debug("Loading bundle '{}' for locale '{}'", getName(), locale);
        for (Property property : new PropertyIterable(node.getProperties())) {
            if (isTranslation(property)) {
                bundle.putString(property.getName(), property.getString());
            }
        }
        return bundle;
    }

    private String getName() {
        final StringBuilder sb = new StringBuilder();
        final Iterator<String> iterator = path.iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next());
            if (iterator.hasNext()) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    public boolean isTranslation(Property property) throws RepositoryException {
        return property.getType() == PropertyType.STRING;
    }

    private class ResourceBundleImpl implements ResourceBundle {
        private final String name;
        private final Locale locale;
        private final Map<String, String> strings = new HashMap<>();
        private ResourceBundle parent;

        private ResourceBundleImpl(final String name, final Locale locale) {
            this.name = name;
            this.locale = locale;
        }

        @Override
        public Locale getLocale() {
            return locale;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getString(final String key) {
            final String result = strings.get(key);
            if (result != null) {
                return result;
            }
            if (parent != null) {
                return parent.getString(key);
            }
            return null;
        }

        private void putString(final String key, final String value) {
            strings.put(key, value);
        }

        private ResourceBundleKey getKey() {
            return new ResourceBundleKey(name, locale);
        }

        private void setParent(ResourceBundle parent) {
            this.parent = parent;
        }

    }
}
