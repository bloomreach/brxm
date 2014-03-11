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

package org.onehippo.cms7.essentials;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public abstract class BaseResourceTest extends BaseTest {

    private static Logger log = LoggerFactory.getLogger(BaseResourceTest.class);

    @Override
    public void setUp() throws Exception {

        final URL resource = getClass().getResource("/project");
        final String path = resource.getPath();
        final Path myDir = new File(path).toPath();
        setProjectRoot(myDir);
        getPluginContextFile();
    }

    @Override
    public void tearDown() throws Exception {
        // do not delete files
    }
}
