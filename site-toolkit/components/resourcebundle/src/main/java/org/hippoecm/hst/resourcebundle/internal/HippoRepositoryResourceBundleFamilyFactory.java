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
package org.hippoecm.hst.resourcebundle.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
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
    private final Credentials credentials;

    public HippoRepositoryResourceBundleFamilyFactory(Repository repository, Credentials credentials) {
        this.repository = repository;
        this.credentials = credentials;
    }

    @Override
    public ResourceBundleFamily createBundleFamily(String basename) {
        ResourceBundleFamily bundleFamily = null;
        Session session = null;

        try {
            String statement = 
                    "//element(*, resourcebundle:resourcebundle)[@resourcebundle:id='" + basename + 
                    "' and (@hippo:availability='live' or @hippo:availability='preview')]";
            session = repository.login(credentials);
            Query query = session.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
            QueryResult result = query.execute();

            List<Node> bundleNodes = new ArrayList<Node>();

            for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext(); ) {
                Node bundleNode = nodeIt.nextNode();

                if (bundleNode != null) {
                    bundleNodes.add(bundleNode);
                }
            }

            if (!bundleNodes.isEmpty()) {
                bundleFamily = createResourceBundleFamily(basename, bundleNodes);
            }

            if (bundleFamily == null) {
                bundleFamily = PLACE_HOLDER_EMPTY_RESOURCE_BUNDLE_FAMILY;
            }
        } catch (RepositoryException e) {
            log.warn("Fail to query resource bundle node", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }

        return bundleFamily;
    }

    private ResourceBundleFamily createResourceBundleFamily(final String basename, Collection<Node> bundleNodes) throws RepositoryException {
        MutableResourceBundleFamily bundleFamily = new DefaultMutableResourceBundleFamily(basename);

        for (Node bundleNode : bundleNodes) {
            String [] availabilities = getPropertyAsStringArray(bundleNode, "hippo:availability");
            boolean availableOnLive = ArrayUtils.contains(availabilities, "live");
            boolean availableOnPreview = ArrayUtils.contains(availabilities, "preview");
            String [] keys = getPropertyAsStringArray(bundleNode, "resourcebundle:keys");

            if (bundleNode.hasProperty("resourcebundle:messages")) {
                try {
                    String [] messages = getPropertyAsStringArray(bundleNode, "resourcebundle:messages");
    
                    if (messages != null) {
                        Object[][] contents = createListResourceBundleContents(keys, messages);
                        ResourceBundle defaultBundle = new SimpleListResourceBundle(contents);
    
                        if (availableOnLive) {
                            bundleFamily.setDefaultBundle(defaultBundle);
                        } else if (availableOnPreview) {
                            bundleFamily.setDefaultBundleForPreview(defaultBundle);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to load default resource bundle", e);
                }
            }

            for (PropertyIterator propIt = bundleNode.getProperties("resourcebundle:messages_*"); propIt.hasNext(); ) {
                Property prop = propIt.nextProperty();

                if (prop != null) {
                    Locale locale = null;

                    try {
                        locale = LocaleUtils.toLocale(prop.getName().substring("resourcebundle:messages_".length()));
                        String [] localizedMessages = getPropertyAsStringArray(prop);

                        if (localizedMessages != null) {
                            Object[][] contents = createListResourceBundleContents(keys, localizedMessages);
                            ResourceBundle localizedBundle = new SimpleListResourceBundle(contents);

                            if (availableOnLive) {
                                bundleFamily.setLocalizedBundle(locale, localizedBundle);
                            } else if (availableOnPreview) {
                                bundleFamily.setLocalizedBundleForPreview(locale, localizedBundle);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load resource bundle for locale: " + locale, e);
                    }
                }
            }
        }

        return bundleFamily;
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

    private Object[][] createListResourceBundleContents(String [] keys, String [] messages) {
        Object [][] contents = new Object[keys.length][];

        for (int i = 0; i < keys.length; i++) {
            String message = (i < messages.length ? messages[i] : null);
            contents[i] = new Object[] { keys[i], message };
        }

        return contents;
    }
}
