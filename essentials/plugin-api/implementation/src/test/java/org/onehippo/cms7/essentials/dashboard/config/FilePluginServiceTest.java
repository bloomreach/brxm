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

package org.onehippo.cms7.essentials.dashboard.config;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FilePluginServiceTest extends BaseTest {

    @Test
    public void testPluginService() throws Exception {

        try (PluginConfigService service = new FilePluginService(getContext())) {
            final ProjectSettingsBean bean = new ProjectSettingsBean();
            bean.setSetupDone(true);
            bean.setProjectNamespace("myNamespace");
            bean.setSelectedBeansPackage("testBeanPackage");
            bean.setSelectedComponentsPackage("testComponentPackage");
            bean.setSelectedRestPackage("testRestPackage");
            final boolean written = service.write(bean);
            assertTrue(written);
            final ProjectSettingsBean myBean = service.read(bean.getClass());
            assertEquals(myBean.getProjectNamespace(), bean.getProjectNamespace());
            assertEquals(myBean.getSelectedBeansPackage(), bean.getSelectedBeansPackage());
            assertEquals(myBean.getSelectedComponentsPackage(), bean.getSelectedComponentsPackage());
            assertEquals(myBean.getSelectedRestPackage(), bean.getSelectedRestPackage());

        }
    }


}