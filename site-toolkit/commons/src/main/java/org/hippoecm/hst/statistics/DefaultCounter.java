/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.statistics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * DefaultCounter
 * @version $Id$
 */
public class DefaultCounter implements Counter {
    
    private static final long serialVersionUID = 1L;
    
    private AtomicLong atomicValue;
    
    public DefaultCounter() {
        this(0L);
    }
    
    public DefaultCounter(long initialValue) {
        this.atomicValue = new AtomicLong(initialValue);
    }
    
    public long getValue() {
        return atomicValue.get();
    }
    
    public long increment() {
        return atomicValue.incrementAndGet();
    }
    
    public long decrement() {
        return atomicValue.decrementAndGet();
    }
    
    public void reset() {
        atomicValue.set(0L);
    }
    
    @Override
    public String toString() {
        return super.toString() + ": " + getValue();
    }
}
