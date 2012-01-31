/*
 *  Copyright 2012 Hippo.
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
package org.onehippo.repository.testutils.test;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.fail;
import static org.onehippo.repository.testutils.AssertLogMessage.assertMessage;
import static org.onehippo.repository.testutils.AssertLogMessage.expectMessage;
import static org.onehippo.repository.testutils.AssertLogMessage.expectAny;
import static org.onehippo.repository.testutils.AssertLogMessage.expectNone;
import static org.onehippo.repository.testutils.AssertLogMessage.expectRemove;

public class LogTest {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    protected static final Logger log = LoggerFactory.getLogger(LogTest.class);

    public LogTest() {
    }

    @Before
    public void setUp() {
        org.onehippo.repository.testutils.AssertLogMessage.expectNone();
    }

    @Test
    public void testSurpressedLogMessagePasses() {
        log.debug("debug message");
        log.info("info message");
    }

    @Test
    public void testWarnMessageFails() {
        try {
            log.warn("warn message");
            fail("Expected unit test to fail due to generated log message");
        } catch(AssertionError ex) {
            if(!ex.getMessage().matches("Unexpected.*message.*stating: warn message")) {
                // not the exception we were asserting for received, rethrow original
                throw ex;
            }
        } catch(Throwable t) {
        }
    }

    @Test
    public void testExpectedMessagePasses() {
        expectMessage("warn.*ssage");
        log.warn("warn message");
    }

    @Test
    public void testExpectedMessagesPasses() {
        expectMessage("warn.*ssage");
        expectMessage("second.*ssage");
        log.warn("warn message");
        log.error("second message");
    }

    @Test
    public void testExpectedMessagesTwicePasses() {
        expectMessage("warn.*ssage");
        log.warn("warn message");
        log.error("warn or error message");
    }

    @Test
    public void testExpectedUnexpectedAfterExpectedMessage() {
        expectMessage("warn.*ssage");
        log.warn("warn message");
        expectRemove("warn.*ssage");
        try {
            log.warn("warn message");
            fail("Expected unit test to fail due to generated log message");
        } catch(AssertionError ex) {
            if(!ex.getMessage().matches("Unexpected.*message.*stating: warn message")) {
                // not the exception we were asserting for received, rethrow original
                throw ex;
            }
        }
    }

    @Test
    public void testExpectNoneAfterAny() {
        expectAny();
        log.warn("warn message");
        expectNone();
        try {
            log.warn("warn message");
            fail("Expected unit test to fail due to generated log message");
        } catch(AssertionError ex) {
            if(!ex.getMessage().matches("Unexpected.*message.*stating: warn message")) {
                // not the exception we were asserting for received, rethrow original
                throw ex;
            }
        }
    }

    @Test
    public void testExpectedAnyMessagePasses() {
        expectAny();
        log.warn("warn message");
        log.error("second message");
    }

    @Test
    public void testAssertMessageSucceeds() {
        expectMessage("warn.*ssage");
        log.warn("warn message");
        assertMessage("warn.*ssage");
    }

    @Test
    public void testAssertMessageFails() {
        try {
            expectMessage("warn.*ssage");
            assertMessage("warn.*ssage");
        } catch(AssertionError ex) {
            if(!ex.getMessage().matches("Expected log message never produced: warn.*ssage")) {
                // not the exception we were asserting for received, rethrow original
                throw ex;
            }
        }
    }

    @Test
    public void testErrorMessageFails() {
        try {
            log.error("error message");
            fail("Expected unit test to fail due to generated log message");
        } catch(AssertionError ex) {
            if(!ex.getMessage().matches("Unexpected.*message.*stating: error message")) {
                // not the exception we were asserting for received, rethrow original
                throw ex;
            }
        }
    }
}
