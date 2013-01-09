/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.logging;

import org.hippoecm.hst.logging.Logger;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

import static org.junit.Assert.assertTrue;


public class TestSlf4jLogger {

    private org.slf4j.Logger slf4jLogger;
    
    @Before
    public void setUp() throws Exception {
        slf4jLogger = LoggerFactory.getLogger(getClass());
        Assume.assumeTrue(slf4jLogger instanceof LocationAwareLogger);
    }
    
    @Test
    public void testUsingLogger() {
        //       Improve this later with asserting...
        Logger logger = new Slf4jLogger(slf4jLogger, getClass().getName());
        logger.debug("Simulating an error (Ignore this; this is only for unit testing)");
        logger.debug("Simulating an error (Ignore this; this is only for unit testing) : {}", "arg1");
        logger.debug("Simulating an error (Ignore this; this is only for unit testing) : {}, {}", "arg1", "arg2");
        logger.debug("Simulating an error (Ignore this; this is only for unit testing) : {}, {}, {}", new Object [] { "arg1", "arg2", "arg3" });
        logger.debug("Simulating an error (Ignore this; this is only for unit testing)", new RuntimeException());
        assertTrue("logger for org.hippoecm.hst.core.logging.TestSlf4jLogger should be on DEBUG", logger.isDebugEnabled());
    }
    
}
