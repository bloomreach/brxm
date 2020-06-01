/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LatestBundledJQueryResourceReferenceTest {
    static final String WICKET_RESOURCE_PATH = "org/apache/wicket/resource/";

    @Test
    public void testVersion3ResourceIsBundled() {
        String bundledJQuery = LatestBundledJQueryResourceReference.getVersion3();
        URL resource = this.getClass().getClassLoader().getResource(WICKET_RESOURCE_PATH + bundledJQuery);
        assertNotNull(String.format("%s not found in %s", bundledJQuery, WICKET_RESOURCE_PATH), resource);
    }

    @Test
    public void testLatestResource() {
        String bundledJQuery = new LatestBundledJQueryResourceReference().getLatestVersion();
        // Check getLatestVersion() did not fall back to VERSION_1
        assertNotEquals("No current JQuery version found", LatestBundledJQueryResourceReference.VERSION_1, bundledJQuery);
        URL resource = this.getClass().getClassLoader().getResource(WICKET_RESOURCE_PATH + bundledJQuery);
        assertNotNull(String.format("%s not found in %s", bundledJQuery, WICKET_RESOURCE_PATH), resource);
    }

    @Test
    public void testLatestResourceIsV3() {
        String bundledJQuery = new LatestBundledJQueryResourceReference().getLatestVersion();
        assertTrue("Expecting a JQuery 3.x release but found " + bundledJQuery, bundledJQuery.startsWith("jquery/jquery-3"));
    }
}