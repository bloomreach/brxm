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

import java.util.LinkedHashMap;
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
import javax.jcr.query.QueryResult;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.LocaleUtils;
import org.hippoecm.hst.resourcebundle.PlaceHolderEmptyResourceBundleFamily;
import org.hippoecm.hst.resourcebundle.ResourceBundleFamily;
import org.hippoecm.hst.resourcebundle.SimpleListResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HippoRepositoryResourceBundleFamilyFactory
 * <P>
 * HippoRepositoryResourceBundleFamilyFactory queries JCR Nodes (the type of which should be 'resourcebundle:resourcebundle')
 * for both live and preview modes, and adds all the bundles for each mode and locale.
 * </P>
 */
public class HippoRepositoryResourceBundleFamilyFactory implements ResourceBundleFamilyFactory {

    private static Logger log = LoggerFactory.getLogger(HippoRepositoryResourceBundleFamilyFactory.class);

    private static final ResourceBundleFamily PLACE_HOLDER_EMPTY_RESOURCE_BUNDLE_FAMILY = new PlaceHolderEmptyResourceBundleFamily();

    private final Repository repository;
    private final Credentials liveCredentials;
    private final Credentials previewCredentials;

    public HippoRepositoryResourceBundleFamilyFactory(Repository repository, Credentials liveCredentials, Credentials previewCredentials) {
        this.repository = repository;
        this.liveCredentials = liveCredentials;
        this.previewCredentials = previewCredentials;
    }

    @Override
    public ResourceBundleFamily createBundleFamily(String basename) {
        MutableResourceBundleFamily bundleFamily = new DefaultMutableResourceBundleFamily(basename);;

        Session session = null;
        Credentials[] creds = {liveCredentials, previewCredentials};
        for (Credentials credentials : creds) {
            try {
                String statement = "//element(*, resourcebundle:resourcebundle)[@resourcebundle:id='" + basename +  "']";
                session = repository.login(credentials);
                Query query = session.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
                QueryResult result = query.execute();

                NodeIterator nodeIt = result.getNodes();
                if (nodeIt.getSize() > 1) {
                    // two bundles (both preview|live) with same resourcebundle:id are not allowed.
                    final StringBuilder paths = new StringBuilder();
                    while (nodeIt.hasNext()){
                        paths.append(nodeIt.nextNode().getPath()).append(",");
                    }
                    // remove last comma
                    final String logPaths = paths.substring(0, (paths.length() -1));
                    log.warn("Cannot load resource bundle with resourcebundle:id '{}' because multiple " +
                            "documents found with same id. Documents containint duplicate ids are: '{}'",
                            basename, logPaths);
                } else if (nodeIt.getSize() == 0){
                    log.warn("Cannot load resource bundle with resourcebundle:id '{}' because no resource bundle " +
                            "with this id found", basename);
                } else {
                    Node bundleNode = nodeIt.nextNode();
                    boolean isPreview = (credentials == previewCredentials);
                    populateResourceBundleFamily(bundleFamily, bundleNode, isPreview);
                }
            } catch (RepositoryException e) {
                log.warn("Fail to query resource bundle node", e);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }

        if (bundleFamily.getDefaultBundle() == null && bundleFamily.getDefaultBundleForPreview() == null) {
            return PLACE_HOLDER_EMPTY_RESOURCE_BUNDLE_FAMILY;
        }

        return bundleFamily;
    }

    private void populateResourceBundleFamily(final MutableResourceBundleFamily bundleFamily, Node bundleNode,
                                              final boolean preview) throws RepositoryException {

        String[] keys = getPropertyAsStringArray(bundleNode, "resourcebundle:keys");
        if (bundleNode.hasProperty("resourcebundle:messages")) {
            try {
                String[] messages = getPropertyAsStringArray(bundleNode, "resourcebundle:messages");

                if (messages != null) {
                    if (keys.length != messages.length) {
                        String state = preview ? "preview" : "live";
                        throw new IllegalArgumentException("keys and messages must be of equal length but was not the case for '"+state+"'");
                    }
                    Object[][] contents = createListResourceBundleContents(keys, messages);
                    ResourceBundle defaultBundle = new SimpleListResourceBundle(contents);

                    if (preview) {
                        bundleFamily.setDefaultBundleForPreview(defaultBundle);
                    } else {
                        bundleFamily.setDefaultBundle(defaultBundle);
                    }
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to load default resource bundle from '"+bundleNode.getPath()+"'.", e);
                } else {
                    log.warn("Failed to load default resource bundle from '{}' : {}.",bundleNode.getPath(), e.toString());
                }
            }
        }

        for (PropertyIterator propIt = bundleNode.getProperties("resourcebundle:messages_*"); propIt.hasNext(); ) {
            Property prop = propIt.nextProperty();

            if (prop != null) {
                Locale locale = null;

                try {
                    locale = LocaleUtils.toLocale(prop.getName().substring("resourcebundle:messages_".length()));
                    String[] localizedMessages = getPropertyAsStringArray(prop);

                    if (localizedMessages != null) {
                        if (keys.length != localizedMessages.length) {
                            String state = preview ? "preview" : "live";
                            throw new IllegalArgumentException("keys and messages must be of equal length but was not the case for '"+state+"'");
                        }
                        Object[][] contents = createListResourceBundleContents(keys, localizedMessages);
                        ResourceBundle localizedBundle = new SimpleListResourceBundle(contents);

                        if (preview) {
                            bundleFamily.setLocalizedBundleForPreview(locale, localizedBundle);
                        } else {
                            bundleFamily.setLocalizedBundle(locale, localizedBundle);
                        }
                    }
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to load default resource bundle from '"+bundleNode.getPath()+"' for locale: " + locale, e);
                    } else {
                        log.warn("Failed to load default resource bundle from '{}' for locale '{}' : {}.",
                                new String[]{bundleNode.getPath(), locale.toString(), e.toString()});
                    }
                }
            }
        }
    }

    private String [] getPropertyAsStringArray(final Node node, final String propName) throws RepositoryException {
        return getPropertyAsStringArray(node.getProperty(propName));
    }

    private String [] getPropertyAsStringArray(final Property prop) throws RepositoryException {
        String [] stringValues = null;
        Value [] values = prop.getValues();

        if (values != null) {
            stringValues = new String[values.length];

            for (int i = 0; i < values.length; i++) {
                stringValues[i] = values[i].getString();
            }
        }

        return (stringValues != null ? stringValues : ArrayUtils.EMPTY_STRING_ARRAY);
    }

    protected Object[][] createListResourceBundleContents(String [] keys, String [] messages) {
        Object [][] contents = new Object[keys.length][];

        Map<String, String> contentsMap = new LinkedHashMap<String, String>();

        for (int i = 0; i < keys.length; i++) {
            String message = (i < messages.length ? messages[i] : "");
            contentsMap.put(keys[i], message);
            contents[i] = new Object[] { keys[i], null };
        }

        // use commons-configuration in order to translate variables (e.g., ${key1}) for the values of the following keys
        MapConfiguration config = new MapConfiguration(contentsMap);
        config.setDelimiterParsingDisabled(true);
        config.setTrimmingDisabled(true);
        for (int i = 0; i < keys.length; i++) {
            String key = (String) contents[i][0];
            contents[i][1] = config.getString(key);
        }

        return contents;
    }
}
