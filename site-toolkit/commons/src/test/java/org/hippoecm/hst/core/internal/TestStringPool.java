/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.core.internal;

import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestStringPool {

    @Test
    public void testStringInstanceFromPool() throws InterruptedException {
        String foo = new String("foo");

        assertSame(foo, StringPool.get(foo));
        System.gc();
        Thread.sleep(100);
        System.gc();
        assertSame(foo, StringPool.get(foo));
        assertSame(foo, StringPool.get(new String("foo")));

        // make foo String ready for GC. This should happen because StringPool has weak keys and weak values
        final int identityHashCode = System.identityHashCode(foo);
        foo = null;
        long start = System.currentTimeMillis();
        while(System.identityHashCode(StringPool.get(new String("foo"))) == identityHashCode) {
            System.gc();
            Thread.sleep(100);
            System.gc();
            if ( (System.currentTimeMillis() - start) > 10000) {
                fail("Within 10 secs we would expect instance for 'foo' to have been GC-ed");
            }
        }

    }

    @Test
    public void testStringPoolWeakRefsAvoidOOM() throws InterruptedException {

        // if all string were kept, the size could never get smaller
        boolean evictionTookPlace = false;
        for (int i = 1; i < 1000 * 10000; i++) {
            // puts 10 Mb + i bytes Strings in pool (i needed to get unique strings)
            StringPool.get(new String(new byte[100 * 100 + i]));
            if (i % 1000 == 0) {
                int sizeBefore = StringPool.size();
                System.gc();
                Thread.sleep(100);
                System.gc();
                if (StringPool.size() < sizeBefore) {
                    evictionTookPlace = true;
                    break;
                }
            }
        }
        assertTrue("GC should had kicked in to reduce memory of weak references.", evictionTookPlace);
    }


}
