/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.config;

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PluginFileServiceTest extends BaseTest {

    @Inject private PluginFileService pluginFileService;

    @Test
    public void testPluginService() throws Exception {
        final String filename = "project-settings";

        final ProjectSettingsBean bean = new ProjectSettingsBean();
        bean.setProjectNamespace("myNamespace");
        bean.setSelectedBeansPackage("testBeanPackage");
        bean.setSelectedComponentsPackage("testComponentPackage");
        bean.setSelectedRestPackage("testRestPackage");
        bean.setSelectedProjectPackage("testProjectPackage");
        assertTrue(pluginFileService.write(filename, bean));

        final ProjectSettingsBean myBean = pluginFileService.read(filename, bean.getClass());
        assertEquals(myBean.getProjectNamespace(), bean.getProjectNamespace());
        assertEquals(myBean.getSelectedBeansPackage(), bean.getSelectedBeansPackage());
        assertEquals(myBean.getSelectedComponentsPackage(), bean.getSelectedComponentsPackage());
        assertEquals(myBean.getSelectedRestPackage(), bean.getSelectedRestPackage());
        assertEquals(myBean.getSelectedProjectPackage(), bean.getSelectedProjectPackage());
    }
}