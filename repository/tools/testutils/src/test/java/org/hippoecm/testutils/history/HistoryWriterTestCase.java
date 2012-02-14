/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.testutils.history;

import org.junit.Ignore;
import org.junit.Test;

public class HistoryWriterTestCase {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    @Test
    @Ignore
    public void test100MeasurePoints() {
        for (int i = 0; i < 100; i++) {
            long start = System.currentTimeMillis();
            somethingExpensive(379);
            long end = System.currentTimeMillis();
            System.out.println("Duration " + String.valueOf(end - start)+ " Milliseconds");
        }
    }

    @Test
    @Ignore
    public void test100MeasurePointsMean() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            somethingExpensive(379);
        }
        long end = System.currentTimeMillis();
        System.out.println("Duration " + String.valueOf( (end - start)/100.0 ) + " Milliseconds");
    }

    @Test
    public void testOneMeasurePoint() {
        long start = System.currentTimeMillis();
        somethingExpensive(501);
        long end = System.currentTimeMillis();
        System.out.println("Duration " + String.valueOf(end - start) + " Milliseconds");
    }

    private void somethingExpensive(int price) {
        for (int i = 0; i < price; i++) {
            for (int j = 0; j < price; j++) {
                Math.sin(i + j);
            }
        }
    }
}
