/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.services;

import org.junit.Test;
import org.onehippo.cms7.essentials.sdk.api.service.RebuildService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RebuildServiceImplTest {

    @Test
    public void collect_plugin_ids() {
        final RebuildService rebuildService = new RebuildServiceImpl();

        assertTrue(rebuildService.getRequestingPluginIds().isEmpty());

        rebuildService.requestRebuild("foo");

        assertEquals(1, rebuildService.getRequestingPluginIds().size());
        assertTrue(rebuildService.getRequestingPluginIds().contains("foo"));

        rebuildService.requestRebuild("bar");

        assertEquals(2, rebuildService.getRequestingPluginIds().size());
        assertTrue(rebuildService.getRequestingPluginIds().contains("foo"));
        assertTrue(rebuildService.getRequestingPluginIds().contains("bar"));

        rebuildService.requestRebuild("foo");

        assertEquals(2, rebuildService.getRequestingPluginIds().size());
        assertTrue(rebuildService.getRequestingPluginIds().contains("foo"));
        assertTrue(rebuildService.getRequestingPluginIds().contains("bar"));
    }
}
