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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Simple YAML parser which maps standard Java objects only using SnakeYaml {@link SafeConstructor}.
 * Once parsed these objects can (currently) be accessed as {@link #asMap(String)}, {@link #asList(String)}
 * or {@link #asObject(String)} with an optional path parameter for direct navigating into the yaml object tree.
 * The path navigation parameter for these methods assume (parent) Yaml keys separated with a / character.
 * <p>
 * Trivial usage example:
 * <pre>
 *    PlainYamlObject pyo = new PlainYamlObject("map:\n" +
 *                                              "  list:\n" +
 *                                              "    - foo\n" +
 *                                              "    - bar\n");
 *    Map rootMap = pyo.asMap().get();
 *    Map map = pyo.asMap("map").get();
 *    List list = pyo.asList("map/list").get();
 * </pre>
 * </p>
 */
public class PlainYamlObject {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final Object yamlObject;

    public PlainYamlObject(final String yamlString) {
        yamlObject = new Yaml(new SafeConstructor()).load(yamlString);
    }

    public PlainYamlObject(final InputStream io) throws IOException {
        try {
            yamlObject = new Yaml(new SafeConstructor()).load(io);
        } finally {
            io.close();
        }
    }

    private Object mapPath(final String path) {
        Object current = yamlObject;
        final String[] pathKeys = (path != null)
                ? Arrays.stream(path.split("/")).filter(StringUtils::isNotEmpty).toArray(String[]::new)
                : EMPTY_STRING_ARRAY;

        // first map pathKeys except the last
        for (int index = 0; index < pathKeys.length-1; index++ ) {
            final String p = pathKeys[index];
            if (current == null || !(current instanceof Map)) {
                break;
            }
            current = ((Map)current).get(p);
        }
        // if we do have a remaining/last pathKey current must be a Map to return it
        if (current != null && pathKeys.length > 0) {
            if (current instanceof Map) {
                current = ((Map)current).get(pathKeys[pathKeys.length-1]);
            } else {
                current = null;
            }
        }
        return current;
    }

    public Optional<Object> asObject() {
        return asObject(null);
    }

    public Optional<Object> asObject(final String path) {
        Object current = mapPath(path);
        return current != null ? Optional.of(current) : Optional.empty();
    }

    public Optional<Map> asMap() {
        return asMap(null);
    }

    public Optional<Map> asMap(final String path) {
        Object current = mapPath(path);
        return current instanceof Map ? Optional.of((Map)current) : Optional.empty();
    }

    public Optional<List> asList() {
        return asList(null);
    }

    public Optional<List> asList(final String path) {
        Object current = mapPath(path);
        return current instanceof List ? Optional.of((List)current) : Optional.empty();
    }
}
