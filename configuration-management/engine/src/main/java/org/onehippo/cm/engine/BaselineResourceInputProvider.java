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
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.engine.Constants.ACTIONS_NODE;
import static org.onehippo.cm.engine.Constants.ACTIONS_YAML;
import static org.onehippo.cm.engine.Constants.CONTENT_TYPE;
import static org.onehippo.cm.engine.Constants.DEFINITIONS_TYPE;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_FOLDER;
import static org.onehippo.cm.engine.Constants.HCM_MODULE_YAML;
import static org.onehippo.cm.engine.Constants.MODULE_DESCRIPTOR_NODE;

/**
 * Provides access to InputStreams based on JCR Nodes stored in the configuration baseline.
 */
public class BaselineResourceInputProvider implements ResourceInputProvider {

    private static final Logger log = LoggerFactory.getLogger(BaselineResourceInputProvider.class);

    /**
     * Base Node from which path references are relative. Typically the HCM_CONTENT_FOLDER child of a module.
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
        String result = resourcePath;
        if (resourcePath.startsWith("/")) {
            // TODO this is an ugly hack in part because RIP uses config root instead of module root
            // special case handling for descriptor and actions
            if (resourcePath.equals("/../"+ HCM_MODULE_YAML)) {
                // short-circuit here, because we want to skip JCR name escaping
                return "../"+MODULE_DESCRIPTOR_NODE;
            }
            if (resourcePath.equals("/../"+ACTIONS_YAML)) {
                // short-circuit here, because we want to skip JCR name escaping
                return "../"+ ACTIONS_NODE;
            }

            result = StringUtils.stripStart(result, "/");
        }
        else {
            final String sourcePath = source.getPath();
            int lastSlash = sourcePath.lastIndexOf('/');
            if (lastSlash < 0) {
                result = resourcePath;
            }
            else {
                result = sourcePath.substring(0, lastSlash+1) + resourcePath;
            }
        }

        // escape JCR-illegal chars here, since resource paths are intended to be filesystem paths, not JCR paths
        String[] pathSegments = result.split("/");
        for (int i = 0; i < pathSegments.length; i++) {
            pathSegments[i] = Text.escapeIllegalJcrChars(pathSegments[i]);
        }
        return String.join("/", pathSegments);
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
        // we expect baseNode to be HCM_CONFIG_FOLDER for this module
        // search for definitions nodes by type
        return searchByType(DEFINITIONS_TYPE, baseNode);
    }

    /**
     * @return a List of all nodes representing content sources for this module
     * @throws RepositoryException
     */
    public List<Node> getContentSourceNodes() throws RepositoryException {
        // we expect baseNode to be HCM_CONFIG_FOLDER for this module
        // if no HCM_CONTENT_FOLDER node exists for this module, return a trivial empty result
        if (!baseNode.getParent().hasNode(HCM_CONTENT_FOLDER)) {
            return Collections.emptyList();
        }

        // otherwise, search for content nodes by type
        final Node searchBase = baseNode.getParent().getNode(HCM_CONTENT_FOLDER);
        return searchByType(CONTENT_TYPE, baseNode);
    }

    /**
     * Helper to search for nodes by type using recursive search.
     */
    private List<Node> searchByType(final String type, final Node searchBase) throws RepositoryException {

        // accumulate results into a List, since we expect them to be few and NodeIterator is annoying
        List<Node> result = new ArrayList<>();

        // recursive search
        findDescendantsByType(type, searchBase, result);

        return result;
    }

    /**
     * Helper for recursive search by node type.
     * @param type the primary node type string that we want
     * @param searchBase the current node to search
     * @param result accumulator for matching Nodes
     * @throws RepositoryException
     */
    private void findDescendantsByType(final String type, final Node searchBase, List<Node> result) throws RepositoryException {
        for (Node childNode : new NodeIterable(searchBase.getNodes())) {
            if (childNode.isNodeType(type)) {
                result.add(childNode);
            }
            findDescendantsByType(type, childNode, result);
        }
    }
}
