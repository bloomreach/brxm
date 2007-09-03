package org.hippoecm.repository.testutils.history;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.hippoecm.repository.testutils.history.HistoryWriter;

public class HistoryWriterTest extends TestCase {

    private static HistoryWriter historyWriter;

    public static Test suite() {
        TestSuite suite = new TestSuite(HistoryWriterTest.class);
        historyWriter = new HistoryWriter(suite);
        return historyWriter;
    }

    protected void setUp() throws Exception {
    }

    public void test100MeasurePoints() {
        for (int i = 0; i < 100; i++) {
            long start = System.currentTimeMillis();
            somethingExpensive(379);
            long end = System.currentTimeMillis();
            historyWriter.write("Duration", String.valueOf(end - start), "Milliseconds");
        }
    }
    
    public void testOneMeasurePoint() {
        long start = System.currentTimeMillis();
        somethingExpensive(501);
        long end = System.currentTimeMillis();
        historyWriter.write("Duration", String.valueOf(end - start), "Milliseconds");
    }

    private void somethingExpensive(int price) {
        for (int i = 0; i < price; i++) {
            for (int j = 0; j < price; j++) {
                Math.sin(i + j);
            }
        }
    }
}
