/*
 * Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.model;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TraceMonitor test
 * <P>
 * If you run this in a command line, you can run these for instance:
 * </P>
 * <UL>
 * <LI>$ mvn test -Dtest=TraceMonitorTest</LI>
 * <LI>$ mvn test -Dtest=TraceMonitorTest -DTraceMonitor.enabled=true</LI>
 * <LI>$ mvn test -Dtest=TraceMonitorTest -DTraceMonitor.enabled=true -DTraceMonitor.max=3000</LI>
 * </UL>
 */
public class TraceMonitorTest {

    private static Logger log = LoggerFactory.getLogger(TraceMonitorTest.class);

    private static Logger originalLogger;
    private static Logger testLogger;

    @Before
    public void before() throws Exception {
        originalLogger = TraceMonitor.log;
        testLogger = EasyMock.createNiceMock(Logger.class);
        EasyMock.expect(testLogger.isDebugEnabled()).andReturn(true).anyTimes();
        EasyMock.replay(testLogger);
        TraceMonitor.log = testLogger;
    }

    @After
    public void after() throws Exception {
        TraceMonitor.log = originalLogger;
    }

    @Test
    public void testDefault() throws Exception {
        boolean enabled = BooleanUtils.toBoolean(System.getProperty(TraceMonitor.ENABLED_PROP));
        int maxSize = NumberUtils.toInt(System.getProperty(TraceMonitor.MAX_PROP), -1);

        if (maxSize < 0) {
            maxSize = TraceMonitor.DEFAULT_MAX_SIZE;
        }

        if (!enabled) {
            log.info("Testing the default setting without any system properties...");

            for (int i = 1; i <= maxSize; i++) {
                TraceMonitor.track(MockNode.root().addNode("node-" + i, "nt:unstructured"));
                assertEquals(0, TraceMonitor.getSize());
            }
        } else {
            if (maxSize == 0) {
                log.info("Testing the setting with `-DTraceMonitor.enabled=true -DTraceMonitor.max=0' ...");

                for (int i = 1; i <= maxSize; i++) {
                    TraceMonitor.track(MockNode.root().addNode("node-" + i, "nt:unstructured"));
                    assertEquals(i, TraceMonitor.getSize());
                }

                int doubledMaxSize = 2 * maxSize;

                for (int i = maxSize; i <= doubledMaxSize; i++) {
                    TraceMonitor.track(MockNode.root().addNode("node-" + i, "nt:unstructured"));
                    assertEquals(i, TraceMonitor.getSize());
                }
            } else {
                log.info("Testing the setting with `-DTraceMonitor.enabled=true -DTraceMonitor.max=N' (N is 2000 by default) ...");

                for (int i = 1; i <= maxSize; i++) {
                    TraceMonitor.track(MockNode.root().addNode("node-" + i, "nt:unstructured"));
                    assertEquals(i, TraceMonitor.getSize());
                }

                // one more cycle to track, but the size shouldn't increase over the maximum.
                for (int i = 1; i <= maxSize; i++) {
                    TraceMonitor.track(MockNode.root().addNode("node-" + i, "nt:unstructured"));
                    assertEquals(maxSize, TraceMonitor.getSize());
                }
            }
        }
    }

}
