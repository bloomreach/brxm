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

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;

import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.LocaleUtils;
import org.hippoecm.hst.resourcebundle.SimpleListResourceBundle;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResourceBundleFamilyFactory is a Spring-created factory for reading a repository-based resource bundle document
 * ('resourcebundle:resourcebundle') into an in-memory, cacheable {@link ResourceBundleFamily}.
 */
public class ResourceBundleFamilyFactory {

    private static Logger log = LoggerFactory.getLogger(ResourceBundleFamilyFactory.class);

    private final Repository repository;
    private final Credentials liveCredentials;
    private final Credentials previewCredentials;

    public ResourceBundleFamilyFactory(Repository repository, Credentials liveCredentials, Credentials previewCredentials) {
        this.repository = repository;
        this.liveCredentials = liveCredentials;
        this.previewCredentials = previewCredentials;
    }

    public ResourceBundleFamily createBundleFamily(String basename) {
        return createBundleFamily(basename, false);
    }

    public ResourceBundleFamily createBundleFamily(final String basename, final boolean preview) {
        ResourceBundleFamily bundleFamily = new ResourceBundleFamily(basename);

        String availabilityConstraint = HippoNodeType.HIPPO_AVAILABILITY + (preview ? "='preview'" : "='live'");
        // "order by @resourcebundle:id" avoids that QueryResult#getSize() or QueryResult#getNodes#getSize can return -1
        String statement = "//element(*, resourcebundle:resourcebundle)[@resourcebundle:id='" + basename + "' and "
                + availabilityConstraint + "] order by @resourcebundle:id";
        Credentials creds = preview ? previewCredentials : liveCredentials;
        Session session = null;
        try {
            session = repository.login(creds);
            Query query = session.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
            NodeIterator nodes = query.execute().getNodes();

            if (nodes.getSize() == 0) {
                log.warn("Cannot load resource bundle with resourcebundle:id '{}' because no resource bundle " +
                        "with this id found", basename);
            } else {
                final Node node = nodes.nextNode();
                populateResourceBundleFamily(bundleFamily, node);

                if (nodes.hasNext()) {
                    List<String> paths = new ArrayList<>((int) nodes.getSize());
                    while (nodes.hasNext()) {
                        paths.add(nodes.nextNode().getPath());
                    }
                    log.warn("Multiple resource bundles found for resourcebundle:id '{}'. "
                            + "We only use resource bundle '{}'. Other (ignored) resource bundles with same id are: '{}'.",
                            basename, node.getPath(), paths.toString());
                }
            }
        } catch (RepositoryException e) {
            log.warn("Failed to populate resource bundle family", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }

        if (bundleFamily.getDefaultBundle() == null) {
            return null;
        }

        bundleFamily.setParentBundles();
        return bundleFamily;
    }

    private void populateResourceBundleFamily(final ResourceBundleFamily bundleFamily, Node bundleNode)
            throws RepositoryException {

        bundleFamily.setVariantUUID(bundleNode.getIdentifier());

        String[] keys = getPropertyAsStringArray(bundleNode.getProperty("resourcebundle:keys"));
        if (keys == null) {
            return;
        }

        // default bundle
        if (bundleNode.hasProperty("resourcebundle:messages")) {
            addBundle(bundleFamily, keys, bundleNode.getProperty("resourcebundle:messages"), null);
        }

        // additional bundles for locales
        final PropertyIterator propIt = bundleNode.getProperties("resourcebundle:messages_*");
        while (propIt.hasNext()) {
            final Property prop = propIt.nextProperty();
            final String localeString = prop.getName().substring("resourcebundle:messages_".length());

            addBundle(bundleFamily, keys, prop, localeString);
        }
    }

    private void addBundle(final ResourceBundleFamily bundleFamily, final String[] keys, final Property prop,
                           final String localeString) throws RepositoryException {
        try {
            Locale locale = LocaleUtils.toLocale(localeString);
            String[] localizedMessages = getPropertyAsStringArray(prop);

            if (localizedMessages != null) {
                if (keys.length != localizedMessages.length) {
                    throw new IllegalArgumentException("keys and messages must be of equal length.");
                }
                final Map<String, String> contents = createListResourceBundleContents(keys, localizedMessages);
                ResourceBundle resourceBundle = new SimpleListResourceBundle(contents);

                if (locale != null) {
                    bundleFamily.setLocalizedBundle(locale, resourceBundle);
                } else {
                    bundleFamily.setDefaultBundle(resourceBundle);
                }
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to load default resource bundle from '" + prop.getPath() + "' for locale: " + localeString, e);
            } else {
                log.warn("Failed to load default resource bundle from '{}' for locale '{}' : {}.",
                        prop.getPath(), localeString, e.toString());
            }
        }
    }

    private String[] getPropertyAsStringArray(final Property prop) throws RepositoryException {
        String[] stringValues = null;
        Value[] values = prop.getValues();

        if (values != null) {
            stringValues = new String[values.length];

            for (int i = 0; i < values.length; i++) {
                stringValues[i] = values[i].getString();
            }
        }

        return (stringValues != null ? stringValues : ArrayUtils.EMPTY_STRING_ARRAY);
    }

    protected Map<String, String> createListResourceBundleContents(String[] keys, String[] messages) {
        Map<String, String> contents = new HashMap<>(keys.length);
        Map<String, String> contentsMap = new HashMap<>();

        for (int i = 0; i < keys.length; i++) {
            String message = (i < messages.length ? messages[i] : "");
            contentsMap.put(keys[i], message);
        }

        // use commons-configuration in order to translate variables (e.g., ${key1}) for the values of the following keys
        MapConfiguration config = new MapConfiguration(contentsMap);
        config.setDelimiterParsingDisabled(true);
        config.setTrimmingDisabled(true);
        for (String key : keys) {
            String message = config.getString(key);
            contents.put(key, message);
        }
        return contents;
    }

}
