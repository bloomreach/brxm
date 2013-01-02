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
