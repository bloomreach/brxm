/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.testutils.log4j;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log4jInterceptorTest {

    static final Logger log = LoggerFactory.getLogger(Log4jInterceptorTest.class);
    static final Logger logPackage = LoggerFactory.getLogger(Log4jInterceptorTest.class.getPackage().getName());
    static final Logger logOrgTestDebug = LoggerFactory.getLogger("org.test.debug");
    static final Logger logOrgTestInfo = LoggerFactory.getLogger("org.test.info");
    static final Logger logComTestInfo = LoggerFactory.getLogger("com.test.info");

    @Test
    public void testCanTrapDebugMessageEvenIfLogLevelIsInfo() {
        try (Log4jInterceptor interceptor = Log4jInterceptor.onDebug().trap().build()) {
            // log level is on info however the following debug message still will be trapped!
            log.debug("Log4jInterceptorTest");
            Assert.assertEquals(1, interceptor.getEvents().size());
            Assert.assertTrue(interceptor.messages().anyMatch(m->m.equals("Log4jInterceptorTest")));
        }
    }

    @Test
    public void testWontTrapDebugMessageOnInfoLevel() {
        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap().build()) {
            log.debug("Log4jInterceptorTest");
            Assert.assertTrue(interceptor.getEvents().isEmpty());
        }
    }

    @Test
    public void testDenyAll() {
        try (Log4jInterceptor interceptor = Log4jInterceptor.onAll().deny().build()) {
            log.debug("Log4jInterceptorTest");
            logPackage.error("org.onehippo.testutils.log4j");
            logOrgTestDebug.debug("org.test.debug");
            logOrgTestInfo.info("org.test.info");
            logComTestInfo.info("com.test.info");
            Assert.assertTrue(interceptor.getEvents().isEmpty());
        }
    }

    @Test
    public void testTrapAll() {
        try (Log4jInterceptor interceptor = Log4jInterceptor.onAll().trap().build()) {
            log.debug("Log4jInterceptorTest");
            logPackage.error("org.onehippo.testutils.log4j");
            logOrgTestDebug.debug("org.test.debug");
            logComTestInfo.info("com.test.info");
            logOrgTestInfo.info("org.test.info");
            Assert.assertEquals(5, interceptor.getEvents().size());
        }
    }

    @Test
    public void testTrapAllDenied() {
        try (Log4jInterceptor interceptor = Log4jInterceptor.onAll().deny().trap().build()) {
            log.debug("Log4jInterceptorTest");
            logPackage.error("org.onehippo.testutils.log4j");
            logOrgTestDebug.debug("org.test.debug");
            logComTestInfo.info("com.test.info");
            Assert.assertEquals(4, interceptor.getEvents().size());
        }
    }

    @Test
    public void testTrapSomeDenyAll() {
        try (Log4jInterceptor interceptor = Log4jInterceptor.onAll().trap("org.onehippo").deny("org.test", "com.test").build()) {
            log.debug("Log4jInterceptorTest");
            logPackage.debug("org.onehippo.testutils.log4j");
            logOrgTestDebug.debug("org.test.debug");
            logOrgTestInfo.info("org.test.info");
            logComTestInfo.info("com.test.info");
            Assert.assertEquals(2, interceptor.getEvents().size());
            List<String> messages = interceptor.messages().collect(Collectors.toList());
            Assert.assertTrue(messages.contains("Log4jInterceptorTest") && messages.contains("org.onehippo.testutils.log4j"));
        }
    }

    @Test
    public void testTrapLoggerHierarchy() {
        try (Log4jInterceptor interceptor = Log4jInterceptor.onAll().trap("org.onehippo").build()) {
            log.debug("Log4jInterceptorTest");
            logPackage.error("org.onehippo.testutils.log4j");
            Assert.assertEquals(2, interceptor.getEvents().size());
            List<String> messages = interceptor.messages().collect(Collectors.toList());
            Assert.assertTrue(messages.contains("Log4jInterceptorTest") && messages.contains("org.onehippo.testutils.log4j"));
        }
    }
}
