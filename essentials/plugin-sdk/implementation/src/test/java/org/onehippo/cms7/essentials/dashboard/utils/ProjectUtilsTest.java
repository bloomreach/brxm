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

package org.onehippo.cms7.essentials.dashboard.utils;

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.dashboard.service.ProjectService;

import static org.junit.Assert.assertTrue;

public class ProjectUtilsTest extends BaseResourceTest {

    @Inject private ProjectService projectService;

    @Test
    public void testGetContextXml() throws Exception {
        assertTrue(projectService.getContextXmlPath().toFile().exists());
    }

    @Test
    public void testGetAssemblyFile() throws Exception {
        assertTrue(projectService.getAssemblyFolderPath().resolve("common-lib-component.xml").toFile().exists());
    }
}
