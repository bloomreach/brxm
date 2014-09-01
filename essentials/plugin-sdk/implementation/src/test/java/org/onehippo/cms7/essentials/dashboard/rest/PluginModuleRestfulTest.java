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

package org.onehippo.cms7.essentials.dashboard.rest;

import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class PluginModuleRestfulTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(PluginModuleRestfulTest.class);

    @Test
    public void testGetApplication() throws Exception {
        PluginModuleRestful restful = new PluginModuleRestful();
        final PluginModuleRestful.PrefixedLibrary prefixedLibrary = new PluginModuleRestful.PrefixedLibrary("components");
        prefixedLibrary.addLibrary(new PluginModuleRestful.Library("html5shiv", "excanvas.js", "IE <= 8"));
        prefixedLibrary.addLibrary(new PluginModuleRestful.Library("ExplorerCanvas", "html5shiv-printshiv.js", "IE <= 8"));
        restful.addLibrary("libraries", prefixedLibrary);
        //
        final PluginModuleRestful.PrefixedLibrary hippoLibrary = new PluginModuleRestful.PrefixedLibrary();
        hippoLibrary.addLibrary(new PluginModuleRestful.Library("components/hippo-plugins", "dist/js/main.js"));
        restful.addLibrary("hippo-plugins", hippoLibrary);
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(restful);
        log.info("json {}", json);
        restful = mapper.readValue(json, PluginModuleRestful.class);
        log.info("json {}", restful);
        final Map<String, PluginModuleRestful.PrefixedLibrary> includes = restful.getIncludes();
        assertEquals("Expected 2 libraries", 2, includes.size());
    }
}
