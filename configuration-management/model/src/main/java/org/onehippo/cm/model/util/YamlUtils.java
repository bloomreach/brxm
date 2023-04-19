/*
 * Copyright 2023 Bloomreach
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
package org.onehippo.cm.model.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

public final class YamlUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(YamlUtils.class);

    private static final String PROPERTY_YAML_CODE_POINTS_LIMIT = "yaml.codePoints.limit";
    private static final int YAML_CODE_POINTS_LIMIT = 50 * 1024 * 1024; // 50 MB

    private static int yamlCodePointsLimit = YAML_CODE_POINTS_LIMIT;

    static {
        try {
            yamlCodePointsLimit = Integer.parseInt(System.getProperty(PROPERTY_YAML_CODE_POINTS_LIMIT,
                    YAML_CODE_POINTS_LIMIT + ""));
        } catch (final NumberFormatException e) {
            LOGGER.warn("System property {} should be a valid integer. Code point limit is set to default {}",
                    PROPERTY_YAML_CODE_POINTS_LIMIT, YAML_CODE_POINTS_LIMIT);
        }
    }

    private YamlUtils() {
    }

    public static Yaml createYamlParser() {

        final LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit(yamlCodePointsLimit);
        return new Yaml(new SafeConstructor(), new Representer(), new DumperOptions(), loaderOptions);
    }
}
