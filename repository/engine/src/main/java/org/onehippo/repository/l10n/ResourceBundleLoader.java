package org.onehippo.repository.l10n;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.LocaleUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCEBUNDLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCEBUNDLES;

class ResourceBundleLoader {

    private static final Logger log = LoggerFactory.getLogger(ResourceBundleLoader.class);

    private final Map<ResourceBundleKey, ResourceBundle> bundles = new HashMap<>();
    private final Stack<String> path = new Stack<>();

    private ResourceBundleLoader() {
    }

    static Map<ResourceBundleKey, ResourceBundle> load(final Node translations) throws RepositoryException {
        final ResourceBundleLoader loader = new ResourceBundleLoader();
        for (Node child : new NodeIterable(translations.getNodes())) {
            loader.traverse(child);
        }
        return Collections.unmodifiableMap(loader.bundles);
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
            bundle.putString(property.getName(), property.getString());
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

    private class ResourceBundleImpl implements ResourceBundle {
        private final String name;
        private final Locale locale;
        private final Map<String, String> strings = new HashMap<>();

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
            return strings.get(key);
        }

        private void putString(final String key, final String value) {
            strings.put(key, value);
        }

        private ResourceBundleKey getKey() {
            return new ResourceBundleKey(name, locale);
        }
    }
}
