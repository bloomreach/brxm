/*
 *  Copyright 2018-2023 Bloomreach
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
package org.hippoecm.hst.configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ConfigurationConstants {

    public static final String CONTAINER_RESOURCE_PIPELINE_NAME = "ContainerResourcePipeline";
    public static final String WEB_FILE_PIPELINE_NAME = "WebFilePipeline";

    public static final Set<String> CDN_SUPPORTED_PIPELINES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(CONTAINER_RESOURCE_PIPELINE_NAME, WEB_FILE_PIPELINE_NAME)));
}
