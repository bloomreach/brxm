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
package org.hippoecm.hst.statistics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * DefaultCounter
 * @version $Id$
 */
public class DefaultCounter implements Counter {
    
    private static final long serialVersionUID = 1L;
    
    private String name;
    private AtomicLong atomicValue;
    private volatile boolean enabled;
    
    public DefaultCounter() {
        this(null);
    }
    
    public DefaultCounter(String name) {
        this(name, 0L);
    }
    
    public DefaultCounter(String name, long initialValue) {
        this.name = name;
        this.atomicValue = new AtomicLong(initialValue);
    }
    
    public String getName() {
        return name;
    }
    
    public long getValue() {
        return atomicValue.get();
    }
    
    public long increment() {
        if (enabled) {
            return atomicValue.incrementAndGet();
        }
        return 0L;
    }
    
    public long decrement() {
        if (enabled) {
            return atomicValue.decrementAndGet();
        }
        return 0L;
    }
    
    public void reset() {
        atomicValue.set(0L);
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public String toString() {
        return "Counter('" + name + "'): " + getValue() + ", " + super.toString();
    }
}
