/**
 * Copyright 2012 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.Random;

import org.junit.Test;

/**
 * TestLazyInitSingletonObjectFactory
 * @version $Id$
 */
public class TestLazyInitSingletonObjectFactory {

    @Test
    public void testNonArgFactory() {
        ResettableObjectFactory<Random, Object> factory = new LazyInitSingletonObjectFactory<Random, Object>() {
            @Override
            protected Random createInstance(Object... args) {
                return new Random();
            }
        };

        Random random1 = factory.getInstance();
        assertNotNull(random1);

        Random random2 = factory.getInstance();
        assertNotNull(random2);

        assertSame(random1, random2);

        factory.reset();

        Random random3 = factory.getInstance();
        assertNotNull(random3);
        assertNotSame(random1, random3);
    }

    @Test
    public void testArgsFactory() {
        ResettableObjectFactory<Random, Long> factory = new LazyInitSingletonObjectFactory<Random, Long>() {
            @Override
            protected Random createInstance(Long ... args) {
                return new Random(args[0]);
            }
        };

        long seed = System.currentTimeMillis();

        Random random1 = factory.getInstance(seed);
        assertNotNull(random1);

        Random random2 = factory.getInstance(seed);
        assertNotNull(random2);

        assertSame(random1, random2);

        factory.reset();

        Random random3 = factory.getInstance(seed);
        assertNotNull(random3);
        assertNotSame(random1, random3);
    }
}
