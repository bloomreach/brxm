/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.mapper;

import org.onehippo.cm.api.model.DefinitionNode;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractFileMapper implements ValueFileMapper {

    static final String PATH_DELIMITER = "/";
    static final String NS_DELIMITER = ":";
    public static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
    public static final String HIPPOGALLERY_IMAGE = "hippogallery:image";
    public static final String HIPPOGALLERY_IMAGESET = "hippogallery:imageset";
    public static final String HIPPOGALLERY_FILENAME = "hippogallery:filename";
    public static final String JCR_MIME_TYPE = "jcr:mimeType";
    public static final String DEFAULT_EXTENSION = "bin";
    public static final String DOT_SEPARATOR = ".";


    public static Map<String, String> mimeTypesToExtMap = Collections.unmodifiableMap(Stream.of(
            entry("image/jpg", "jpg"),
            entry("image/jpeg", "jpg"),
            entry("image/png", "png"),
            entry("image/gif", "gif"),
            entry("image/bmp", "bmp"),
            entry("application/pdf", "pdf"))
            .collect(entriesToMap()));

    protected String getFileExtension(DefinitionNode node) {
        return node.getProperties().containsKey(JCR_MIME_TYPE) ? mimeTypesToExtMap.getOrDefault(node.getProperties().get(JCR_MIME_TYPE).getValue().getString(), DEFAULT_EXTENSION)
                : DEFAULT_EXTENSION;
    }

    protected String constructPathFromJcrPath(String jcrPath) {
        final String[] split = jcrPath.split(PATH_DELIMITER);
        return Arrays.stream(split).map(this::normalizeJcrName).collect(Collectors.joining(PATH_DELIMITER));
    }

    protected String normalizeJcrName(String part) {
        return part.contains(NS_DELIMITER) ? part.substring(part.indexOf(NS_DELIMITER) + 1) : part;
    }

    protected boolean isType(DefinitionNode node, String nodeType) {
        return node != null && node.getProperties().containsKey(JCR_PRIMARY_TYPE) && node.getProperties().get(JCR_PRIMARY_TYPE).getValue().getString().equals(nodeType);
    }


    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public static <K, U> Collector<Map.Entry<K, U>, ?, Map<K, U>> entriesToMap() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }
}
