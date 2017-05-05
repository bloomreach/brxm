/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.engine.Constants.ACTIONS_NODE;
import static org.onehippo.cm.engine.Constants.ACTIONS_YAML;
import static org.onehippo.cm.engine.Constants.CONTENT_TYPE;
import static org.onehippo.cm.engine.Constants.DEFINITIONS_TYPE;
import static org.onehippo.cm.engine.Constants.MODULE_DESCRIPTOR_NODE;
import static org.onehippo.cm.engine.Constants.REPO_CONFIG_YAML;
import static org.onehippo.cm.engine.Constants.REPO_CONTENT_FOLDER;

/**
 * Provides access to InputStreams based on JCR Nodes stored in the configuration baseline.
 */
public class BaselineResourceInputProvider implements ResourceInputProvider {

    private static final Logger log = LoggerFactory.getLogger(BaselineResourceInputProvider.class);

    /**
     * Base Node from which path references are relative. Typically the REPO_CONTENT_FOLDER child of a module.
     */
    private Node baseNode;

    public BaselineResourceInputProvider(final Node baseNode) {
        this.baseNode = baseNode;
    }

    @Override
    public boolean hasResource(final Source source, final String resourcePath) {
        try {
            final String pathFromBase = makeBaseRelativePath(source, resourcePath);
            return baseNode.hasNode(pathFromBase);
        }
        catch (RepositoryException e) {
            throw new RuntimeException("Problem checking for existence of resource in baseline", e);
        }
    }

    @Override
    public InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException {
        try {
            final String pathFromBase = makeBaseRelativePath(source, resourcePath);
            Node resourceNode = baseNode.getNode(pathFromBase);
            Property primary = (Property) resourceNode.getPrimaryItem();

            // JCR API Spec defines conversion of String to binary using UTF-8, which is what we want here
            return primary.getBinary().getStream();
        }
        catch (RepositoryException e) {
            throw new RuntimeException("Problem loading stream for resource in baseline", e);
        }
    }

    /**
     * Helper method to handle convention that a path with leading slash is relative to baseNode rather than Source.
     * @param source a Source, which may be null if resourcePath starts with "/"
     * @param resourcePath a resource path reference -- iff starts with "/", a baseNode-relative path, else Source-relative
     * @return a path String normalized to be relative to the baseNode
     */
    protected static String makeBaseRelativePath(final Source source, final String resourcePath) {
        // TODO handle JCR name encoding / unencoding!!!!!

        if (resourcePath.startsWith("/")) {
            // TODO this is an ugly hack in part because RIP uses config root instead of module root
            // special case handling for descriptor and actions
            if (resourcePath.equals("/../"+REPO_CONFIG_YAML)) {
                return "../"+MODULE_DESCRIPTOR_NODE;
            }
            if (resourcePath.equals("/../"+ACTIONS_YAML)) {
                return "../"+ ACTIONS_NODE;
            }

            return StringUtils.stripStart(resourcePath, "/");
        }
        else {
            final String sourcePath = source.getPath();
            int lastSlash = sourcePath.lastIndexOf('/');
            if (lastSlash < 0) {
                return resourcePath;
            }
            else {
                return sourcePath.substring(0, lastSlash+1) + resourcePath;
            }
        }
    }

    /**
     * This is a stub implementation, as there is no standard way to compose a URL for a JCR Node, and no framework
     * support for loading an InputStream from such a reference. Callers beware!
     * @return a stub URL
     */
    @Override
    public URL getBaseURL() {
        try {
            return new URL("jcr:/"+baseNode.getPath());
        }
        catch (MalformedURLException|RepositoryException e) {
            throw new RuntimeException("Problem creating baseURL", e);
        }
    }

    /**
     * @return base Node from which resource paths may be relative
     */
    public Node getBaseNode() {
        return baseNode;
    }

    /**
     * @return a List of all nodes representing config definition sources for this module
     * @throws RepositoryException
     */
    public List<Node> getConfigSourceNodes() throws RepositoryException {
        // we expect baseNode to be REPO_CONFIG_FOLDER for this module
        // search for definitions nodes by type
        return searchByType(DEFINITIONS_TYPE, baseNode);
    }

    /**
     * @return a List of all nodes representing content sources for this module
     * @throws RepositoryException
     */
    public List<Node> getContentSourceNodes() throws RepositoryException {
        // we expect baseNode to be REPO_CONFIG_FOLDER for this module
        // if no REPO_CONTENT_FOLDER node exists for this module, return a trivial empty result
        if (!baseNode.getParent().hasNode(REPO_CONTENT_FOLDER)) {
            return Collections.emptyList();
        }

        // otherwise, search for content nodes by type
        final Node searchBase = baseNode.getParent().getNode(REPO_CONTENT_FOLDER);
        return searchByType(CONTENT_TYPE, baseNode);
    }

    /**
     * Helper to search for nodes by type using xpath queries.
     */
    private List<Node> searchByType(final String type, final Node searchBase) throws RepositoryException {
        QueryManager qm = searchBase.getSession().getWorkspace().getQueryManager();
        final String queryString = "/jcr:root" + Text.encodeIllegalXMLCharacters(searchBase.getPath()) + "//element(*," + type + ")";

        log.debug("Searching for sources with query: {}", queryString);

        // accumulate results into a List, since we expect them to be few and NodeIterator is annoying
        List<Node> result = new ArrayList<>();
        for (NodeIterator ni = qm.createQuery(queryString, Query.XPATH).execute().getNodes(); ni.hasNext();) {
            result.add(ni.nextNode());
        }
        return result;
    }
}
