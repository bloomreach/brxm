/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.sitemap.components.util;

import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.StringCodecFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class with utility methods
 */
public final class RepositoryUtils {
    private RepositoryUtils() {} // Hide constructor for utility class

    /**
     * Takes a basePath and a path to localize. Returns the relative path from the basepath for the path to localize
     * @param basePath the basepath from which to generate a relative path
     * @param pathToLocalize the path to localize
     * @return the relative path to the path to localize from the basepath
     */
    public static String localizePath(final String basePath, final String pathToLocalize) {
        String normalizedBasePath = PathUtils.normalizePath(basePath);
        String normalizedPathToLocalize = PathUtils.normalizePath(pathToLocalize);
        if (normalizedPathToLocalize.startsWith(normalizedBasePath)) {
            return PathUtils.normalizePath(normalizedPathToLocalize.substring(normalizedBasePath.length()));
        } else {
            return pathToLocalize;
        }
    }

    /**
     * Takes a path and ISO9075 encodes every part of it
     * @param path The path to encode
     * @return the ISO9075 encoded path
     */
    public static String encodePath(final String path) {
        String[] parts = path.split("/", -1);
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (final String part : parts) {
            if (part.equals("*")) {
                // Do not encode stars..
                sb.append(part);
            } else {
                sb.append(StringCodecFactory.ISO9075Helper.encodeLocalName(part));
            }
            i++;
            if (i < parts.length) {
                // Add a slash, but not after the last part
                sb.append("/");
            }
        }

        return sb.toString();
    }

    /**
     * JCR returns paths with indexed node names when there are two nodes with the same name under the same node.
     * This method returns a List of indexed node names, in order of appearance in the string from left to right
     * @param path the path to parse for indexed node names
     * @return a {@link List} of nodenames that are indexed
     */
    public static List<String> getIndexedNodeNames(String path) {
        List<String> nodeNamesWithIndex = new ArrayList<String>();
        String[] nodeNames = path.split("/");
        for (String nodeName : nodeNames) {
            Matcher indexedNodeMatcher = Pattern.compile("([\\w-]+)\\[\\d+\\]").matcher(nodeName);
            if (indexedNodeMatcher.matches()) {
                nodeNamesWithIndex.add(indexedNodeMatcher.group(1));
            }
        }
        return nodeNamesWithIndex;
    }

    /**
     * Checks whether indexed nodes in path B are also indexed in path A when Path A contains those paths
     *
     * @param pathA the first path
     * @param pathB the second path
     * @return <code>true</code> if the indexed node is not indexed in the query, <code>false</code> otherwise
     */
    public static boolean indexedNodesInPathBMatchIndexedNodesInPathAWhenPathAHasThatNode(
            final String pathA,
            final String pathB) {
        List<String> indexedNodesForPathA = getIndexedNodeNames(pathA);
        List<String> indexedNodesForPathB = getIndexedNodeNames(pathB);
        boolean indexedNodeInNodeNotIndexedInQuery = false;
        for (String indexedNode : indexedNodesForPathB) {
            if (pathA.contains(indexedNode) &&
                    !indexedNodesForPathA.contains(indexedNode)) {
                indexedNodeInNodeNotIndexedInQuery = true;
            }
        }
        return !indexedNodeInNodeNotIndexedInQuery;
    }
}
