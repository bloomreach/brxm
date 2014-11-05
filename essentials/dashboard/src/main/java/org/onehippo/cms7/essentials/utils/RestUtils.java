/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.onehippo.cms7.essentials.dashboard.model.PluginRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public final class RestUtils {

    private static final Logger log = LoggerFactory.getLogger(RestUtils.class);

    private RestUtils() {
    }

    public static RestfulList<PluginRestful> parsePlugins(final String jsonString) {
        try {

            if (Strings.isNullOrEmpty(jsonString)) {
                return new RestfulList<>();
            }
            final ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            final RestfulList<PluginRestful> restfulList = mapper.readValue(jsonString, RestfulList.class);
            return restfulList;
        } catch (Exception e) {
            log.error("Error parsing  plugins ", e);
        }
        return new RestfulList<>();
    }
}
