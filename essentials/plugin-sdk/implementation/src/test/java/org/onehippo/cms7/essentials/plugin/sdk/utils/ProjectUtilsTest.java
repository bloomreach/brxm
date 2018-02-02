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

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ProjectUtilsTest {

    @Test
    public void get_base_project_dir() {
        System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, "/foo/bar");

        assertEquals("/foo/bar", ProjectUtils.getBaseProjectDirectory());

        System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, " ");

        try {
            ProjectUtils.getBaseProjectDirectory();
            fail("Should not get here.");
        } catch (IllegalStateException e) {
            assertEquals("System property 'project.basedir' was null or empty. " +
                    "Please start your application with -D=project.basedir=/project/path", e.getMessage());
        }
    }

    @Test
    public void get_essentials_module_name() throws Exception {
        assertEquals("essentials", ProjectUtils.getEssentialsModuleName());

        System.setProperty(EssentialConst.ESSENTIALS_BASEDIR_PROPERTY, "test");

        assertEquals("test", ProjectUtils.getEssentialsModuleName());
    }
}
