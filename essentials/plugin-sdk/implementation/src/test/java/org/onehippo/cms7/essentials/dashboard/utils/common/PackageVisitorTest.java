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

package org.onehippo.cms7.essentials.dashboard.utils.common;

import java.nio.file.Files;
import java.util.Collection;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id: PackageVisitorTest.java 174288 2013-08-19 16:21:19Z mmilicevic $"
 */
public class PackageVisitorTest extends BaseResourceTest {


    @Test
    public void testGetPackages() throws Exception {
        final PackageVisitor visitor = new PackageVisitor();
        Files.walkFileTree(getContext().getSiteDirectory().toPath(), visitor);
        final Collection<String> packages = visitor.getPackages();
        assertTrue(packages.size() >= 8);

    }
}
