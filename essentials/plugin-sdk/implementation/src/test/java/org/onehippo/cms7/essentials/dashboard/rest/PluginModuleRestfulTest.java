/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.dashboard.model.Library;
import org.onehippo.cms7.essentials.dashboard.model.PrefixedLibraryList;
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
        final PrefixedLibraryList prefixedLibrary = new PrefixedLibraryList("components");
        prefixedLibrary.addLibrary(new Library("html5shiv", "excanvas.js", "IE <= 8"));
        prefixedLibrary.addLibrary(new Library("ExplorerCanvas", "html5shiv-printshiv.js", "IE <= 8"));
        restful.addLibrary("libraries", prefixedLibrary);
        //
        final PrefixedLibraryList hippoLibrary = new PrefixedLibraryList();
        hippoLibrary.addLibrary(new Library("components/hippo-plugins", "dist/js/main.js"));
        restful.addLibrary("hippo-plugins", hippoLibrary);
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(restful);
        log.info("json {}", json);
        restful = mapper.readValue(json, PluginModuleRestful.class);
        log.info("json {}", restful);
        final Map<String, PrefixedLibraryList> includes = restful.getIncludes();
        assertEquals("Expected 2 libraries", 2, includes.size());
    }
}
