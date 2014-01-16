/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.concurrent.action;

import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Repository Action. 
 * Note: When you implement a new Action you must also register it with StampedeTest
 */
public abstract class Action {

    private final AtomicInteger count = new AtomicInteger(0);
    private final AtomicInteger missed = new AtomicInteger(0);
    private final AtomicInteger timeSpent = new AtomicInteger(0);

    protected final ActionContext context;
    
    public Action(ActionContext context) {
        this.context = context;
    }

    public final Node execute(Node node) throws Exception {
        count.incrementAndGet();
        Session session = node.getSession();
        try {
            long start = System.currentTimeMillis();
            node = doExecute(node);
            long delta = System.currentTimeMillis() - start;
            timeSpent.addAndGet((int) delta);
        } finally {
            session.refresh(false);
        }
        return node;
    }
    
    /**
     * Is the operation you have in mind applicable to this node?
     */
    public abstract boolean canOperateOnNode(Node node) throws Exception;
    
    public int getCount() {
        return count.get();
    }

    public void addMissed() {
        missed.incrementAndGet();
    }

    public int getMissed() {
        return missed.get();
    }

    public int getTimeSpent() {
        return timeSpent.get();
    }
    
    public double getWeight() {
        return 1.0;
    }

    public abstract boolean isWriteAction();
    
    /**
     * Do your thing. The node this method returns acts as a starting point
     * for the next action. Returning null will cause the runner
     * to start from the top.
     */
    protected abstract Node doExecute(Node node) throws Exception;
}
