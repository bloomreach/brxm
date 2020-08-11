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
package org.hippoecm.hst.core.jcr.pool;

import org.hippoecm.hst.statistics.Counter;
import org.hippoecm.hst.statistics.DefaultCounter;

/**
 * DefaultPoolingCounter
 * @version $Id$
 */
public class DefaultPoolingCounter implements PoolingCounter, PoolingCounterMBean {
    
    private boolean enabled;
    
    private Counter sessionCreatedCounter = new DefaultCounter();
    private Counter sessionActivatedCounter = new DefaultCounter();
    private Counter sessionObtainedCounter = new DefaultCounter();
    private Counter sessionPassivatedCounter = new DefaultCounter();
    private Counter sessionDestroyedCounter = new DefaultCounter();
    private Counter sessionReturnedCounter = new DefaultCounter();
    
    public DefaultPoolingCounter() {
        this(false);
    }
    
    public DefaultPoolingCounter(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void reset() {
        sessionCreatedCounter.reset();
        sessionActivatedCounter.reset();
        sessionObtainedCounter.reset();
        sessionPassivatedCounter.reset();
        sessionDestroyedCounter.reset();
        sessionReturnedCounter.reset();
    }
    
    public void sessionCreated() {
        if (enabled) {
            sessionCreatedCounter.increment();
        }
    }
    
    public void sessionActivated() {
        if (enabled) {
            sessionActivatedCounter.increment();
        }
    }
    
    public void sessionObtained() {
        if (enabled) {
            sessionObtainedCounter.increment();
        }
    }
    
    public void sessionPassivated() {
        if (enabled) {
            sessionPassivatedCounter.increment();
        }
    }
    
    public void sessionDestroyed() {
        if (enabled) {
            sessionDestroyedCounter.increment();
        }
    }
    
    public void sessionReturned() {
        if (enabled) {
            sessionReturnedCounter.increment();
        }
    }
    
    public long getNumSessionsCreated() {
        return sessionCreatedCounter.getValue();
    }
    
    public long getNumSessionsActivated() {
        return sessionActivatedCounter.getValue();
    }
    
    public long getNumSessionsObtained() {
        return sessionObtainedCounter.getValue();
    }
    
    public long getNumSessionsPassivated() {
        return sessionPassivatedCounter.getValue();
    }
    
    public long getNumSessionsDestroyed() {
        return sessionDestroyedCounter.getValue();
    }
    
    public long getNumSessionsReturned() {
        return sessionReturnedCounter.getValue();
    }
}
