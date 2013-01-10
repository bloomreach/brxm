/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.standard;

import static junit.framework.Assert.assertTrue;

import java.math.BigDecimal;

import org.hippoecm.hst.mock.content.beans.standard.MockHippoResourceBean;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class HippoResourceTest {

    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(HippoResourceTest.class);
    private MockHippoResourceBean resource;

    @Before
    public void setUp() throws Exception {
        //resource = new MockHippoResourceBean();
        //resource.setLength(1024);
    }

    @Test
    public void testGetLengthKB() throws Exception {
        resource = new MockHippoResourceBean();
        resource.setLength(1024 / 8);
        assertTrue("Expected 0,125 KB, but got: " + resource.getLengthKB(), resource.getLengthKB().equals(new BigDecimal("0.125")));
        resource = new MockHippoResourceBean();
        resource.setLength(1024);
        assertTrue("Expected 1 KB, but got: " + resource.getLengthKB(), resource.getLengthKB().equals(new BigDecimal("1")));
    }

    @Test
    public void testGetLengthMB() throws Exception {
        resource = new MockHippoResourceBean();
        resource.setLength(1024 * 1024);
        assertTrue("Expected 1 MB, but got: " + resource.getLengthMB(), resource.getLengthMB().equals(new BigDecimal("1")));
        resource = new MockHippoResourceBean();
        resource.setLength(1024*1024 * 10);
        assertTrue("Expected 10 MB, but got: " + resource.getLengthMB(), resource.getLengthMB().equals(new BigDecimal("10")));

    }


}
