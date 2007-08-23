package org.hippoecm.repository.testutils;

import org.hippoecm.repository.testutils.history.HistoryWriter;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class HistoryWriterTest extends TestCase {

    private static HistoryWriter historyWriter;

    public static Test suite() {
        TestSuite suite = new TestSuite(HistoryWriterTest.class);
        historyWriter = new HistoryWriter(suite);
        return historyWriter;
    }

    protected void setUp() throws Exception {
    }


    public void testSimple() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 379; i++) {
            for (int j = 0; j < 379; j++) {
                for (int k = 0; k < 379; k++) {
                    @SuppressWarnings("unused")
                    int sum = i + j + k;
                }
            }
        }
        long end = System.currentTimeMillis();
        historyWriter.write("Chaos", String.valueOf(end - start), "Milliseconds");
    }

    public void testFlatline() {
        historyWriter.write("Flatline", "10", "Warnings", false);
    }
}
