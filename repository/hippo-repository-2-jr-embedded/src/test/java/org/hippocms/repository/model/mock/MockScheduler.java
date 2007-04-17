/*
 * Copyright 2007 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.model.mock;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.hippocms.repository.model.Scheduler;

public class MockScheduler implements Scheduler {
    private static int nextId;
    private Map tasks = new HashMap();

    public MockScheduler() {
        super();
    }

    public String schedule(Date time, Runnable task) {
        String result = String.valueOf(getNextId());
        tasks.put(result, task);
        return result;
    }

    public void cancel(String taskId) {
        tasks.remove(taskId);
    }

    public void runAllTasks() {
        Iterator tasksIterator = tasks.values().iterator();
        while (tasksIterator.hasNext()) {
            Runnable task = (Runnable) tasksIterator.next();
            task.run();
        }
        tasks.clear();
    }

    private static synchronized int getNextId() {
        int result = nextId;
        nextId += 1;
        return result;
    }
}
