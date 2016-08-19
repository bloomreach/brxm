/*
 * Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.brokenlinks;

import java.io.PrintStream;

import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;

import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.verify;

/**
 * Tests {@link LinkChecker}.
 */
public class LinkCheckerTest {

    @Test
    public void testUncaughtExceptionHandler() throws InterruptedException {
        PrintStream realErr = System.err;

        try {
            final PrintStream mockErr = EasyMock.createMock(PrintStream.class);
            System.setErr(mockErr);

            mockErr.print(isA(String.class));
            EasyMock.expectLastCall();

            final RuntimeException expectedException = new RuntimeException("Test");

            final Logger log = EasyMock.createMock(Logger.class);
            log.error("java.lang.RuntimeException: Test", expectedException);
            EasyMock.expectLastCall();

            EasyMock.replay(mockErr, log);

            Thread t = new Thread() {
                @Override
                public void run() {
                    throw expectedException;
                }
            };

            final LinkChecker.LogUncaughtExceptionHandler eh = new LinkChecker.LogUncaughtExceptionHandler(log);
            t.setUncaughtExceptionHandler(eh);

            t.start();
            t.join();

            verify(mockErr, log);
        } finally {
            System.setErr(realErr);
        }
    }

}
