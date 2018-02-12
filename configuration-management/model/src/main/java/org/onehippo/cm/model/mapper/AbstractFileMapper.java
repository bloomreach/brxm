/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.mapper;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.DefinitionNode;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_MIMETYPE;

public abstract class AbstractFileMapper implements ValueFileMapper {

    static final String PATH_DELIMITER = "/";
    static final String NS_DELIMITER = ":";
    public static final String DEFAULT_EXTENSION = "bin";
    public static final String DOT_SEPARATOR = ".";


    // TODO: use a larger mapping from e.g. Apache Tika
    public static Map<String, String> mimeTypesToExtMap = Collections.unmodifiableMap(Stream.of(
            entry("image/jpg", "jpg"),
            entry("image/jpeg", "jpg"),
            entry("image/png", "png"),
            entry("image/gif", "gif"),
            entry("image/bmp", "bmp"),
            entry("application/pdf", "pdf"))
            .collect(entriesToMap()));

    protected String getFileExtension(DefinitionNode node) {
        return node.getProperty(JCR_MIMETYPE) != null ? mimeTypesToExtMap.getOrDefault(node.getProperty(JCR_MIMETYPE).getValue().getString(), DEFAULT_EXTENSION)
                : DEFAULT_EXTENSION;
    }

    public static String constructFilePathFromJcrPath(String path) {
        JcrPath jcrPath = JcrPaths.getPath(path);
        return jcrPath.stream().map(JcrPathSegment::toString).map(AbstractFileMapper::mapNodeNameToFileName).collect(Collectors.joining(PATH_DELIMITER, "/", ""));
    }

    protected static String mapNodeNameToFileName(String name) {
        return name.contains(NS_DELIMITER) ? name.substring(name.indexOf(NS_DELIMITER) + 1) : name;
    }

    protected boolean isType(DefinitionNode node, String nodeType) {
        return node != null && node.getProperty(JCR_PRIMARYTYPE) != null && node.getProperty(JCR_PRIMARYTYPE).getValue().getString().equals(nodeType);
    }

    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public static <K, U> Collector<Map.Entry<K, U>, ?, Map<K, U>> entriesToMap() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }
}
