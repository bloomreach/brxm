/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.autoexport;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.observation.Event;

public final class Constants {

    private Constants() {
    }

    public static final String SYSTEM_ALLOWED_PROPERTY_NAME = "repo.autoexport.allowed";
    public static final String SYSTEM_ENABLED_PROPERTY_NAME = "repo.autoexport.enabled";
    public static final String SERVICE_CONFIG_PATH = "/hippo:configuration/hippo:modules/autoexport/hippo:moduleconfig";
    public static final String CONFIG_ENABLED_PROPERTY_NAME = "autoexport:enabled";
    public static final String CONFIG_MODULES_PROPERTY_NAME = "autoexport:modules";
    public static final String CONFIG_EXCLUDED_PROPERTY_NAME = "autoexport:excluded";
    public static final String CONFIG_FILTER_UUID_PATHS_PROPERTY_NAME = "autoexport:filteruuidpaths";
    public static final String CONFIG_LAST_REVISION_PROPERTY_NAME = "autoexport:lastrevision";
    public static final String CONFIG_NTR_LAST_MODIFIED_PROPERTY_NAME = "autoexport:ntrlastmodified";
    public static final String SYSTEM_NODETYPES_PATH = "/jcr:system/jcr:nodeTypes/";

    public static final String DEFAULT_MAIN_CONFIG_FILE = "main.yaml";

    public static final String LOGGER_NAME = "org.onehippo.cms.autoexport";

    private static final Map<Integer, String> eventTypeMap =
            Collections.unmodifiableMap(
                Stream.of(
                    new SimpleEntry<>(Event.NODE_ADDED, "nodeadded"),
                    new SimpleEntry<>(Event.NODE_MOVED, "nodemoved"),
                    new SimpleEntry<>(Event.NODE_REMOVED, "noderemoved"),
                    new SimpleEntry<>(Event.PROPERTY_ADDED, "propertyadded"),
                    new SimpleEntry<>(Event.PROPERTY_CHANGED, "propertychanged"),
                    new SimpleEntry<>(Event.PROPERTY_REMOVED, "propertyremoved"),
                    new SimpleEntry<>(Event.PERSIST, "persist")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

    public static String getJCREventTypeName(final int eventType) {
        return eventTypeMap.get(eventType);
    }
}
