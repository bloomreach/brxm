/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.model.hst;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class HstConfigurationServiceTest extends BaseRepositoryTest {

    private static Logger log = LoggerFactory.getLogger(HstConfigurationServiceTest.class);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        createHstRootConfig();
    }

    @Test
    public void testBuild() throws Exception {
        HstConfigurationService service = new HstConfigurationService("test", getContext());
        final HstConfiguration configuration = service.build();
        assertTrue(configuration != null);
        assertEquals("test", configuration.getName());

    }
}
