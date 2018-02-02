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
import org.onehippo.cms7.essentials.BaseRepositoryTest;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id: HippoNodeUtilsTest.java 167907 2013-06-17 08:34:55Z mmilicevic $"
 */
public class HippoNodeUtilsTest extends BaseRepositoryTest {

    @Test
    public void testGetPrefixFromType() throws Exception {
        assertEquals("prefix", HippoNodeUtils.getPrefixFromType("prefix:name"));
        assertEquals("", HippoNodeUtils.getPrefixFromType(":name"));
        assertEquals(null, HippoNodeUtils.getPrefixFromType("name"));
        assertEquals(null, HippoNodeUtils.getPrefixFromType(""));
    }
}
