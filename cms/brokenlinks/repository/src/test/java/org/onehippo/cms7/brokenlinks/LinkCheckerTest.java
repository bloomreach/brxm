/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.slf4j.Logger;

import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;

/**
 * Tests {@link LinkChecker}.
 */
public class LinkCheckerTest {

    @Test
    public void testUncaughtExceptionHandler() throws InterruptedException {
        PrintStream realErr = System.err;

        try {
            PrintStream mockErr = Mockito.mock(PrintStream.class);
            System.setErr(mockErr);

            Logger log = Mockito.mock(Logger.class);

            final RuntimeException expectedException = new RuntimeException("Test");

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

            Mockito.verify(log).error(expectedException.getClass().getName() + ": Test", expectedException);
            Mockito.verify(mockErr, atLeastOnce()).println(Matchers.anyObject());
        } finally {
            System.setErr(realErr);
        }
    }

}
