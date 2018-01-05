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

package org.onehippo.cms7.essentials.dashboard.services;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

import static org.junit.Assert.assertEquals;

public class ProjectServiceImplTest extends ResourceModifyingTest {

    private ProjectServiceImpl projectService = new ProjectServiceImpl();

    @Test
    public void get_base_path() {
        System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, "/foo/bar");
        assertEquals("/foo/bar", projectService.getBasePath());
    }

    @Test
    public void testGetLog4jFiles() throws Exception {
        createModifiableFile("/services/project/empty.txt", "conf/log4j2.xml");
        createModifiableFile("/services/project/empty.txt", "conf/unrelated.xml");
        createModifiableFile("/services/project/empty.txt", "conf/log4j2-foo.xml");
        createModifiableFile("/services/project/empty.txt", "site/log4j2-bar.xml");

        List<String> fileNames = projectService.getLog4j2Files().stream().map(File::getName).collect(Collectors.toList());

        assertEquals(Arrays.asList("log4j2.xml", "log4j2-foo.xml"), fileNames);
    }
}
