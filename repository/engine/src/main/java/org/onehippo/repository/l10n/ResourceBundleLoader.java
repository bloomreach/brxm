/*
 * Copyright 2015-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.text.StringSubstitutor;
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

    private ResourceBundleImpl resolveParent(ResourceBundleImpl bundle) {
        ResourceBundleImpl result = null;
        for (ResourceBundleImpl current : bundles.values()) {
            if (current == bundle) {
                continue;
            }
            if (current.getName().equals(bundle.getName())) {
                if (isFallback(current.getLocale(), bundle.getLocale())) {
                    if (result == null || isFallback(result.getLocale(), current.getLocale())) {
                        result = current;
                    }
                }
            }
        }
        return result;
    }

    private boolean isDefaultLocale(Locale locale) {
        return locale.equals(DEFAULT_LOCALE);
    }

    private boolean isFallback(Locale locale1, Locale locale2) {
        if (isDefaultLocale(locale1)) {
            return true;
        }
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

    private static class ResourceBundleDecorator extends java.util.ResourceBundle {
        private ResourceBundleImpl repositoryResourceBundle;

        public ResourceBundleDecorator(ResourceBundleImpl repositoryResourceBundle) {
            this.repositoryResourceBundle = repositoryResourceBundle;

            final ResourceBundleImpl parent = repositoryResourceBundle.getParent();
            if (parent != null) {
                setParent(new ResourceBundleDecorator(parent));
            }
        }

        @Override
        protected Object handleGetObject(final String key) {
            return repositoryResourceBundle.getString(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(repositoryResourceBundle.getKeys());
        }
    }

    private static class ResourceBundleImpl implements ResourceBundle {
        private final String name;
        private final Locale locale;
        private final Map<String, String> strings = new HashMap<>();
        private ResourceBundleImpl parent;

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

        /**
         * {@inheritDoc}
         * For the replacement {@link org.apache.commons.text.StringSubstitutor} is used.
         */
        @Override
        public String getString(final String key, final Map<String, String> parameters) {
            return new StringSubstitutor(parameters).replace(getString(key));
        }

        @Override
        public String getString(final String key, final String parameterName, final String parameterValue) {
            return new StringSubstitutor(Collections.singletonMap(parameterName, parameterValue))
                    .replace(getString(key));
        }

        @Override
        public java.util.ResourceBundle toJavaResourceBundle() {
            return new ResourceBundleDecorator(this);
        }

        private void putString(final String key, final String value) {
            strings.put(key, value);
        }

        private ResourceBundleKey getKey() {
            return new ResourceBundleKey(name, locale);
        }

        private Set<String> getKeys() {
            return strings.keySet();
        }

        private ResourceBundleImpl getParent() {
            return parent;
        }

        private void setParent(ResourceBundleImpl parent) {
            this.parent = parent;
        }

    }

}
